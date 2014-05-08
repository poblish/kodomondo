/**
 * 
 */
package com.andrewregan.kodomondo.modules.maven.tasks;

import com.andrewregan.kodomondo.fs.api.IFileObject;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public interface PomIndexerFactory {

	PomIndexingTask create( IFileObject artifact, IFileObject docJar);
}
