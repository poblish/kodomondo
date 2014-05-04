/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
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
import com.andrewregan.kodomondo.maven.util.ArtifactDesc;
import com.andrewregan.kodomondo.util.DirectoryContentsRestoration;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class JavaDocDownloadTask implements Callable<Integer> {

	private final IFileObject artifactFile;

	private IFileSystem fs;
	private JavaDocIndexerFactory indexerFactory;
	private IFileObject mvnRoot;

	private final static Logger LOG = LoggerFactory.getLogger( JavaDocDownloadTask.class );

	/**
	 * @param artifactDir
	 */
	public JavaDocDownloadTask( IFileObject inArtifact, IFileSystem fs, IFileObject mvnRoot, JavaDocIndexerFactory indexerFactory) {
		this.artifactFile = inArtifact;
		this.fs = checkNotNull(fs);
		this.indexerFactory = checkNotNull(indexerFactory);
		this.mvnRoot = checkNotNull(mvnRoot);
	}

	@Override
	public Integer call() {
		final IFileObject relativeRef = artifactFile.getFileRelativeToFile(mvnRoot);

		ArtifactDesc artifact = fs.toArtifact(relativeRef);
		LOG.debug("> Try to download JAR for " + artifact + " (" + relativeRef + ")");

		DirectoryContentsRestoration restorer = new DirectoryContentsRestoration( artifactFile.getParent() );

		try {
			Properties props = new Properties();
			props.put("groupId", artifact.getGroupId());
			props.put("artifactId", artifact.getArtifactId());
			props.put("version", artifact.getVersion());
			props.put("classifier", "javadoc");
			
			InvocationRequest request = new DefaultInvocationRequest();
			request.setProperties(props);
			request.setGoals( Lists.newArrayList("org.apache.maven.plugins:maven-dependency-plugin:2.8:get") );

			final String m2Loc = System.getenv("M2");

			Invoker invoker = new DefaultInvoker();
			invoker.setMavenHome( new File( m2Loc != null ? m2Loc : "/usr/local/") );
			invoker.setOutputHandler(null);

			int result = invoker.execute( request ).getExitCode();
			if ( result == 0)
			{
				LOG.debug("SUCCESS");

				if (restorer.contentsChanged()) {
					throw new RuntimeException("Nothing downloaded");
				}

				// Must be synchronous, otherwise we'd have to hand over responsibility for cleanup
				indexerFactory.create( artifact.toPath(), mvnRoot.getChild( artifact.toPath() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + "-javadoc.jar")).run();  // Yes, in same Thread
			}
			else {
				LOG.debug("FAIL");
			}

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