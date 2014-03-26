/**
 * 
 */
package com.andrewregan.kodomondo.handlers;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.tasks.SourceDownloaderFactory;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class LaunchHandler implements HttpHandler {

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

	public void handle( final HttpExchange t) throws IOException {
		final String clazz = t.getRequestURI().getPath().substring(8);  // '/launch/...'

		IFileObject artifactDir = null;
		boolean isJar = false;
		boolean isSource = false;

		for ( NameValuePair each : URLEncodedUtils.parse( t.getRequestURI(), "utf-8")) {
			if (each.getName().equals("artifact")) {
				artifactDir = mvnRoot.getChild( each.getValue() );
			}
			else if (each.getName().equals("jar")) {
				isJar = true;
			}
			else if (each.getName().equals("source")) {
				isSource = true;
			}
		}
		
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
					srcDownloaderFactory.create(artifactDir).run();
				}
			}
			else {
				System.err.println( artifactDir + " is not a directory!");
				t.sendResponseHeaders( 404, 0);
				t.getResponseBody().close();
				return;
			}
		}

		t.sendResponseHeaders(200, 2);
		OutputStream os = t.getResponseBody();
		os.write( "OK".getBytes() );
		os.close();
	}
}