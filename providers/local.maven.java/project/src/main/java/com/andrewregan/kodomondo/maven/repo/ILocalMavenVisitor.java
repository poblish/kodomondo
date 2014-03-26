/**
 * 
 */
package com.andrewregan.kodomondo.maven.repo;

import com.andrewregan.kodomondo.fs.api.IFileObject;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public interface ILocalMavenVisitor {

	boolean acceptDirectory( IFileObject inDir);
	void doneDirectory( IFileObject inDir);

	void foundPom( IFileObject dir, IFileObject pomFile);
	void foundJavaDoc( String relativePath, IFileObject docJar);
	void missingJavaDoc( IFileObject artifactDir);
}