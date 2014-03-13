/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import java.io.File;
import java.io.IOException;

import com.andrewregan.kodomondo.maven.ArtifactDesc;
import com.google.common.base.Throwables;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class SourceDownloadTask implements Runnable {

	private final File artifactFile;

	/**
	 * @param artifactDir
	 */
	public SourceDownloadTask( File inArtifact) {
		artifactFile = inArtifact;
	}

	@Override
	public void run() {
		System.out.println("Source JAR not found for " + artifactFile);

		ArtifactDesc artifact = ArtifactDesc.forFile(artifactFile);

		// FIXME Need security here!
		final String cmd = "mvn -DgroupId=" + artifact.getGroupId() + " -DartifactId=" + artifact.getArtifactId() + " -Dversion=" + artifact.getVersion() + " -Dclassifier=sources org.apache.maven.plugins:maven-dependency-plugin:2.8:get";
		System.err.println("cmd = " + cmd);

		try {
			int retVal = new ProcessBuilder(cmd).start().waitFor();
			if ( retVal == 0) {
				System.out.println("Source JAR downloaded for " + artifactFile);
			}
			else {
				System.out.println("Source JAR FAILED (" + retVal + ") for " + artifactFile);
			}
		}
		catch (InterruptedException e) {
			Throwables.propagate(e);
		}
		catch (IOException e) {
			Throwables.propagate(e);
		}
	}
}
