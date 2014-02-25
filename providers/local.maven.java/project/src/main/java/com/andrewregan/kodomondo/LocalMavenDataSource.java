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

	// case-sensitive
	public Set<String> getStopwords() {
		return Sets.newHashSet("LICENSE", "Java", "About", "Default", "Library", "Maven", "Oracle","Project", "Sun", "Start", "String", "Search", "Check", "deprecated", "package", "param", "return", "switch", "goto", "continue", "break", "update", "language");
	}
}
