/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.maven.ArtifactDesc;
import com.google.common.base.Throwables;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class SourceDownloadTask implements Runnable {

	private final IFileObject artifactFile;

	private IFileSystem fs;

	/**
	 * @param artifactDir
	 */
	public SourceDownloadTask( IFileObject inArtifact, IFileSystem fs) {
		this.artifactFile = inArtifact;
		this.fs = checkNotNull(fs);
	}

	@Override
	public void run() {
		System.out.println("Source JAR not found for " + artifactFile);

		ArtifactDesc artifact = fs.toArtifact(artifactFile);

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
