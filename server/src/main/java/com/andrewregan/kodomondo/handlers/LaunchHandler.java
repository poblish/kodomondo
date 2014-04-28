/**
 * 
 */
package com.andrewregan.kodomondo.handlers;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.tasks.SourceDownloaderFactory;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class LaunchHandler extends AbstractHandler {

	private File tempSrcDownloadDir;

	@Inject IFileSystem fs;
	@Inject SourceDownloaderFactory srcDownloaderFactory;

	@Named("mvnRoot")
	@Inject IFileObject mvnRoot;

	private final static Logger LOG = LoggerFactory.getLogger( LaunchHandler.class );

	public LaunchHandler() {
		tempSrcDownloadDir = Files.createTempDir();
		tempSrcDownloadDir.deleteOnExit();
	}

	public void handle(final String target, final Request baseRequest, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {
		final String clazz = baseRequest.getRequestURI().substring(8);  // '/launch/...'

		baseRequest.setHandled(true);

		final IFileObject artifactDir = mvnRoot.getChild( req.getParameter("artifact") );
		final boolean isJar = req.getParameter("jar") != null;
		final boolean isSource = req.getParameter("source") != null;
		
		if (artifactDir != null && ( isJar || isSource)) {
			if (artifactDir.isDirectory()) {
				IFileObject[] files = artifactDir.listFiles( new FileFilter() {

					public boolean accept( File other) {
						return !other.getName().startsWith(".") && other.getName().endsWith(".jar") && !other.getName().endsWith("-shaded.jar") && !other.getName().endsWith("-javadoc.jar") && !other.getName().endsWith("-tests.jar");
					}} );

				LOG.debug( "LAUNCH: matching JARS: " + Arrays.toString(files));

				final String expectedSourceFileName = clazz.replace('.', '/') + ".java";
//					LOG.debug( "expectedSourceFileName: " + expectedSourceFileName);

				boolean sourceJarFound = false;

				for ( IFileObject eachJar : files) {
					if ( isSource && eachJar.getName().endsWith("-sources.jar")) {
						sourceJarFound = true;
						JarFile jf = fs.openJar(eachJar);
						try {
							Enumeration<JarEntry> theEntries = jf.entries();
							while (theEntries.hasMoreElements()) {
								JarEntry eachEntry = theEntries.nextElement();
								if (eachEntry.isDirectory()) {
									continue;
								}

								if (expectedSourceFileName.equals( eachEntry.getName() )) {
									LOG.debug( "SOURCES: found: " + expectedSourceFileName);
									final String javaSrc = new String( ByteStreams.toByteArray( jf.getInputStream(eachEntry) ), Charset.forName("utf-8"));

									if (!tempSrcDownloadDir.exists()) {
										tempSrcDownloadDir = Files.createTempDir();
										tempSrcDownloadDir.deleteOnExit();
									}
									
									File javaSrcFile = new File( tempSrcDownloadDir, expectedSourceFileName);
									Files.createParentDirs(javaSrcFile);
									Files.write( javaSrc, javaSrcFile, Charset.forName("utf-8"));
									Desktop.getDesktop().open(javaSrcFile);  // Launch .java in whatever viewer/editor
									break;
								}
							}
						}
						catch (Throwable tt) {
							LOG.error( "", tt);  // FIXME
							Throwables.propagate(tt);
						}
						finally {
							jf.close();
						}
					}
					else if ( isJar && eachJar.getName().endsWith("jar")) {
						eachJar.launch();
					}
				}

				if ( isSource && !sourceJarFound) {
					srcDownloaderFactory.create(artifactDir).call();
				}
			}
			else {
				System.err.println( artifactDir + " is not a directory!");
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}

		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().println("OK");
	}
}