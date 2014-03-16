/**
 * 
 */
package com.andrewregan.kodomondo.fs.api;

import java.io.IOException;
import java.util.jar.JarFile;

import com.andrewregan.kodomondo.maven.ArtifactDesc;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public interface IFileSystem {
	IFileObject resolveFile( String path);
	JarFile openJar( final IFileObject file) throws IOException;
	ArtifactDesc toArtifact( final IFileObject file);
}
