package com.andrewregan.kodomondo.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.LocalMavenConfig;
import com.andrewregan.kodomondo.fs.TestFileObject;
import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.maven.util.ArtifactDesc;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class LaunchHandlerTest {

	@Inject LaunchHandler handler;

	@BeforeClass
    void injectDependencies() {
        ObjectGraph.create( new TestModule() ).inject(this);
    }

	@Test
	public void testHandle() throws IOException, ServletException {
		final Request baseReq = mock( Request.class );
		final HttpServletRequest req = mock( HttpServletRequest.class );
		final HttpServletResponse resp = mock( HttpServletResponse.class );

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		when(resp.getWriter()).thenReturn(pw);

		when(baseReq.getRequestURI()).thenReturn("org.apache.lucene.analysis.Analyzer");
		when(req.getParameter("artifact")).thenReturn("org/apache/lucene/lucene-core/4.5.1");
		when(req.getParameter("source")).thenReturn("1");

		handler.handle("", baseReq, req, resp);

		assertThat( sw.toString(), is("OK\n"));
	}

	@Module( includes=LocalMavenConfig.class, overrides=true, injects=LaunchHandlerTest.class)
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

			when (ifs.resolveFile( any( IFileObject.class ), anyString() ))
				.thenReturn( new TestFileObject( ifs, "/usr/blah/org/apache/lucene/lucene-core/4.5.1", true, new IFileObject[]{ new TestFileObject( ifs, "/usr/blah/org/apache/lucene/lucene-core/4.5.1/blah.jar") }) )
				.thenReturn( new TestFileObject( ifs, "/usr/blah/org/apache/lucene/lucene-core/", true, new IFileObject[]{}) );

			when(ifs.toArtifact( any( IFileObject.class ) )).thenReturn( new ArtifactDesc("org.apache.lucene", "lucene-core", "4.5.1") );

			return ifs;
		}
	}
}