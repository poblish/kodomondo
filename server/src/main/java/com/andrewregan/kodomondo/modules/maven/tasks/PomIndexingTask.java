/**
 * 
 */
package com.andrewregan.kodomondo.modules.maven.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.Charset;

import javax.inject.Inject;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

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

	private final static Logger LOG = LoggerFactory.getLogger( PomIndexingTask.class );

	@Inject
	public PomIndexingTask( IFileObject artifact, IFileObject pomFile, IFileObject mvnRoot, Client esClient, ObjectMapper mapper) {
		this.pomFile = checkNotNull(pomFile);
		this.esClient = checkNotNull(esClient);
		this.mapper = checkNotNull(mapper);
		this.artifactRelativePath = checkNotNull(artifact).getPathRelativeToFile(mvnRoot);
	}

	public void run() {
		LOG.debug("> Start POM: " + pomFile);

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

			LOG.debug("--> Indexing: " + artifactRelativePath);

			esClient.prepareIndex( "datasource.local-maven", "metadata", artifactRelativePath).setSource( mapper.writeValueAsBytes( new PomIndexEntry( name, desc, artifactRelativePath) ) ).get();
		}
		catch (Throwable e) {
			LOG.error( "", e);  // FIXME
			Throwables.propagate(e);
		}
		finally {
			LOG.debug("> DONE: " + pomFile);
		}
	}

	private static class PomIndexEntry extends IndexEntry {
		public PomIndexEntry( final String inName, final String inDesc, final String artifact) {
			super( inDesc, artifact, null, inName);
		}
	}
}