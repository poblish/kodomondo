package com.andrewregan.kodomondo.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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
public class InfoHandlerTest {

	@Inject InfoHandler handler;

	@BeforeClass
    void injectDependencies() {
        ObjectGraph.create( new TestModule() ).inject(this);
    }

	@Test
	public void testHandle() throws IOException, ServletException {
		final HttpServletRequest req = mock( HttpServletRequest.class );
		final HttpServletResponse resp = mock( HttpServletResponse.class );

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		when(resp.getWriter()).thenReturn(pw);

		when(req.getParameter("class")).thenReturn("org.hibernate.criterion.AggregateProjection");
		when(req.getParameter("artifact")).thenReturn("/org/hibernate/hibernate-core/4.3.0.Final");
		when(req.getParameter("jar")).thenReturn("org/hibernate/hibernate-core/4.3.0.Final/hibernate-core-4.3.0.Final.jar");

		handler.handle("", mock( Request.class ), req, resp);

		assertThat( sw.toString(), containsString("<title>Kodomondo: AggregateProjection</title>"));
	}

	@Module( includes=DaggerModule.class, overrides=true, injects=InfoHandlerTest.class)
	static class TestModule {}
}