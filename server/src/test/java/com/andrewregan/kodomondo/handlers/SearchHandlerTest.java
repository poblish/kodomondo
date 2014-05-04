package com.andrewregan.kodomondo.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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
import org.elasticsearch.common.text.StringText;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.LocalMavenConfig;
import com.andrewregan.kodomondo.es.EsUtils;
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
public class SearchHandlerTest {

	@Inject EsUtils esUtils;
	@Inject SearchHandler handler;

	@BeforeClass
    void injectDependencies() {
        ObjectGraph.create( new SearchTestModule() ).inject(this);
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

		when(req.getParameter("q")).thenReturn("ImmutableMap");

		handler.handle("", mock( Request.class ), req, resp);

		assertThat( new String( bas.toByteArray(), "utf-8"), startsWith("[{\"entry\":{\"artifact\":\"com/google/guava/guava/16.0.1\",\"className\":\"com.google.common.collect.ImmutableMap\",\"name\":\"\"},\"highlights\""));
	}

	@Module( includes=LocalMavenConfig.class, overrides=true, injects=SearchHandlerTest.class)
	static class SearchTestModule {

		@Provides @Named("mvnRoot")
		IFileObject provideMavenRoot(IFileSystem inFS) {
			return new TestFileObject( mock( IFileSystem.class ), "/usr/blah");
		}

		@Provides @Singleton
		IFileSystem provideFileSystemManager() {
			return mock( IFileSystem.class );
		}
	}
}