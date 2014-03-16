/**
 * 
 */
package com.andrewregan.kodomondo.fs.impl;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.io.Files;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class LocalFile implements IFileObject {

	private final File file;

	public LocalFile( final File file) {
		this.file = file;
	}

	@Override
	public IFileObject getChild( String path) {
		return new LocalFile( new File( file, path) );
	}

	@Override
	public IFileObject[] listFiles( FileFilter filter) {
		// FIXME Hideous
        return FluentIterable.from( Arrays.asList(file.listFiles(filter)) ).transform( new Function<File,IFileObject>() {

			@Override
			public IFileObject apply( File input) {
				return new LocalFile(input);
			}}
        ).toArray( IFileObject.class );
	}

	@Override
	public boolean isDirectory() {
		return file.isDirectory();
	}

	@Override
	public boolean exists() {
		return file.exists();
	}

	@Override
	public String getName() {
		return file.getName();
	}

	@Override
	public String getPath() {
		return file.getPath();
	}

	@Override
	public String getAbsolutePath() {
		return file.getAbsolutePath();
	}

	@Override
	public String toString( Charset cs) throws IOException {
		return Files.toString( file, Charset.forName("utf-8"));
	}

	@Override
	public String getPathRelativeToFile( IFileObject other) {
		final String ourAP = file.getAbsolutePath();
		final String parentAP = other.getAbsolutePath();
		return ourAP.startsWith(parentAP) ? ourAP.substring( parentAP.length() ) : ourAP;	// FIXME Pretty crude
	}

	public File getFile() {
		return file;
	}

	@Override
	public void launch() throws IOException {
		Desktop.getDesktop().open(file);  // Launch JAR in whatever viewer/editor
	}
}