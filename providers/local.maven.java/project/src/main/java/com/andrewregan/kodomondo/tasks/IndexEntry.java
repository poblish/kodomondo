/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class IndexEntry {
	protected final String artifact;
	protected final String className;
	protected final String name;
	protected final String text;

	@JsonCreator
	public IndexEntry( @JsonProperty("text") String inText, @JsonProperty("artifact") final String artifact, @JsonProperty("className") final String className, @JsonProperty("name") final String inName) {
		this.artifact = checkNotNull(artifact);
		this.className = className; // checkNotNull(className);
		this.name = inName; // checkNotNull(className);
		this.text = checkNotNull(inText);
	}

	public String getText() {
		return text;
	}

	public String getArtifact() {
		return artifact;
	}

	public String getName() {
		return name;
	}

	public String getClassName() {
		return className;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).omitNullValues()
						.add( "artifact", artifact)
						.add( "class", className)
						.add( "name", name)
						.add( "text", text.length() + " chars")
						.toString();
	}
}
