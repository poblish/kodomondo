/**
 * 
 */
package com.andrewregan.kodomondo.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.List;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.google.common.collect.Lists;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class DirectoryContentsRestoration {

	private final IFileObject dir;
	private final IFileObject[] originalChildren;

	private List<IFileObject> newFiles;

	public DirectoryContentsRestoration( final IFileObject inDir) {
		this.dir = checkNotNull(inDir);
		this.originalChildren = inDir.listFiles();
	}

	public boolean contentsChanged() {
		return assessState().isEmpty();
	}

	public void restore() {
		for ( IFileObject eachNewFile : assessState()) {
			eachNewFile.delete();
		}
	}

	private List<IFileObject> assessState() {
		newFiles = Lists.newArrayList( dir.listFiles() );
		newFiles.removeAll( Arrays.asList(originalChildren) );
		return newFiles;
	}
}
