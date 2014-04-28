/**
 * 
 */
package com.andrewregan.kodomondo.maven.repo;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.handlers.ListingsHandler;
import com.andrewregan.kodomondo.util.VersionComparator;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class LocalMavenRepository {

	private final IFileObject mvnRoot;

	public LocalMavenRepository(IFileObject mvnRoot) {
		this.mvnRoot = checkNotNull(mvnRoot);
	}

	public void visit( ILocalMavenVisitor inVisitor, IFileObject inDir, boolean isArtifactDir) {

		if (!inVisitor.acceptDirectory(inDir)) {
			return;
		}

//		System.out.println("Here: " + inDir + " (" + isArtifactDir + ")");

		Collection<String> versions = Lists.newArrayList();

		boolean foundJavaDoc = false;

		for ( IFileObject each : inDir.listFiles( new ListingsHandler.BadDirFilter() )) {

			if (each.isDirectory()) {
				if (Character.isDigit( each.getName().charAt(0) )) {
					versions.add( each.getName() );
				}
				else {
					visit( inVisitor, inDir.getChild( each.getName() ), false);
				}
			}
			else {
				if (each.getName().endsWith(".pom")) {
					inVisitor.foundPom( inDir, each);
				}

				if (each.getName().endsWith("-javadoc.jar") ) {
					inVisitor.foundJavaDoc( inDir.getPathRelativeToFile(mvnRoot), each);
					foundJavaDoc = true;
					continue;
				}
			}
		}

		/////////////////////////////////////

		if ( isArtifactDir && !foundJavaDoc) {
			inVisitor.missingJavaDoc( inDir.getFileRelativeToFile(mvnRoot) );
		}

		if (!versions.isEmpty()) {
			String highest = Ordering.from( new VersionComparator() ).max(versions);
			visit( inVisitor, inDir.getChild(highest), true);
		}

		inVisitor.doneDirectory(inDir);
	}
}