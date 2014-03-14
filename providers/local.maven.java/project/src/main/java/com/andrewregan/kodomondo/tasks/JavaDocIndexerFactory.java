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
public interface JavaDocIndexerFactory {

	JavaDocIndexingTask create( File artifact, File docJar);
}
