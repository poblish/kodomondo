/**
 * 
 */
package com.andrewregan.kodomondo;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.handlers.ListingsHandler;
import com.andrewregan.kodomondo.tasks.JavaDocDownloaderFactory;
import com.andrewregan.kodomondo.tasks.JavaDocIndexerFactory;
import com.andrewregan.kodomondo.tasks.PomIndexerFactory;
import com.andrewregan.kodomondo.util.VersionComparator;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
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
		visitDir(mvnRoot);
	}

	private void visitDir( IFileObject inDir) {

		final String id = inDir.getAbsolutePath();

		final GetResponse getResp = esClient.prepareGet( "datasource.local-maven", "dir-visit", id).get();
		if (getResp.isExists()) {
			return;  // Either never existed, or its _ttl expired and it was deleted
		}

		////////////////////////////////////////////////////////

		System.out.println("Here:" + inDir);

		IFileObject[] files = inDir.listFiles( new ListingsHandler.BadDirFilter() );
		Collection<String> versions = Lists.newArrayList();
		Collection<IFileObject> jars = Lists.newArrayList();

		boolean foundJavaDoc = false;

		for ( IFileObject each : files) {
			if (each.isDirectory()) {
				if (Character.isDigit( each.getName().charAt(0) )) {
					versions.add( each.getName() );
				}
				else {
					visitDir( inDir.getChild( each.getName() ) );
				}
			}
			else {
				if (each.getName().endsWith(".pom")) {
					pomIndexerFactory.create( inDir, each).run();
				}

				if (ListingsHandler.isUselessFile(each)) {
					continue;
				}

				if (each.getName().endsWith("-javadoc.jar") ) {
					indexerFactory.create( inDir.getPathRelativeToFile(mvnRoot), each).run();
					foundJavaDoc = true;
					continue;
				}

				jars.add(each);
				System.out.println("> Add JAR: " + each);
			}
		}

		/////////////////////////////////////

		if (!jars.isEmpty()) {
			if (jars.size() == 1) {
				// handleFile( t, jars.iterator().next());
			}
			else {
				System.err.println("> 1 match: " + jars);
				// handleFile( t, jars.iterator().next());
			}

			if (!foundJavaDoc) {
				taskExecutor.submit( docsDownloaderFactory.create( inDir.getFileRelativeToFile(mvnRoot) ) );
			}

			return;
		}
		else if (!versions.isEmpty()) {
			String highest = Ordering.from( new VersionComparator() ).max(versions);
			// FIXME new VersionResponse(highest) );
			System.out.println("> highest: " + highest);
		}

		// DONE!
		esClient.prepareIndex( "datasource.local-maven", "dir-visit", id).setTTL( TimeUnit.DAYS.toMillis(2) ).setSource("{\"done\":true}").get();
	}
}