/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
@JsonInclude(Include.NON_NULL)
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
		this.text = inText;	// checkNotNull(inText);
	}

	public String getText() {
		return text;
	}

	public String getArtifact() {
		return artifact;
	}

	public String getName() {
		return Strings.nullToEmpty(name);
	}

	public String getClassName() {
		return Strings.nullToEmpty(className);
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
