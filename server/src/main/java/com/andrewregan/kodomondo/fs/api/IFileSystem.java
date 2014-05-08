/**
 * 
 */
package com.andrewregan.kodomondo.fs.api;

import java.io.IOException;
import java.util.jar.JarFile;

import com.andrewregan.kodomondo.modules.maven.util.ArtifactDesc;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public interface IFileSystem {
	IFileObject resolveFile( IFileObject parent, String path);
	IFileObject resolveFile( String path);
	JarFile openJar( final IFileObject file) throws IOException;
	ArtifactDesc toArtifact( final IFileObject file);
}
