package com.andrewregan.kodomondo.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.LocalMavenConfig;
import com.andrewregan.kodomondo.fs.TestFileObject;
import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;

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
	public void testHandle() throws IOException, ServletException {
		final Request req = mock( Request.class );
		final HttpServletResponse resp = mock( HttpServletResponse.class );

		final ByteArrayOutputStream bas = new ByteArrayOutputStream();
		final ServletOutputStream sos = new ServletOutputStream() {

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setWriteListener( WriteListener writeListener) {
			}

			@Override
			public void write( int b) throws IOException {
				bas.write(b);
			}};

		when(resp.getOutputStream()).thenReturn(sos);

		when(req.getRequestURI()).thenReturn("/com/codahale");

		handler.handle("", req, mock( HttpServletRequest.class ), resp);

		assertThat( new String( bas.toByteArray(), "utf-8"), is("{\"dirs\":[{\"dir\":\"/metrics\"}]}"));
	}

	@Module( includes=LocalMavenConfig.class, overrides=true, injects=ListingsHandlerTest.class)
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
				.thenReturn( new TestFileObject( ifs, "/usr/blah/com/codahale", true, new IFileObject[]{ new TestFileObject( ifs, "/usr/blah/com/codahale/metrics", true, new IFileObject[]{}) }) );

			return ifs;
		}
	}
}