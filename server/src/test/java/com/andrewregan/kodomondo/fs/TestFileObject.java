/**
 * 
 */
package com.andrewregan.kodomondo.fs;

import static com.google.common.base.Preconditions.checkState;

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

	protected final IFileSystem fs;
	private String path;
	private boolean isDir = false;
	protected IFileObject[] children = new IFileObject[0];

	public TestFileObject( final IFileSystem fs, final String path) {
		this( fs, path, false, new IFileObject[]{});
	}

	public TestFileObject( final IFileSystem fs, final String path, boolean isDir) {
		this.fs = fs;
		this.path = path;
		this.isDir = isDir;
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
		return fs.resolveFile( this, path.substring( 0, path.lastIndexOf('/')));
	}

	@Override
	public IFileObject[] listFiles() {
		checkState(this.isDir, "Must be a directory: " + this);
		return children;
	}

	@Override
	public IFileObject[] listFiles( FileFilter fileFilter) {
		checkState(this.isDir, "Must be a directory: " + this);
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
		return path;
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

	@Override
	public String toString() {
		return path;
	}
}