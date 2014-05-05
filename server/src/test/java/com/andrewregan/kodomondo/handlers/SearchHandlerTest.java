package com.andrewregan.kodomondo.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.StringText;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.indices.TypeMissingException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.LocalMavenConfig;
import com.andrewregan.kodomondo.es.EsUtils;
import com.andrewregan.kodomondo.fs.TestFileObject;
import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.maven.util.ArtifactDesc;
import com.andrewregan.kodomondo.tasks.JavaDocDownloaderFactory;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;

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
	@Inject Client esClient;

	@Inject SearchHandler handler;
	@Inject IFileSystem fs;
	@Inject JavaDocDownloaderFactory docsDownloaderFactory;

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

		// Clear down...
		try {
			esClient.admin().indices().prepareDeleteMapping().setIndices("datasource.local-maven").setType("_all").execute().actionGet();
		}
		catch (TypeMissingException e) {
			// Ignore
		}
		catch (IndexMissingException e) {
			// Ignore
		}

		// Index something...
		docsDownloaderFactory.create( fs.resolveFile("/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1.jar") ).call();

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
			return new TestFileObject( getFS(), "/usr/blah");
		}

		@Provides @Singleton
		IFileSystem provideFileSystemManager() {
			return getFS();
		}

		private IFileSystem getFS() {
			final IFileSystem ifs = mock( IFileSystem.class );

			JarFile jf = mock( JarFile.class );
			final JarEntry je1 = new JarEntry("com/google/common/collect/ImmutableMap.html");
			final List<JarEntry> jl = Lists.newArrayList(je1);

			when(jf.entries()).thenReturn( Iterators.asEnumeration( jl.iterator() ) );

			try {
				when(jf.getInputStream( eq(je1) )).thenReturn( new ByteArrayInputStream( "ImmutableMap: An immutable, hash-based Map with reliable user-specified iteration order. Does not permit null keys or values".getBytes("utf-8") ) );
				when( ifs.openJar( any( IFileObject.class ) ) ).thenReturn(jf);
			}
			catch (IOException e) {
				Throwables.propagate(e);
			}

			when (ifs.resolveFile( anyString() ))
				.thenReturn( new TestFileObject( ifs, "/usr/blah/com/google/guava/guava/16.0.1/guava-16.0.1.jar") );

			when (ifs.resolveFile( any( IFileObject.class ), anyString() )).thenAnswer( new Answer<IFileObject>() {

				@Override
				public IFileObject answer( InvocationOnMock invocation) throws Throwable {
					String name = (String) invocation.getArguments()[1];
					return name.endsWith(".jar") ? new TestFileObject( ifs, "/usr/blah/" + name, false) : new TestFileObject( ifs, "/usr/blah/" + name, true, new IFileObject[]{});
				}
			} );

			when(ifs.toArtifact( any( IFileObject.class ) )).thenReturn( new ArtifactDesc("com.google.guava", "guava", "16.0.1") );

			return ifs;
		}

		// Horrible code to simulate download of a file into a dir
		private static class StatefulTestDir extends TestFileObject {

			public StatefulTestDir(IFileSystem fs, String path) {
				super(fs, path, true);
				this.children = new IFileObject[]{};
			}

			@Override
			public IFileObject[] listFiles() {
				IFileObject[] ret = children.clone();
				children = ObjectArrays.concat( children, new TestFileObject( this.fs, getAbsolutePath() + "/child." + ret.length, false));
				return ret;
			}
		}
	}
}