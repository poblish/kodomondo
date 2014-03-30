/**
 * 
 */
package com.andrewregan.kodomondo.handlers;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.util.VersionComparator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class ListingsHandler extends AbstractHandler {

	@Named("mvnRoot")
	@Inject IFileObject mvnRoot;

	@Inject IFileSystem fs;
	@Inject ObjectMapper mapper;

	private static Pattern POM_PATTERN = Pattern.compile("META-INF/.*/pom.xml");
	private static Pattern REPACKAGED_PATTERN = Pattern.compile("/repackaged/");

	public void handle(final String target, final Request baseRequest, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {

		baseRequest.setHandled(true);

		final String path = baseRequest.getRequestURI().substring(1);
		IFileObject f = mvnRoot.getChild(path);

		if (!f.exists()) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (f.isDirectory()) {
			List<DirEntry> dirsList = Lists.newArrayList();
			Collection<String> versions = Lists.newArrayList();
			Collection<IFileObject> jars = Lists.newArrayList();

			IFileObject[] files = f.listFiles( new BadDirFilter() );

			for ( IFileObject each : files) {
				if (each.isDirectory()) {
					if (Character.isDigit( each.getName().charAt(0) )) {
						versions.add( each.getName() );
					}
					else {
						dirsList.add( new DirEntry( "/" + each.getName() ) );
					}
				}
				else {
					if (isUselessFile(each) || each.getName().endsWith(".pom") || each.getName().endsWith("-javadoc.jar") ) {
						continue;
					}

					jars.add(each);
				}
			}

			final String output;

			if (!jars.isEmpty()) {
				if (jars.size() == 1) {
					handleFile( resp, jars.iterator().next());
				}
				else {
					System.err.println("> 1 match: " + jars);
					handleFile( resp, jars.iterator().next());
				}

				return;
			}
			else if (!versions.isEmpty()) {
				String highest = Ordering.from( new VersionComparator() ).max(versions);
				output = mapper.writeValueAsString( new VersionResponse(highest) );
			}
			else {
				output = mapper.writeValueAsString( new DirResponse(dirsList) );
			}

			resp.setContentType("application/json;charset=utf-8");
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentLength( output.length() );
			resp.getWriter().println(output);
		}
		else {
			handleFile( resp, f);
		}
	}

	public static boolean isUselessFile( IFileObject each) {
		return (!each.getName().endsWith(".jar") || each.getName().endsWith(".sha1") || each.getName().endsWith(".lastUpdated") || each.getName().endsWith(".repositories") || each.getName().endsWith("-sources.jar") || each.getName().endsWith("-shaded.jar") || each.getName().endsWith("-tests.jar"));
	}

	private void handleFile( final HttpServletResponse resp, final IFileObject f) throws IOException {
		List<ClassEntry> classesList = Lists.newArrayList();

		boolean gotClassAlready = false;
		boolean gotPomAlready = false;

		JarFile jf = fs.openJar(f);
		try {
			Enumeration<JarEntry> theEntries = jf.entries();
			while (theEntries.hasMoreElements()) {
				JarEntry eachEntry = theEntries.nextElement();
				if (eachEntry.isDirectory()) {
					continue;
				}

				String eachName = eachEntry.getName();

				// Fairly lousy attempt to deal with shaded JARs. Essentially it boils down to using a discovered pom.xml as 
				// a divider between the first set of classes (which we *assume* are the 'real' classes) and the next class
				// hierarchy. This may all be ultimately fruitless...

				if (POM_PATTERN.matcher(eachName).matches()) {
					if (gotClassAlready && gotPomAlready) {
						System.err.println("Already got classes, so skip shaded JARs: " + eachName + " in " + f);
						break;
					}

					gotPomAlready = true;
				}

				if (!eachName.endsWith(".class") || eachName.contains("$") || /* Skip 'com.google.appengine.labs.repackaged.org.json.JSONArray' etc. */ REPACKAGED_PATTERN.matcher(eachName).find()) {
					continue;
				}

				if (eachName.startsWith("scala/")) {  // FIXME Add restriction to DataSource def / handler
					continue;
				}

				classesList.add( new ClassEntry( eachName.substring( 0, eachName.length() - 6) .replace('/', '.')) );

				gotClassAlready = true;
			}
		}
		finally {
			jf.close();
		}

		final String mavenRelJarPath = f.getPathRelativeToFile(mvnRoot);

		final String output = mapper.writeValueAsString( new ClassResponse( mavenRelJarPath, classesList) );

		resp.setContentType("application/json;charset=utf-8");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentLength( output.length() );
		resp.getWriter().println(output);
	}

	// FIXME See IndexerService
	public static class BadDirFilter implements FileFilter {

		@Override
		public boolean accept( File pathname) {
			return !pathname.getName().startsWith(".") && !pathname.getAbsolutePath().contains("com/google/collections");
		}
	}

	private static class VersionResponse {
		final String version;
	
		public VersionResponse( String inVersion) {
			this.version = inVersion;
		}
	
		@JsonProperty("version")
		public String geVersion() {
			return version;
		}
	}
	
	private static class DirResponse {
		final List<DirEntry> dirs;
	
		public DirResponse( final List<DirEntry> inClasses) {
			this.dirs = inClasses;
		}
	
		@JsonProperty("dirs")
		public List<DirEntry> getClasses() {
			return dirs;
		}
	}
	
	private static class DirEntry {
		final String dirName;
	
		public DirEntry(String inDirName) {
			this.dirName = inDirName;
		}
	
		@JsonProperty("dir")
		public String getClassName() {
			return dirName;
		}
	}
	
	private static class ClassResponse {
		final String name;
		final List<ClassEntry> classes;
	
		public ClassResponse( String inName, List<ClassEntry> inClasses) {
			this.name = inName;
			this.classes = inClasses;
		}
	
		@JsonProperty("jar")
		public String getJarName() {
			return name;
		}
	
		@JsonProperty("classes")
		public List<ClassEntry> getClasses() {
			return classes;
		}
	}
	
	private static class ClassEntry {
		final String className;
	
		public ClassEntry(String inClassName) {
			this.className = inClassName;
		}
	
		@JsonProperty("class")
		public String getClassName() {
			return className;
		}
	}
}