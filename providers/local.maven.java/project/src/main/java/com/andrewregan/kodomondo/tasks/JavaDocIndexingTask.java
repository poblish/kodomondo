/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.jsoup.Jsoup;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;

/**
 * Try http://localhost:2000/com/higherfrequencytrading/chronicle/1.7.1-SNAPSHOT
 *
 * @author andrewregan
 *
 */
public class JavaDocIndexingTask implements Runnable {

	private final String artifactRelativePath;
	private final IFileObject docJar;

	private Client esClient;
	private ObjectMapper mapper;
	private IFileSystem fs;

	@Inject
	public JavaDocIndexingTask( String artifact, IFileObject javaDocJar, IFileObject mvnRoot, Client esClient, ObjectMapper mapper, IFileSystem fs) {
		this.docJar = checkNotNull(javaDocJar);
		this.esClient = checkNotNull(esClient);
		this.mapper = checkNotNull(mapper);
		this.artifactRelativePath = checkNotNull(artifact);
		this.fs = checkNotNull(fs);
	}

	public void run() {
		checkArgument( docJar.getName().endsWith("-javadoc.jar"), "Not a JavaDoc JAR!");

		System.out.println("> Start indexing " + docJar);

		try (JarFile jf = fs.openJar(docJar)) {
			Enumeration<JarEntry> theEntries = jf.entries();
			while (theEntries.hasMoreElements()) {
				JarEntry eachEntry = theEntries.nextElement();
				if (eachEntry.isDirectory() || !eachEntry.getName().contains("/") || eachEntry.getName().startsWith("src-html") || eachEntry.getName().contains("/class-use/") || eachEntry.getName().contains("/package-")) {
					continue;
				}

				if ( /*!eachEntry.getName().startsWith("src-html") || */ !eachEntry.getName().endsWith(".html")) {
					continue;
				}

				byte[] b = ByteStreams.toByteArray( jf.getInputStream(eachEntry) );
				String text = Jsoup.parse( new String( b, "utf-8") ).text();

				// Cleanup begins... FIXME pretty lame
				int preambleIdx = text.indexOf("Constr |  Method");
				text = ( preambleIdx > 0) ? text.substring( preambleIdx + 17) : text;

				// Cleanup suffix... FIXME pretty lame
				int psIdx = text.lastIndexOf("Overview Package Class");
				text = ( psIdx > 0) ? text.substring( 0, psIdx) : text;

				String className = eachEntry.getName().replace('/', '.').substring( 0, /* Strip .html */ eachEntry.getName().length() - 5);
				String id = artifactRelativePath + ":" + className;

				final GetResponse getResp = esClient.prepareGet( "datasource.local-maven", "javadoc", id).get();
				if (getResp.isExists()) {
					continue;  // Either never existed, or its _ttl expired and it was deleted
				}

				System.out.println("> Indexing as " + id);

				esClient.prepareIndex( "datasource.local-maven", "javadoc", id).setSource( mapper.writeValueAsBytes( new JavaDocIndexEntry( text, artifactRelativePath, className) ) ).get();
			}
		}
		catch (Throwable tt) {
			tt.printStackTrace(); // Throwables.propagate(tt);
		}
		finally {
			System.out.println("> DONE indexing " + docJar);
		}
	}

	private static class JavaDocIndexEntry extends IndexEntry {
		public JavaDocIndexEntry( final String inText, final String artifact, final String className) {
			super( inText, artifact, className, null);
		}
	}
}