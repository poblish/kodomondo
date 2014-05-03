package com.andrewregan.kodomondo.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.LocalMavenConfig;

import dagger.Module;
import dagger.ObjectGraph;

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

//		@Provides
//		@Singleton
//		IFileSystem provideFileSystemManager() {
//			return new TestFileSystem();
//		}
	}
}