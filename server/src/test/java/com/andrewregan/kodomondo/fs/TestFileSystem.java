/**
 * 
 */
package com.andrewregan.kodomondo.fs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.maven.util.ArtifactDesc;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class TestFileSystem implements IFileSystem {

	@Override
	public IFileObject resolveFile( String path) {
		switch (path) {
			case "/Users/andrewregan/.m2/repository/":  // FIXME!
				return new TestFileObject(path);
			default:
				return null;
		}
	}

	@Override
	public IFileObject resolveFile( IFileObject parent, String path) {
		return new TestFileObject(parent.getAbsolutePath() + "/" + path);
	}

	@Override
	public JarFile openJar( IFileObject file) throws IOException {
		JarFile jf = mock( JarFile.class );

		final JarEntry je1 = new JarEntry("je1");

		final List<JarEntry> jl = Lists.newArrayList(je1);
		when(jf.entries()).thenReturn( Iterators.asEnumeration( jl.iterator() ) );

		return jf;
	}

	@Override
	public ArtifactDesc toArtifact( IFileObject file) {
		throw new RuntimeException("unimpl");
	}
}