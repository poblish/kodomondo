/**
 * 
 */
package com.andrewregan.kodomondo.maven;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class ArtifactDesc {

	private final String groupId;
	private final String artifactId;
	private final String version;

	public static ArtifactDesc forFile( File inArtifactFile) {
		String artifactStr = inArtifactFile.getPath();

		while (artifactStr.startsWith("/")) {
			artifactStr = artifactStr.substring(1);
		}

		int last = artifactStr.lastIndexOf('/');
		int nextPos = artifactStr.lastIndexOf('/', last - 1);

		return new ArtifactDesc( artifactStr.substring( 0, nextPos).replace( '/', '.'), artifactStr.substring( nextPos + 1, last), artifactStr.substring( last + 1));
	}

	public ArtifactDesc( final String group, String artifact, String inVersion) {
		groupId = checkNotNull(group);
		artifactId = checkNotNull(artifact);
		version = checkNotNull(inVersion);
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}
}
