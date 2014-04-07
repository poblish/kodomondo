package com.andrewregan.kodomondo.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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
import org.elasticsearch.common.text.StringText;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.DaggerModule;
import com.andrewregan.kodomondo.es.EsUtils;

import dagger.Module;
import dagger.ObjectGraph;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class SearchHandlerTest {

	@Inject EsUtils esUtils;
	@Inject SearchHandler handler;

	@BeforeClass
    void injectDependencies() {
        ObjectGraph.create( new TestModule() ).inject(this);
    }

	@Test
	public void testCleanupHighlight() {
		assertThat( handler.cleanUpHighlight( new StringText("Test 1 2 3") ), is("Test 1 2 3"));
		assertThat( handler.cleanUpHighlight( new StringText("Interfaces: Cloneable public class StatsConfig extends Object implements Cloneable Specifies the attributes of a <em>statistics</em> retrieval operation. Field Summary static StatsConfig DEFAULT           A") ), is("Interfaces: Cloneable public class StatsConfig implements Cloneable Specifies the attributes of a <em>statistics</em> retrieval operation. Field Summary static StatsConfig DEFAULT"));
		assertThat( handler.cleanUpHighlight( new StringText(".   Method Summary  StatsConfig clone()           Returns a copy of this configuration object.  ") ), is("StatsConfig clone() Returns a copy of this configuration object"));
	}

	@Test
	public void testHandle() throws IOException, ServletException {
		esUtils.waitForStatus();

		final HttpServletRequest req = mock( HttpServletRequest.class );
		final HttpServletResponse resp = mock( HttpServletResponse.class );

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		when(resp.getWriter()).thenReturn(pw);

		when(req.getParameter("q")).thenReturn("ImmutableMap");

		handler.handle("", mock( Request.class ), req, resp);

		assertThat( sw.toString(), startsWith("[{\"entry\":{\"artifact\":\"com/google/guava/guava/16.0\",\"className\":\"com.google.common.collect.ImmutableMap\",\"name\":\"\"},\"highlights\""));
	}

	@Module( includes=DaggerModule.class, overrides=true, injects=SearchHandlerTest.class)
	static class TestModule {}
}