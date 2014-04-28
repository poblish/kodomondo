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

	@Module( includes=DaggerModule.class, overrides=true, injects=LaunchHandlerTest.class)
	static class TestModule {}
}