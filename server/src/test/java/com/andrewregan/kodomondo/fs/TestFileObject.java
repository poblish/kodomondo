/**
 * 
 */
package com.andrewregan.kodomondo.fs;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;

import com.andrewregan.kodomondo.fs.api.IFileObject;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class TestFileObject implements IFileObject {

	private String path;

	public TestFileObject( final String path) {
		this.path = path;
	}

	@Override
	public IFileObject getChild( String path) {
		return new TestFileObject( getAbsolutePath() + "/" + path);
	}

	@Override
	public IFileObject getParent() {
		return null;
	}

	@Override
	public IFileObject[] listFiles() {
		return null;
	}

	@Override
	public IFileObject[] listFiles( FileFilter fileFilter) {
		return null;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean exists() {
		return ( path != null);
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getPath() {
		return null;
	}

	@Override
	public String getAbsolutePath() {
		return path;
	}

	@Override
	public String getPathRelativeToFile( IFileObject other) {
		return null;
	}

	@Override
	public IFileObject getFileRelativeToFile( IFileObject other) {
		return null;
	}

	@Override
	public String toString( Charset cs) throws IOException {
		return null;
	}

	@Override
	public void launch() throws IOException {
	}

	@Override
	public boolean delete() {
		return false;
	}
}