package com.andrewregan.kodomondo;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.objectweb.asm.ClassReader;
import org.yaml.snakeyaml.Yaml;

import com.andrewregan.kodomondo.api.IDataSource;
import com.andrewregan.kodomondo.ds.AbstractDataSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * Hello world!
 *
 */
public class LocalMavenServer 
{
	private static final Map<String,IDataSource> dataSources = Maps.newHashMap();

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static void main(String[] args) throws Exception {
		String origMvnRoot = System.getenv("M2_HOME");
		final String mvnRoot = (( origMvnRoot != null && !origMvnRoot.isEmpty()) ? origMvnRoot : "~/.m2").replace("~",System.getProperty("user.home")) + "/repository/";
		System.out.println(mvnRoot);

		readConfig();

		final Configuration theConfig = new Configuration();
		theConfig.setDirectoryForTemplateLoading( new File("src/main/resources/templates"));

		HttpServer server = HttpServer.create(new InetSocketAddress(2000), 0);
		server.createContext("/", new ListingsHandler(mvnRoot));
		server.createContext("/launch", new LaunchHandler(mvnRoot));
		server.createContext("/datasource", new DataSourceHandler());
		server.createContext("/info", new InfoHandler(mvnRoot, theConfig));
		server.setExecutor(null); // creates a default executor
		server.start();
	}

	@SuppressWarnings("unchecked")
	private static void readConfig() {
		try {
			for ( Object eachEntry : new Yaml().loadAll( Files.toString( new File( "src/main/resources/conf", "ds.yaml"), Charset.forName("utf-8")))) {
				Map<String,Object> eachDsEntry = (Map<String,Object>) eachEntry;

				final String dsName = Strings.emptyToNull((String) eachDsEntry.get("name"));
				final String className = Strings.emptyToNull((String) eachDsEntry.get("implClass"));

				final IDataSource dsInst = ( className != null) ? (IDataSource) Class.forName(className).getConstructor( String.class ).newInstance(dsName) : new AbstractDataSource(dsName);
				dsInst.setKeyTerms((List<String>) eachDsEntry.get("key-terms") );
				dsInst.setStopwords((List<String>) eachDsEntry.get("stopwords") );

				final String parentName = Strings.emptyToNull((String) eachDsEntry.get("inherit"));  // FIXME Should be *list* for multiple!
				final IDataSource parent = ( parentName != null) ? /* FIXME error handling! */ dataSources.get(parentName) : null;
				dsInst.setParent(parent);

				dataSources.put( dsName, dsInst);
			}
		}
		catch (IOException e) {
			Throwables.propagate(e);
		}
		catch (ReflectiveOperationException e) {
			Throwables.propagate(e);
		}
	}

	static class InfoHandler implements HttpHandler {

		private final String mvnRoot;
		private final Configuration config;

		public InfoHandler( String mvnRoot, final Configuration inConfig) {
			this.mvnRoot = mvnRoot;
			config = inConfig;
		}

		/* (non-Javadoc)
		 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
		 */
		public void handle( HttpExchange t) throws IOException {

			String className = null;
			String artifactName = null;
			String jarName = null;

			for ( NameValuePair each : URLEncodedUtils.parse( t.getRequestURI(), "utf-8")) {
				if (each.getName().equals("class")) {
					className = each.getValue();
				}
				else if (each.getName().equals("artifact")) {
					artifactName = each.getValue();
				}
				else if (each.getName().equals("jar")) {
					jarName = each.getValue();
				}
			}

			final Map<String,Object> resultsModel = Maps.newHashMap();
			populateModelFromInputs( resultsModel, className, artifactName, jarName);

			t.getResponseHeaders().put( "Content-type", Lists.newArrayList("text/html"));

			StringWriter sw = new StringWriter();
			try {
				config.getTemplate("info_tmpl.ftl").process( resultsModel, sw);
			} catch (TemplateException e) {
				Throwables.propagate(e);
			}

			final String output = sw.toString();
			t.sendResponseHeaders(200, output.length());
			OutputStream os = t.getResponseBody();
			os.write( output.getBytes("utf-8") );
			os.close();
		}

