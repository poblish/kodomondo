/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

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
