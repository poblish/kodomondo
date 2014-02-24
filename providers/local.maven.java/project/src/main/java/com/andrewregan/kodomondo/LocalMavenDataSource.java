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

	public Set<String> getStopwords() {
		return Sets.newHashSet("Java", "About");
	}
}
