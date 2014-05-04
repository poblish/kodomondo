package com.andrewregan.kodomondo.tasks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.LocalMavenConfig;
import com.andrewregan.kodomondo.es.EsUtils;
import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;

import dagger.Module;
import dagger.ObjectGraph;

/**
 * 
 * TODO
 *
 * @author andrewregan
 *
 */
public class SourceDownloadTaskTest {

	@Named("mvnRoot")
	@Inject IFileObject mvnRoot;

	@Inject EsUtils esEnv;
	@Inject IFileSystem fs;
	@Inject SourceDownloaderFactory srcDownloaderFactory;

	@BeforeClass
    void injectDependencies() {
        ObjectGraph.create( new TestModule() ).inject(this);
    }

	@Test
	public void testTask() {
		assertThat( "Source should not already exist", new File( mvnRoot.getPath(), "/com/google/guava/guava/16.0.1/guava-16.0.1-sources.jar").exists(), is(false));

		final SourceDownloadTask task = srcDownloaderFactory.create( fs.resolveFile( mvnRoot.getPath() + "/com/google/guava/guava/16.0.1/guava-16.0.1.jar") );

		esEnv.waitForStatus();

		assertThat( task.call(), is(0));

		// Check file deleted
		assertThat( "File not deleted", new File( mvnRoot.getPath(), "/com/google/guava/guava/16.0.1/guava-16.0.1-sources.jar").exists(), is(false));
		assertThat( "File not deleted", new File( mvnRoot.getPath(), "/com/google/guava/guava/16.0.1/guava-16.0.1-sources.jar.sha1").exists(), is(false));
	}

	@Module( includes=LocalMavenConfig.class, overrides=true, injects=SourceDownloadTaskTest.class)
	static class TestModule {}
}