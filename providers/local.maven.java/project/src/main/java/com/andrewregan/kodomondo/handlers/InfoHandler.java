/**
 * 
 */
package com.andrewregan.kodomondo.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.objectweb.asm.ClassReader;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class InfoHandler implements HttpHandler {

	@Inject Configuration fmConfig;
	@Inject IFileSystem fs;

	@Named("mvnRoot")
	@Inject IFileObject mvnRoot;

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
			fmConfig.getTemplate("info_tmpl.ftl").process( resultsModel, sw);
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
		try ( JarFile jf = fs.openJar( mvnRoot.getChild(jarName) )) {
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
				String fqnSuper = cr.getSuperName();

				resultsModel.put("interfaces", ifs);
				resultsModel.put("class_pkg", getPackage(fqn));
				resultsModel.put("class_name", getName(fqn));
				resultsModel.put("superClass_pkg", getPackage(fqnSuper));
				resultsModel.put("superClass_name", getName(fqnSuper));

				break;
			}
		}
		catch (IOException e) {
			Throwables.propagate(e);
		}
	}

	private static String getPackage( final String fqn) {
		return fqn.substring( 0, fqn.lastIndexOf('/') ).replace('/', '.');
	}

	private static String getName( final String fqn) {
		return fqn.substring( fqn.lastIndexOf('/') + 1);
	}
}