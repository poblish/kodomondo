/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.Charset;

import javax.inject.Inject;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class PomIndexingTask implements Runnable {

	private final String artifactRelativePath;
	private final IFileObject pomFile;

	private Client esClient;
	private ObjectMapper mapper;

	@Inject
	public PomIndexingTask( IFileObject artifact, IFileObject pomFile, IFileObject mvnRoot, Client esClient, ObjectMapper mapper) {
		this.pomFile = checkNotNull(pomFile);
		this.esClient = checkNotNull(esClient);
		this.mapper = checkNotNull(mapper);
		this.artifactRelativePath = checkNotNull(artifact).getPathRelativeToFile(mvnRoot);
	}

	public void run() {
		System.out.println("> Start indexing POM: " + pomFile);

		try {
			String text = pomFile.toString( Charset.forName("utf-8") );
			int namePos = text.indexOf("<name>");
			String name = ( namePos > 0) ? text.substring( namePos + 6, text.indexOf("</name>", namePos + 6)).trim() : "";
			int descPos = text.indexOf("<description>") ;
			String desc = ( descPos > 0) ? text.substring( descPos + 13, text.indexOf("</description>", descPos + 13)).replace('\n', ' ').trim() : "";

			final GetResponse getResp = esClient.prepareGet( "datasource.local-maven", "metadata", artifactRelativePath).get();
			if (getResp.isExists()) {
				return;  // Either never existed, or its _ttl expired and it was deleted
			}

			System.out.println("> Indexing as " + artifactRelativePath);

			esClient.prepareIndex( "datasource.local-maven", "metadata", artifactRelativePath).setSource( mapper.writeValueAsBytes( new PomIndexEntry( name, desc, artifactRelativePath) ) ).get();
		}
		catch (Throwable e) {
			e.printStackTrace();  // Throwables.propagate(e);
		}
		finally {
			System.out.println("> DONE indexing POM: " + pomFile);
		}
	}

	private static class PomIndexEntry extends IndexEntry {
		public PomIndexEntry( final String inName, final String inDesc, final String artifact) {
			super( inDesc, artifact, null, inName);
		}
	}
}