/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.maven.ArtifactDesc;
import com.google.common.collect.Lists;

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

		try {
			Properties props = new Properties();
			props.put("groupId", artifact.getGroupId());
			props.put("artifactId", artifact.getArtifactId());
			props.put("version", artifact.getVersion());
			props.put("classifier", "sources");
			
			InvocationRequest request = new DefaultInvocationRequest();
			request.setProperties(props);
			request.setGoals( Lists.newArrayList("org.apache.maven.plugins:maven-dependency-plugin:2.8:get") );

			Invoker invoker = new DefaultInvoker();
			invoker.setMavenHome( new File("/usr/local/") );  // FIXME!
			invoker.setOutputHandler(null);
			int result = invoker.execute( request ).getExitCode();

			System.out.println( result == 0 ? "SUCCESS" : "FAIL");
		}
		catch (Throwable e) {
			e.printStackTrace(); // Throwables.propagate(e);
		}
	}
}