		private void populateModelFromInputs( Map<String, Object> resultsModel, String className, String artifactName, String jarName) {
			try ( JarFile jf = new JarFile( new File( mvnRoot, jarName))) {
				String classNameToMatch = className.replace( '.', '/') + ".class";

				Enumeration<JarEntry> theEntries = jf.entries();
				while (theEntries.hasMoreElements()) {
					JarEntry eachEntry = theEntries.nextElement();
					if (eachEntry.isDirectory() || !eachEntry.getName().equals(classNameToMatch)) {
						continue;
					}

					InputStream theStream = jf.getInputStream(eachEntry);
					byte[] b = ByteStreams.toByteArray(theStream);

					ClassReader cr = new ClassReader(b);
					String[] ifs = cr.getInterfaces();

					String fqn = cr.getClassName();

					resultsModel.put("interfaces", ifs);
					resultsModel.put("classname", fqn.substring( fqn.lastIndexOf('/') + 1));
					resultsModel.put("package", fqn.substring( 0, fqn.lastIndexOf('/') ).replace('/', '.'));
					resultsModel.put("superClass", cr.getSuperName());

					break;
				}

			}
			catch (IOException e) {
				Throwables.propagate(e);
			}
		}		
	}

	static class DataSourceHandler implements HttpHandler {

		/* (non-Javadoc)
		 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
		 */
		public void handle( HttpExchange t) throws IOException {
			final String dsName = t.getRequestURI().getPath().substring(12);  // '/datasource/...'
			final IDataSource ds = dataSources.get(dsName);

			if ( ds != null) {
				final byte[] bs = MAPPER.writeValueAsBytes(ds);
				t.getResponseHeaders().put( "Content-type", Lists.newArrayList("application/json"));
				t.sendResponseHeaders(200, bs.length);
				OutputStream os = t.getResponseBody();
				os.write(bs);
				os.close();
			}
			else {
				t.sendResponseHeaders( 404, 0);
				t.getResponseBody().close();
			}
		}		
	}

	static class LaunchHandler implements HttpHandler {

		private final String mvnRoot;
		private File tempSrcDownloadDir;

		public LaunchHandler(String mvnRoot) {
			this.mvnRoot = mvnRoot;

			tempSrcDownloadDir = Files.createTempDir();
			tempSrcDownloadDir.deleteOnExit();
		}

