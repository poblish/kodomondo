/**
 * 
 */
package com.andrewregan.kodomondo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.maven.repo.ILocalMavenVisitor;
import com.andrewregan.kodomondo.maven.repo.LocalMavenRepository;
import com.andrewregan.kodomondo.tasks.JavaDocDownloaderFactory;
import com.andrewregan.kodomondo.tasks.JavaDocIndexerFactory;
import com.andrewregan.kodomondo.tasks.PomIndexerFactory;
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

	@Override
	protected void run() throws Exception {
		new LocalMavenRepository(mvnRoot).visit( new ILocalMavenVisitor() {

			@Override
			public boolean acceptDirectory( IFileObject inDir) {
				final GetResponse getResp = esClient.prepareGet( "datasource.local-maven", "dir-visit", inDir.getAbsolutePath()).get();
				return (!getResp.isExists());  // Either never existed, or its _ttl expired and it was deleted
			}

			@Override
			public void foundPom( IFileObject dir, IFileObject pomFile) {
				System.out.println("> Got POM: " + pomFile);
				pomIndexerFactory.create( dir, pomFile).run();
			}

			@Override
			public void foundJavaDoc( String relativePath, IFileObject docJar) {
				System.out.println("> Got JavaDoc: " + docJar);
				indexerFactory.create( relativePath, docJar).run();
			}

			@Override
			public void missingJavaDoc( IFileObject artifactDir) {
				System.out.println("> Try to download JavaDoc: " + artifactDir);
				taskExecutor.submit( docsDownloaderFactory.create(artifactDir) );
			}

			@Override
			public void doneDirectory( IFileObject inDir) {
				esClient.prepareIndex( "datasource.local-maven", "dir-visit", inDir.getAbsolutePath()).setTTL( TimeUnit.DAYS.toMillis(2) ).setSource("{\"done\":true}").get();
			}

		}, mvnRoot, false, "");
	}
}