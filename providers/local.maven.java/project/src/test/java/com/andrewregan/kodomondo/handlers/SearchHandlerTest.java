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
public class SearchHandlerTest {

	@Inject SearchHandler handler;

	@BeforeClass
    void injectDependencies() {
        ObjectGraph.create( new TestModule() ).inject(this);
    }

	@Test
	public void testHandle() throws IOException {
		final com.sun.net.httpserver.HttpExchange req = mock( com.sun.net.httpserver.HttpExchange.class );
		when(req.getRequestURI()).thenReturn( URI.create("http://localhost:2000/search?q=ImmutableMap") );
		when(req.getResponseHeaders()).thenReturn( new Headers() );
		when(req.getResponseBody()).thenReturn( new ByteArrayOutputStream() );
		handler.handle(req);
	}

	@Module( includes=DaggerModule.class, overrides=true, injects=SearchHandlerTest.class)
	static class TestModule {}
}