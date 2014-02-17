package com.andrewregan.kodomondo;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
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
				Collection<String> versions = Lists.newArrayList();

				File[] files = f.listFiles( new FileFilter() {

					public boolean accept( File pathname) {
						return !pathname.getName().startsWith(".");
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
						if (!each.getName().endsWith(".jar")) {
							continue;
						}
						System.out.println( each.getName() );
					}
				}

				final String output;

				if (!versions.isEmpty()) {
					String highest = Ordering.from( new VersionComparator() ).max(versions);
					output = MAPPER.writeValueAsString( new VersionResponse( f.getAbsolutePath(), highest) );
				}
				else {
					output = MAPPER.writeValueAsString( new DirResponse( f.getAbsolutePath(), dirsList) );
				}

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

	private static class VersionResponse {
		final String name;
		final String version;

		public VersionResponse( String inName, String inVersion) {
			this.name = inName;
			this.version = inVersion;
		}

		@JsonProperty("version")
		public String geVersion() {
			return version;
		}
	}

	private static class DirResponse {
		final String name;
		final List<DirEntry> dirs;

		public DirResponse( String inName, List<DirEntry> inClasses) {
			this.name = inName;
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

	// From http://stackoverflow.com/a/10034633/954442
	private static class VersionComparator implements Comparator<String> {

		public boolean equals(String o1, String o2) {
			return compare(o1, o2) == 0;
		}

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
