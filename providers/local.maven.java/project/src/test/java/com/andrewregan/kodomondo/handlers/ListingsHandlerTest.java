package com.andrewregan.kodomondo.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.DaggerModule;
import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.maven.util.ArtifactDesc;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.sun.net.httpserver.Headers;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class ListingsHandlerTest {

	@Inject ListingsHandler handler;

	@BeforeClass
    void injectDependencies() {
        ObjectGraph.create( new TestModule() ).inject(this);
    }

	@Test
	public void testHandle() throws IOException {
		final com.sun.net.httpserver.HttpExchange req = mock( com.sun.net.httpserver.HttpExchange.class );
		when(req.getRequestURI()).thenReturn( URI.create("http://localhost:2000/com/codahale") );
		when(req.getResponseHeaders()).thenReturn( new Headers() );
		when(req.getResponseBody()).thenReturn( new ByteArrayOutputStream() );
		handler.handle(req);
	}

	@Module( includes=DaggerModule.class, overrides=true, injects=ListingsHandlerTest.class)
	static class TestModule {

		@Provides
		@Singleton
		IFileSystem provideFileSystemManager() {
			return new TestFileSystem();
		}
	}

	private static class TestFileSystem implements IFileSystem {

		@Override
		public IFileObject resolveFile( String path) {
			switch (path) {
				case "/Users/andrewregan/.m2/repository/":  // FIXME!
					return new TestFile(path);
				default:
					return null;
			}
		}

		@Override
		public IFileObject resolveFile( IFileObject parent, String path) {
			return new TestFile(parent.getAbsolutePath() + "/" + path);
		}

		@Override
		public JarFile openJar( IFileObject file) throws IOException {
			JarFile jf = mock( JarFile.class );

			final JarEntry je1 = new JarEntry("je1");

			final List<JarEntry> jl = Lists.newArrayList(je1);
			when(jf.entries()).thenReturn( Iterators.asEnumeration( jl.iterator() ) );

			return jf;
		}

		@Override
		public ArtifactDesc toArtifact( IFileObject file) {
			throw new RuntimeException("unimpl");
		}
	}

	private static class TestFile implements IFileObject {

		private String path;

		public TestFile( final String path) {
			this.path = path;
		}

		@Override
		public IFileObject getChild( String path) {
			return new TestFile( getAbsolutePath() + "/" + path);
		}

		@Override
		public IFileObject[] listFiles( FileFilter fileFilter) {
			return null;
		}

		@Override
		public boolean isDirectory() {
			return false;
		}

		@Override
		public boolean exists() {
			return ( path != null);
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getPath() {
			return null;
		}

		@Override
		public String getAbsolutePath() {
			return path;
		}

		@Override
		public String getPathRelativeToFile( IFileObject other) {
			return null;
		}

		@Override
		public IFileObject getFileRelativeToFile( IFileObject other) {
			return null;
		}

		@Override
		public String toString( Charset cs) throws IOException {
			return null;
		}

		@Override
		public void launch() throws IOException {
		}
	}
}