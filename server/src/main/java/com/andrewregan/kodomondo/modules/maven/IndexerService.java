/**
 * 
 */
package com.andrewregan.kodomondo.modules.maven;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.modules.maven.repo.ILocalMavenVisitor;
import com.andrewregan.kodomondo.modules.maven.repo.LocalMavenRepository;
import com.andrewregan.kodomondo.modules.maven.tasks.JavaDocDownloaderFactory;
import com.andrewregan.kodomondo.modules.maven.tasks.JavaDocIndexerFactory;
import com.andrewregan.kodomondo.modules.maven.tasks.PomIndexerFactory;
import com.google.common.util.concurrent.AbstractExecutionThreadService;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
@Singleton
public class IndexerService extends AbstractExecutionThreadService {

	@Inject IFileSystem fs;

	@Named("mvnRoot")
	@Inject IFileObject mvnRoot;

	@Inject Client esClient;

	@Inject ExecutorService taskExecutor;

	@Inject JavaDocDownloaderFactory docsDownloaderFactory;
	@Inject JavaDocIndexerFactory indexerFactory;
	@Inject PomIndexerFactory pomIndexerFactory;

	private final static Logger LOG = LoggerFactory.getLogger( IndexerService.class );

	@Override
	protected void run() throws Exception {
		Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay( new ServiceRunner(), 0L, /* Every: */ 1L, TimeUnit.HOURS);
	}

	private class ServiceRunner implements Runnable {

		@Override
		public void run() {
			LOG.trace("Started...");

			new LocalMavenRepository(mvnRoot).visit( new ILocalMavenVisitor() {

				@Override
				public boolean acceptDirectory( IFileObject inDir) {
					final GetResponse getResp = esClient.prepareGet( "datasource.local-maven", "dir-visit", inDir.getAbsolutePath()).get();
					return (!getResp.isExists());  // Either never existed, or its _ttl expired and it was deleted
				}

				@Override
				public void foundPom( IFileObject dir, IFileObject pomFile) {
					LOG.debug("> Got POM: " + pomFile);
					pomIndexerFactory.create( dir, pomFile).run();
				}

				@Override
				public void foundJavaDoc( String relativePath, IFileObject docJar) {
					LOG.debug("> Got JavaDoc: " + docJar);
					indexerFactory.create( relativePath, docJar).run();
				}

				@Override
				public void missingJavaDoc( IFileObject artifactDir) {
					LOG.debug("> Try to download JavaDoc: " + artifactDir);
					taskExecutor.submit( docsDownloaderFactory.create(artifactDir) );
				}

				@Override
				public void doneDirectory( IFileObject inDir) {
					if (inDir.equals(mvnRoot)) {
						return;
					}

					esClient.prepareIndex( "datasource.local-maven", "dir-visit", inDir.getAbsolutePath()).setTTL( TimeUnit.DAYS.toMillis(2) ).setSource("{\"done\":true}").get();
				}

			}, mvnRoot, false);

			LOG.trace("STOPPED");
		}
	}
}