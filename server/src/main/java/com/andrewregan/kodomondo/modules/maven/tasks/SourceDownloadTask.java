/**
 * 
 */
package com.andrewregan.kodomondo.modules.maven.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.modules.maven.util.ArtifactDesc;
import com.andrewregan.kodomondo.modules.maven.util.MavenHome;
import com.andrewregan.kodomondo.util.DirectoryContentsRestoration;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class SourceDownloadTask implements Callable<Integer> {

	private IFileSystem fs;

	private final IFileObject artifactFile;
	private IFileObject mvnRoot;

	private final static Logger LOG = LoggerFactory.getLogger( SourceDownloadTask.class );

	/**
	 * @param artifactDir
	 */
	public SourceDownloadTask( IFileObject inArtifact, IFileSystem fs, IFileObject mvnRoot) {
		this.artifactFile = inArtifact;
		this.fs = checkNotNull(fs);
		this.mvnRoot = checkNotNull(mvnRoot);
	}

	@Override
	public Integer call() {
		LOG.debug("Source JAR not found for " + artifactFile);

		final IFileObject relativeRef = artifactFile.getFileRelativeToFile(mvnRoot);

		ArtifactDesc artifact = fs.toArtifact(relativeRef);

		DirectoryContentsRestoration restorer = new DirectoryContentsRestoration( artifactFile.getParent() );

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
			invoker.setMavenHome( MavenHome.getMavenHome() );
			invoker.setOutputHandler(null);
			int result = invoker.execute( request ).getExitCode();

			LOG.debug( result == 0 ? "SUCCESS" : "FAIL");

			return result;
		}
		catch (MavenInvocationException e) {
			throw Throwables.propagate(e);
		}
		finally {
			restorer.restore();

			LOG.debug("> DONE: " + artifact);
		}
	}
}
