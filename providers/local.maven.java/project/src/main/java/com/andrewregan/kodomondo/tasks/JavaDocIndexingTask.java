/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import java.io.File;

/**
 * Try http://localhost:2000/com/higherfrequencytrading/chronicle/1.7.1-SNAPSHOT
 *
 * @author andrewregan
 *
 */
public class JavaDocIndexingTask implements Runnable {

	private final File docJar;

	public JavaDocIndexingTask( File javaDocJar) {
		docJar = javaDocJar;
	}

	public void run() {
		System.out.println("> Start indexing " + docJar);
	}
}
