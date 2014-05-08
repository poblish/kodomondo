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
public interface IDataSource {

	IDataSource getParent();  // FIXME Should be multiple, i.e. List

	String getName();
	Set<String> getStopwords();
	Set<String> getKeyTerms();

	void setKeyTerms( Collection<String> terms);
	void setStopwords( Collection<String> terms);
	void setParent( IDataSource parent);
}
