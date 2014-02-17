package com.andrewregan.kodomondo;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Hello world!
 *
 */
@SuppressWarnings("restriction")
public class LocalMavenServer 
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static void main(String[] args) throws Exception {
		String origMvnRoot = System.getenv("M2_HOME");
		final String mvnRoot = (( origMvnRoot != null && !origMvnRoot.isEmpty()) ? origMvnRoot : "~/.m2").replace("~",System.getProperty("user.home")) + "/repository/";
		System.out.println(mvnRoot);

		HttpServer server = HttpServer.create(new InetSocketAddress(2000), 0);
		server.createContext("/", new MyHandler(mvnRoot));
		server.setExecutor(null); // creates a default executor
		server.start();
	}

	static class MyHandler implements HttpHandler {

		private final String mvnRoot;

		public MyHandler(String mvnRoot) {
			this.mvnRoot = mvnRoot;
		}

		public void handle( final HttpExchange t) throws IOException {

			final String path = t.getRequestURI().getPath().substring(1);
			File f = new File( mvnRoot, path);

			if (!f.exists()) {
				t.sendResponseHeaders( 404, 0);
				t.getResponseBody().close();
				return;
			}


			if (f.isDirectory()) {
				List<DirEntry> dirsList = Lists.newArrayList();

				File[] files = f.listFiles( new FileFilter() {

					public boolean accept( File pathname) {
						return !pathname.getName().startsWith(".");
					}} );

				for ( File each : files) {
					dirsList.add( new DirEntry( "/" + each.getName() ) );
				}

				final String output = MAPPER.writeValueAsString( new DirResponse( f.getAbsolutePath(), dirsList) );

				t.sendResponseHeaders(200, output.length());
				OutputStream os = t.getResponseBody();
				os.write( output.getBytes() );
				os.close();
			}
			else {
				List<ClassEntry> classesList = Lists.newArrayList();

				JarFile jf = new JarFile(f);
				try {
					Enumeration<JarEntry> theEntries = jf.entries();
					while (theEntries.hasMoreElements()) {
						JarEntry eachEntry = theEntries.nextElement();
						if (eachEntry.isDirectory()) {
							continue;
						}
	
						String	eachName = eachEntry.getName();
	
						if (!eachName.endsWith(".class") || eachName.contains("$")) {
							continue;
						}
	
	//					System.out.println( "Class: " + eachName);
	
						classesList.add( new ClassEntry( eachName.substring( 0, eachName.length() - 6) .replace('/', '.')) );
	//					InputStream theStream = jf.getInputStream(eachEntry);
	//					ClassReader cr = new ClassReader( Files.toByteArray(f) );
	//					String[] ifs = cr.getInterfaces();
	//					System.out.println( Arrays.toString(ifs) );
					}
				}
				finally {
					jf.close();
				}

				final String output = MAPPER.writeValueAsString( new ClassResponse( f.getAbsolutePath(), classesList) );

				t.sendResponseHeaders(200, output.length());
				OutputStream os = t.getResponseBody();
				os.write( output.getBytes() );
				os.close();
			}

		}
	}

	private static class DirResponse {
		final String name;
		final List<DirEntry> dirs;

		public DirResponse( String inName, List<DirEntry> inClasses) {
			this.name = inName;
			this.dirs = inClasses;
		}

//		@JsonProperty("name")
//		public String getJarName() {
//			return name;
//		}

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

		@JsonProperty("name")
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
