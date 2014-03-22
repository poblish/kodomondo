/**
 * 
 */
package com.andrewregan.kodomondo.fs.api;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public interface IFileObject {
	IFileObject getChild( String path);
	IFileObject[] listFiles( FileFilter fileFilter);
	boolean isDirectory();
	boolean exists();

	String getName();
	String getPath();
	String getAbsolutePath();
	String getPathRelativeToFile( IFileObject other);
	IFileObject getFileRelativeToFile( IFileObject other);

	String toString( Charset cs) throws IOException;

	void launch() throws IOException;
}
