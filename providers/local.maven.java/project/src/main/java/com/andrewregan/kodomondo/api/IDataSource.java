/**
 * 
 */
package com.andrewregan.kodomondo.api;

import java.util.Set;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public interface IDataSource {

	IDataSource getParent();

	String getName();
	Set<String> getStopwords();
	Set<String> getKeyTerms();
}
