/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.nio.charset.Charset;

import javax.inject.Inject;

import org.elasticsearch.client.Client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.io.Files;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class PomIndexingTask implements Runnable {

	private final String artifactRelativePath;
	private final File pomFile;

	private Client esClient;
	private ObjectMapper mapper;

	@Inject
	public PomIndexingTask( File artifact, File pomFile, String mvnRoot, Client esClient, ObjectMapper mapper) {
		this.pomFile = checkNotNull(pomFile);
		this.esClient = checkNotNull(esClient);
		this.mapper = checkNotNull(mapper);
		this.artifactRelativePath = checkNotNull(artifact).getPath().substring( mvnRoot.length() );
	}

	public void run() {
		System.out.println("> Start indexing " + pomFile);

		try {
			String text = Files.toString(pomFile, Charset.forName("utf-8"));
			int namePos = text.indexOf("<name>") + 6;
			String name = text.substring( namePos, text.indexOf("</name>", namePos)).trim();
			int descPos = text.indexOf("<description>") + 14;
			String desc = text.substring( descPos, text.indexOf("</description>", descPos)).replace('\n', ' ').trim();

			esClient.prepareIndex( "datasource.local-maven", "metadata", artifactRelativePath).setSource( mapper.writeValueAsBytes( new PomIndexEntry( name, desc, artifactRelativePath) ) ).get();
		}
		catch (Throwable e) {
			e.printStackTrace();
			Throwables.propagate(e);
		}
	}

	private static class PomIndexEntry extends IndexEntry {
		public PomIndexEntry( final String inName, final String inDesc, final String artifact) {
			super( inDesc, artifact, null, inName);
		}
	}
}