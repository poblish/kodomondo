/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Throwables;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class SourceDownloadTask implements Runnable {

	private final File artifact;

	/**
	 * @param artifactDir
	 */
	public SourceDownloadTask( File inArtifact) {
		artifact = inArtifact;
	}

	@Override
	public void run() {
		String artifactStr = artifact.getPath();
		System.err.println("Source JAR not found for " + artifactStr);

		int last = artifactStr.lastIndexOf('/');
		String vers = artifactStr.substring( last + 1);
		int nextPos = artifactStr.lastIndexOf('/', last - 1);
		String artifactId = artifactStr.substring( nextPos + 1, last);
		String groupId = artifactStr.substring( /* Skip /*/ 1, nextPos).replace( '/', '.');

		// FIXME Need security here!
		final String cmd = "mvn -DgroupId=" + groupId + " -DartifactId=" + artifactId + " -Dversion=" + vers + " -Dclassifier=sources org.apache.maven.plugins:maven-dependency-plugin:2.8:get";
		System.err.println("cmd = " + cmd);

		try {
			Process p = new ProcessBuilder(cmd).start();
		}
		catch (IOException e) {
			Throwables.propagate(e);
		}
	}
}
