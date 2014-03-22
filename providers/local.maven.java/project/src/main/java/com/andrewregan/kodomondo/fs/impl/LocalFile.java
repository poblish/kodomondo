/**
 * 
 */
package com.andrewregan.kodomondo.fs.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
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

	private final IFileSystem fs;
	private final File file;

	public LocalFile( final IFileSystem fs, final File file) {
		this.fs = checkNotNull(fs);
		this.file = file;
	}

	@Override
	public IFileObject getChild( String path) {
		return fs.resolveFile( this, path);
	}

	@Override
	public IFileObject[] listFiles( FileFilter filter) {
		// FIXME Hideous
        return FluentIterable.from( Arrays.asList(file.listFiles(filter)) ).transform( new Function<File,IFileObject>() {

			@Override
			public IFileObject apply( File input) {
				return new LocalFile( fs, input);
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

		String endBit = ourAP.startsWith(parentAP) ? ourAP.substring( parentAP.length() ) : ourAP;	// FIXME Pretty crude
		while (endBit.startsWith("/")) {
			endBit = endBit.substring(1);
		}
		return endBit;
	}

	@Override
	public IFileObject getFileRelativeToFile( IFileObject other) {
		return fs.resolveFile( getPathRelativeToFile(other) );
	}

	public File getFile() {
		return file;
	}

	@Override
	public void launch() throws IOException {
		Desktop.getDesktop().open(file);  // Launch JAR in whatever viewer/editor
	}

	@Override
	public String toString() {
		return file.toString();
	}
}