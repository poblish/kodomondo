package com.andrewregan.kodomondo.modules.maven.tasks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.es.EsUtils;
import com.andrewregan.kodomondo.fs.TestFileObject;
import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.modules.maven.config.LocalMavenConfig;
import com.andrewregan.kodomondo.modules.maven.tasks.SourceDownloadTask;
import com.andrewregan.kodomondo.modules.maven.tasks.SourceDownloaderFactory;
import com.andrewregan.kodomondo.modules.maven.util.ArtifactDesc;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

/**
 * 
 * TODO
 *
 * @author andrewregan
 *
 */
public class SourceDownloadTaskTest {

	@Inject EsUtils esEnv;
	@Inject IFileSystem fs;
	@Inject SourceDownloaderFactory srcDownloaderFactory;

	@BeforeClass
    void injectDependencies() {
        ObjectGraph.create( new TestModule() ).inject(this);
    }

	@Test
	public void testTask() {
		assertThat( "Source should not already exist", new File("/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1-sources.jar").exists(), is(false));

		final SourceDownloadTask task = srcDownloaderFactory.create( fs.resolveFile("/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1.jar") );

		esEnv.waitForStatus();

		assertThat( task.call(), is(0));

		// Check file deleted
		assertThat( "File not deleted", new File("/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1-sources.jar").exists(), is(false));
		assertThat( "File not deleted", new File("/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1-sources.jar.sha1").exists(), is(false));
	}

	@Module( includes=LocalMavenConfig.class, overrides=true, injects=SourceDownloadTaskTest.class)
	static class TestModule {

		@Provides @Named("mvnRoot")
		IFileObject provideMavenRoot(IFileSystem inFS) {
			return new TestFileObject( getFS(), "/usr/blah");
		}

		@Provides @Singleton
		IFileSystem provideFileSystemManager() {
			return getFS();
		}

		private IFileSystem getFS() {
			final IFileSystem ifs = mock( IFileSystem.class );

			when (ifs.resolveFile( anyString() ))
				.thenReturn( new TestFileObject( ifs, "/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1.jar") );

			when (ifs.resolveFile( any( IFileObject.class ), anyString() ))
				.thenReturn( new TestFileObject( ifs, "/usr/blah/com/google/guava/guava/16.0.1", true, new IFileObject[]{}) );

			when(ifs.toArtifact( any( IFileObject.class ) )).thenReturn( new ArtifactDesc("com.google.guava", "guava", "16.0.1") );

			return ifs;
		}
	}
}