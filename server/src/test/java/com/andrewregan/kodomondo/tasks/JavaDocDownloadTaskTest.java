package com.andrewregan.kodomondo.tasks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.es.EsUtils;
import com.andrewregan.kodomondo.fs.TestFileObject;
import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.modules.maven.config.LocalMavenConfig;
import com.andrewregan.kodomondo.modules.maven.tasks.JavaDocDownloadTask;
import com.andrewregan.kodomondo.modules.maven.tasks.JavaDocDownloaderFactory;
import com.andrewregan.kodomondo.modules.maven.util.ArtifactDesc;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;

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
		assertThat( "JavaDoc should not already exist", new File("/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1-javadoc.jar").exists(), is(false));

		final JavaDocDownloadTask task = docsDownloaderFactory.create( fs.resolveFile("/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1.jar") );

		esEnv.waitForStatus();

		assertThat( task.call(), is(0));

		// Check file deleted
		assertThat( "File not deleted", new File("/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1-javadoc.jar").exists(), is(false));
		assertThat( "File not deleted", new File("/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1-javadoc.jar.sha1").exists(), is(false));
	}

	@Module( includes=LocalMavenConfig.class, overrides=true, injects=JavaDocDownloadTaskTest.class)
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

			JarFile jf = mock( JarFile.class );
			final JarEntry je1 = new JarEntry("com/google/common/collect/ImmutableMap.class");
			final List<JarEntry> jl = Lists.newArrayList(je1);

			try {
				when(jf.entries()).thenReturn( Iterators.asEnumeration( jl.iterator() ) );
				when( ifs.openJar( any( IFileObject.class ) ) ).thenReturn(jf);
			}
			catch (IOException e) {
				Throwables.propagate(e);
			}

			when (ifs.resolveFile( anyString() )).thenReturn( new TestFileObject( ifs, "/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1.jar") );

			when (ifs.resolveFile( any( IFileObject.class ), anyString() )).thenAnswer( new Answer<IFileObject>() {

					@Override
					public IFileObject answer( InvocationOnMock invocation) throws Throwable {
						String name = (String) invocation.getArguments()[1];
						return name.endsWith(".jar") ? new TestFileObject( ifs, "/usr/blah/" + name, false) : new StatefulTestDir( ifs, name);
					}
				} );

			when(ifs.toArtifact( any( IFileObject.class ) )).thenReturn( new ArtifactDesc("com.google.guava", "guava", "16.0.1") );

			return ifs;
		}

		// Horrible code to simulate download of a file into a dir
		private static class StatefulTestDir extends TestFileObject {

			public StatefulTestDir(IFileSystem fs, String path) {
				super(fs, path, true);
				this.children = new IFileObject[]{};
			}

			@Override
			public IFileObject[] listFiles() {
				IFileObject[] ret = children.clone();
				children = ObjectArrays.concat( children, new TestFileObject( this.fs, getAbsolutePath() + "/child." + ret.length, false));
				return ret;
			}
		}
	}
}