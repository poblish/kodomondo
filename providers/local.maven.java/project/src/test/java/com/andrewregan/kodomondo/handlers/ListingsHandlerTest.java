package com.andrewregan.kodomondo.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.DaggerModule;
import com.andrewregan.kodomondo.fs.TestFileSystem;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.sun.net.httpserver.Headers;

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
	public void testHandle() throws IOException {
		final com.sun.net.httpserver.HttpExchange req = mock( com.sun.net.httpserver.HttpExchange.class );
		when(req.getRequestURI()).thenReturn( URI.create("http://localhost:2000/com/codahale") );
		when(req.getResponseHeaders()).thenReturn( new Headers() );
		when(req.getResponseBody()).thenReturn( new ByteArrayOutputStream() );
		handler.handle(req);
	}

	@Module( includes=DaggerModule.class, overrides=true, injects=ListingsHandlerTest.class)
	static class TestModule {

		@Provides
		@Singleton
		IFileSystem provideFileSystemManager() {
			return new TestFileSystem();
		}
	}
}