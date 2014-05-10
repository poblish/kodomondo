/**
 * 
 */
package com.andrewregan.kodomondo.ds.api;

import java.util.Collection;
import java.util.Set;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public interface IKeyTermDataSource extends IDataSource {

	Set<String> getStopwords();
	Set<String> getKeyTerms();

	void setKeyTerms( Collection<String> terms);
	void setStopwords( Collection<String> terms);
}