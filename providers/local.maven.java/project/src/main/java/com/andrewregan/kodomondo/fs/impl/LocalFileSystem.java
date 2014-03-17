package com.andrewregan.kodomondo.fs.impl;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.maven.ArtifactDesc;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class LocalFileSystem implements IFileSystem {

	@Override
	public IFileObject resolveFile( String path) {
		return new LocalFile( this, new File(path));
	}

	@Override
	public IFileObject resolveFile( IFileObject parent, String path) {
		return new LocalFile( this, new File( ((LocalFile) parent).getFile(), path) );
	}

	/* (non-Javadoc)
	 * @see com.andrewregan.kodomondo.fs.api.IFileSystem#openJar(com.andrewregan.kodomondo.fs.api.IFileObject)
	 */
	@Override
	public JarFile openJar( IFileObject file) throws IOException {
		return new JarFile( ((LocalFile) file).getFile() );
	}

	/* (non-Javadoc)
	 * @see com.andrewregan.kodomondo.fs.api.IFileSystem#toArtifact(com.andrewregan.kodomondo.fs.api.IFileObject)
	 */
	@Override
	public ArtifactDesc toArtifact( IFileObject file) {
		return ArtifactDesc.forFile( ((LocalFile) file).getFile() );
	}
}