		public void handle( final HttpExchange t) throws IOException {
			final String clazz = t.getRequestURI().getPath().substring(8);  // '/launch/...'

			File artifactDir = null;
			boolean isJar = false;
			boolean isSource = false;

			for ( NameValuePair each : URLEncodedUtils.parse( t.getRequestURI(), "utf-8")) {
				if (each.getName().equals("artifact")) {
					artifactDir = new File( mvnRoot, each.getValue());
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
					File[] files = artifactDir.listFiles( new FileFilter() {

						public boolean accept( File other) {
							return !other.getName().startsWith(".") && other.getName().endsWith(".jar") && !other.getName().endsWith("-shaded.jar") && !other.getName().endsWith("-javadoc.jar") && !other.getName().endsWith("-tests.jar");
						}} );

					System.out.println( "LAUNCH: matching JARS: " + Arrays.toString(files));

					final String expectedSourceFileName = clazz.replace('.', '/') + ".java";
//						System.out.println( "expectedSourceFileName: " + expectedSourceFileName);

					boolean sourceJarFound = false;

					for ( File eachJar : files) {
						if ( isSource && eachJar.getName().endsWith("-sources.jar")) {
							sourceJarFound = true;
							JarFile jf = new JarFile(eachJar);
							try {
								Enumeration<JarEntry> theEntries = jf.entries();
								while (theEntries.hasMoreElements()) {
									JarEntry eachEntry = theEntries.nextElement();
									if (eachEntry.isDirectory()) {
										continue;
									}

									if (expectedSourceFileName.equals( eachEntry.getName() )) {
										System.out.println( "SOURCES: found: " + expectedSourceFileName);
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
								tt.printStackTrace(); // Throwables.propagate(tt);
							}
							finally {
								jf.close();
							}
						}
						else if ( isJar && eachJar.getName().endsWith("jar")) {
							Desktop.getDesktop().open(eachJar);  // Launch JAR in whatever viewer/editor
						}
					}

					if ( isSource && !sourceJarFound) {
						System.err.println("Source JAR not found for " + artifactDir);
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

	static class ListingsHandler implements HttpHandler {

		private final String mvnRoot;

		public ListingsHandler(String mvnRoot) {
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
				Collection<String> versions = Lists.newArrayList();
				Collection<File> jars = Lists.newArrayList();

				File[] files = f.listFiles( new FileFilter() {

					public boolean accept( File pathname) {
						return !pathname.getName().startsWith(".") && !pathname.getAbsolutePath().contains("com/google/collections");
					}} );

				for ( File each : files) {
					if (each.isDirectory()) {
						if (Character.isDigit( each.getName().charAt(0) )) {
							versions.add( each.getName() );
						}
						else {
							dirsList.add( new DirEntry( "/" + each.getName() ) );
						}
					}
					else {
						if (!each.getName().endsWith(".jar") || each.getName().endsWith("-sources.jar") || each.getName().endsWith("-shaded.jar") || each.getName().endsWith("-javadoc.jar") || each.getName().endsWith("-tests.jar")) {
							continue;
						}

						jars.add(each);
					}
				}

				final String output;

				if (!jars.isEmpty()) {
					if (jars.size() == 1) {
						handleFile( t, jars.iterator().next());
					}
					else {
						System.err.println("> 1 match: " + jars);
						handleFile( t, jars.iterator().next());
					}
					return;
				}
				else if (!versions.isEmpty()) {
					String highest = Ordering.from( new VersionComparator() ).max(versions);
					output = MAPPER.writeValueAsString( new VersionResponse(highest) );
				}
				else {
					output = MAPPER.writeValueAsString( new DirResponse(dirsList) );
				}

				t.getResponseHeaders().put( "Content-type", Lists.newArrayList("application/json"));
				t.sendResponseHeaders(200, output.length());
				OutputStream os = t.getResponseBody();
				os.write( output.getBytes() );
				os.close();
			}
			else {
				handleFile( t, f);
			}
		}

		private static Pattern POM_PATTERN = Pattern.compile("META-INF/.*/pom.xml");
		private static Pattern REPACKAGED_PATTERN = Pattern.compile("/repackaged/");

		private void handleFile( final HttpExchange t, final File f) throws IOException {
			List<ClassEntry> classesList = Lists.newArrayList();

			boolean gotClassAlready = false;

			JarFile jf = new JarFile(f);
			try {
				Enumeration<JarEntry> theEntries = jf.entries();
				while (theEntries.hasMoreElements()) {
					JarEntry eachEntry = theEntries.nextElement();
					if (eachEntry.isDirectory()) {
						continue;
					}

					String	eachName = eachEntry.getName();

					// Fairly lousy attempt to deal with shaded JARs. Essentially it boils down to using a discovered pom.xml as 
					// a divider between the first set of classes (which we *assume* are the 'real' classes) and the next class
					// hierarchy. This may all be ultimately fruitless...

					if (POM_PATTERN.matcher(eachName).matches()) {
						if (gotClassAlready) {
							System.err.println("Already got classes, so skip shaded JARs: " + eachName + " in " + f);
							break;
						}
					}

					if (!eachName.endsWith(".class") || eachName.contains("$") || /* Skip 'com.google.appengine.labs.repackaged.org.json.JSONArray' etc. */ REPACKAGED_PATTERN.matcher(eachName).find()) {
						continue;
					}

					if (eachName.startsWith("scala/")) {  // FIXME Add restriction to DataSource def / handler
						continue;
					}

					classesList.add( new ClassEntry( eachName.substring( 0, eachName.length() - 6) .replace('/', '.')) );
//					InputStream theStream = jf.getInputStream(eachEntry);
//					ClassReader cr = new ClassReader( Files.toByteArray(f) );
//					String[] ifs = cr.getInterfaces();
//					System.out.println( Arrays.toString(ifs) );

					gotClassAlready = true;
				}
			}
			finally {
				jf.close();
			}

			final String mavenRelJarPath = f.getAbsolutePath().startsWith(mvnRoot) ? f.getAbsolutePath().substring( mvnRoot.length() ) : f.getAbsolutePath();  // FIXME Ugh!

			final String output = MAPPER.writeValueAsString( new ClassResponse( mavenRelJarPath, classesList) );

			t.getResponseHeaders().put( "Content-type", Lists.newArrayList("application/json"));
			t.sendResponseHeaders(200, output.length());
			OutputStream os = t.getResponseBody();
			os.write( output.getBytes() );
			os.close();
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

	// From http://stackoverflow.com/a/10034633/954442
	private static class VersionComparator implements Comparator<String> {

//		public boolean equals(String o1, String o2) {
//			return compare(o1, o2) == 0;
//		}

		public int compare(String version1, String version2) {

			VersionTokenizer tokenizer1 = new VersionTokenizer(version1);
			VersionTokenizer tokenizer2 = new VersionTokenizer(version2);

			int number1 = 0, number2 = 0;
			String suffix1 = "", suffix2 = "";

			while (tokenizer1.MoveNext()) {
				if (!tokenizer2.MoveNext()) {
					do {
						number1 = tokenizer1.getNumber();
						suffix1 = tokenizer1.getSuffix();
						if (number1 != 0 || suffix1.length() != 0) {
							// Version one is longer than number two, and non-zero
							return 1;
						}
					}
					while (tokenizer1.MoveNext());

					// Version one is longer than version two, but zero
					return 0;
				}

				number1 = tokenizer1.getNumber();
				suffix1 = tokenizer1.getSuffix();
				number2 = tokenizer2.getNumber();
				suffix2 = tokenizer2.getSuffix();

				if (number1 < number2) {
					// Number one is less than number two
					return -1;
				}
				if (number1 > number2) {
					// Number one is greater than number two
					return 1;
				}

				boolean empty1 = suffix1.length() == 0;
				boolean empty2 = suffix2.length() == 0;

				if (empty1 && empty2) continue; // No suffixes
				if (empty1) return 1; // First suffix is empty (1.2 > 1.2b)
				if (empty2) return -1; // Second suffix is empty (1.2a < 1.2)

				// Lexical comparison of suffixes
				int result = suffix1.compareTo(suffix2);
				if (result != 0) return result;

			}
			if (tokenizer2.MoveNext()) {
				do {
					number2 = tokenizer2.getNumber();
					suffix2 = tokenizer2.getSuffix();
					if (number2 != 0 || suffix2.length() != 0) {
						// Version one is longer than version two, and non-zero
						return -1;
					}
				}
				while (tokenizer2.MoveNext());

				// Version two is longer than version one, but zero
				return 0;
			}
			return 0;
		}
	}

	// From http://stackoverflow.com/a/10034633/954442
	static class VersionTokenizer {
		private final String _versionString;
		private final int _length;

		private int _position;
		private int _number;
		private String _suffix;
		private boolean _hasValue;

		public int getNumber() {
			return _number;
		}

		public String getSuffix() {
			return _suffix;
		}

		public boolean hasValue() {
			return _hasValue;
		}

		public VersionTokenizer(String versionString) {
			if (versionString == null)
				throw new IllegalArgumentException("versionString is null");

			_versionString = versionString;
			_length = versionString.length();
		}

		public boolean MoveNext() {
			_number = 0;
			_suffix = "";
			_hasValue = false;

			// No more characters
			if (_position >= _length)
				return false;

			_hasValue = true;

			while (_position < _length) {
				char c = _versionString.charAt(_position);
				if (c < '0' || c > '9') break;
				_number = _number * 10 + (c - '0');
				_position++;
			}

			int suffixStart = _position;

			while (_position < _length) {
				char c = _versionString.charAt(_position);
				if (c == '.') break;
				_position++;
			}

			_suffix = _versionString.substring(suffixStart, _position);

			if (_position < _length) _position++;

			return true;
		}
	}
}
