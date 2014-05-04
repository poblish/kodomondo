/**
 * 
 */
package com.andrewregan.kodomondo.fs;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class TestFileObject implements IFileObject {

	private final IFileSystem fs;
	private String path;
	private boolean isDir = false;
	private IFileObject[] children;

	public TestFileObject( final IFileSystem fs, final String path) {
		this( fs, path, false, new IFileObject[]{});
	}

	public TestFileObject( final IFileSystem fs, final String path, boolean isDir, IFileObject[] children) {
		this.fs = fs;
		this.path = path;
		this.isDir = isDir;
		this.children = children;
	}

	@Override
	public IFileObject getChild( String path) {
		return fs.resolveFile( this, path);
	}

	@Override
	public IFileObject getParent() {
		return null;
	}

	@Override
	public IFileObject[] listFiles() {
		return children;
	}

	@Override
	public IFileObject[] listFiles( FileFilter fileFilter) {
		return children;
	}

	@Override
	public boolean isDirectory() {
		return isDir;
	}

	@Override
	public boolean exists() {
		return ( path != null);
	}

	@Override
	public String getName() {
		return path.substring( path.lastIndexOf("/") + 1);
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