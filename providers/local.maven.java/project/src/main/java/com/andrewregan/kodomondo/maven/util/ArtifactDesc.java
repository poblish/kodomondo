/**
 * 
 */
package com.andrewregan.kodomondo.maven.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.regex.Pattern;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

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

	private final static Pattern TRAILING_JAR_NAME = Pattern.compile("/[^/]*\\.jar$", Pattern.CASE_INSENSITIVE);

	public static ArtifactDesc forFile( File inArtifactFile) {
		String artifactStr = inArtifactFile.getPath();

		while (artifactStr.startsWith("/")) {
			artifactStr = artifactStr.substring(1);
		}

		artifactStr = TRAILING_JAR_NAME.matcher(artifactStr).replaceFirst("");

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

	public String toPath() {
		StringBuilder sb = new StringBuilder();
		return sb.append(groupId.replace('.', '/')).append("/").append(artifactId).append("/").append(version).toString();
	}

	public String toString() {
		return Objects.toStringHelper(this).omitNullValues()
						.add( "groupId", Strings.emptyToNull(groupId))
						.add( "artifactId", Strings.emptyToNull(artifactId))
						.add( "version", Strings.emptyToNull(version))
						.toString();
	}
}
