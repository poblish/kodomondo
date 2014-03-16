package com.andrewregan.kodomondo.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.DaggerModule;
import com.sun.net.httpserver.Headers;

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
	public void testHandle() throws IOException {
		final com.sun.net.httpserver.HttpExchange req = mock( com.sun.net.httpserver.HttpExchange.class );
		when(req.getRequestURI()).thenReturn( URI.create("http://localhost:2000/launch/org.apache.lucene.analysis.Analyzer?artifact=org/apache/lucene/lucene-core/4.5.1?source=1") );
		when(req.getResponseHeaders()).thenReturn( new Headers() );
		when(req.getResponseBody()).thenReturn( new ByteArrayOutputStream() );
		handler.handle(req);
	}

	@Module( includes=DaggerModule.class, overrides=true, injects=LaunchHandlerTest.class)
	static class TestModule {}
}