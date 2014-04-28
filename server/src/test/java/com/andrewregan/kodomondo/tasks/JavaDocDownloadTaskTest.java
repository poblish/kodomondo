package com.andrewregan.kodomondo.tasks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;

import javax.inject.Inject;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.DaggerModule;
import com.andrewregan.kodomondo.es.EsUtils;
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
public class JavaDocDownloadTaskTest {

	@Inject EsUtils esEnv;
	@Inject IFileSystem fs;
	@Inject JavaDocDownloaderFactory docsDownloaderFactory;

	@BeforeClass
    void injectDependencies() {
        ObjectGraph.create( new TestModule() ).inject(this);
    }

	@Test
	public void testTask() {
		assertThat( "JavaDoc should not already exist", new File("/Users/andrewregan/.m2/repository/com/google/guava/guava/16.0.1/guava-16.0.1-javadoc.jar").exists(), is(false));

		final JavaDocDownloadTask task = docsDownloaderFactory.create( fs.resolveFile("/Users/andrewregan/.m2/repository/com/google/guava/guava/16.0.1/guava-16.0.1.jar") );

		esEnv.waitForStatus();

		assertThat( task.call(), is(0));

		// Check file deleted
		assertThat( "File not deleted", new File("/Users/andrewregan/.m2/repository/com/google/guava/guava/16.0.1/guava-16.0.1-javadoc.jar").exists(), is(false));
		assertThat( "File not deleted", new File("/Users/andrewregan/.m2/repository/com/google/guava/guava/16.0.1/guava-16.0.1-javadoc.jar.sha1").exists(), is(false));
	}

	@Module( includes=DaggerModule.class, overrides=true, injects=JavaDocDownloadTaskTest.class)
	static class TestModule {

//		@Provides
//		@Singleton
//		IFileSystem provideFileSystemManager() {
//			return new OurTestFileSystem();
//		}
	}

//	private static class OurTestFileSystem extends TestFileSystem {
//
//		@Override
//		public ArtifactDesc toArtifact( IFileObject file) {
//			return ArtifactDesc.forFile( /* FIXME */ new File(((TestFileObject) file).getAbsolutePath()) );
//		}
//	}
}