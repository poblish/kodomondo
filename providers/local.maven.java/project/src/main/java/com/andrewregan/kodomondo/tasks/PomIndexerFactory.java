/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import java.io.File;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public interface PomIndexerFactory {

	PomIndexingTask create( File artifact, File docJar);
}
