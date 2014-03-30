package com.andrewregan.kodomondo.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.DaggerModule;

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

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		when(resp.getWriter()).thenReturn(pw);

		when(req.getRequestURI()).thenReturn("/com/codahale");

		handler.handle("", req, mock( HttpServletRequest.class ), resp);

		assertThat( sw.toString(), is("{\"classes\":[],\"jar\":null}\n"));
	}

	@Module( includes=DaggerModule.class, overrides=true, injects=ListingsHandlerTest.class)
	static class TestModule {

//		@Provides
//		@Singleton
//		IFileSystem provideFileSystemManager() {
//			return new TestFileSystem();
//		}
	}
}