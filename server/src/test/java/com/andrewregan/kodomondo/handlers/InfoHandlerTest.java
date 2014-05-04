package com.andrewregan.kodomondo.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.LocalMavenConfig;
import com.andrewregan.kodomondo.fs.TestFileObject;
import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

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

	@Module( includes=LocalMavenConfig.class, overrides=true, injects=InfoHandlerTest.class)
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

			JarFile jf = mock( JarFile.class );
			final JarEntry je1 = new JarEntry("org/hibernate/criterion/AggregateProjection.class");
			final List<JarEntry> jl = Lists.newArrayList(je1);

			try {
				when(jf.entries()).thenReturn( Iterators.asEnumeration( jl.iterator() ) );
				when(jf.getInputStream( eq(je1) )).thenReturn( new ByteArrayInputStream( Files.toByteArray( new File("src/test/resources/Test.class.bin") ) ) );
	
				when (ifs.resolveFile( any( IFileObject.class ), anyString() ))
					.thenReturn( new TestFileObject( ifs, "/usr/blah/org/hibernate/hibernate-core/4.3.0.Final/hibernate-core-4.3.0.Final.jar", false, new IFileObject[]{}) );

				when( ifs.openJar( any( IFileObject.class ) ) ).thenReturn(jf);
			}
			catch (IOException e) {
				Throwables.propagate(e);
			}

			return ifs;
		}
	}
}