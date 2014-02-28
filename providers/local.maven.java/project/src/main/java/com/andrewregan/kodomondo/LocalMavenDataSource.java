/**
 * 
 */
package com.andrewregan.kodomondo;

import java.util.Set;

import com.andrewregan.kodomondo.api.IDataSource;
import com.google.common.collect.Sets;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class LocalMavenDataSource implements IDataSource {

	public String getName() {
		return "local-maven";
	}

	public IDataSource getParent() {
		return null;
	}

	// case-sensitive - FIXME Just not sustainable
	public Set<String> getStopwords() {
		return Sets.newHashSet("LICENSE", "Java", "About", "After", "Default", "Library", "Maven", "Oracle","Project", "Sun", "Since", "Start", "String", "Search", "Check", "deprecated", "download", "Download", "package", "param", "remove", "rename", "Remove", "Rename", "return", "switch", "goto", "continue", "break", "update", "Update", "language");
	}

	// Case-insensitive regex
	public Set<String> getKeyTerms() {
		return Sets.newHashSet("Java", "Commits?", "Merge requests?");
	}
}
