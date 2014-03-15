/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;

import org.elasticsearch.client.Client;
import org.jsoup.Jsoup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;

/**
 * Try http://localhost:2000/com/higherfrequencytrading/chronicle/1.7.1-SNAPSHOT
 *
 * @author andrewregan
 *
 */
public class JavaDocIndexingTask implements Runnable {

	private final String artifactRelativePath;
	private final File docJar;

	private Client esClient;
	private ObjectMapper mapper;

	@Inject
	public JavaDocIndexingTask( File artifact, File javaDocJar, String mvnRoot, Client esClient, ObjectMapper mapper) {
		this.docJar = checkNotNull(javaDocJar);
		this.esClient = checkNotNull(esClient);
		this.mapper = checkNotNull(mapper);
		this.artifactRelativePath = checkNotNull(artifact).getPath().substring( mvnRoot.length() );
	}

	public void run() {
		System.out.println("> Start indexing " + docJar);

		try (JarFile jf = new JarFile(docJar)) {
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

				String id = artifactRelativePath + ":" + eachEntry.getName().replace('/', '.').substring( 0, /* Strip .html */ eachEntry.getName().length() - 5);
				System.out.println("> Indexing as " + id);

				esClient.prepareIndex( "datasource.local-maven", "doc", id).setSource( mapper.writeValueAsBytes( new JavaDocIndexEntry( text, artifactRelativePath) ) ).get();
			}
		}
		catch (Throwable tt) {
			tt.printStackTrace(); // Throwables.propagate(tt);
		}

	}

	private static class JavaDocIndexEntry {
		private final String text;
		private final String artifact;

		public JavaDocIndexEntry( final String inText, final String artifact) {
			this.text = checkNotNull(inText);
			this.artifact = checkNotNull(artifact);
		}

		public String getText() {
			return text;
		}

		public String getArtifact() {
			return artifact;
		}
	}
}