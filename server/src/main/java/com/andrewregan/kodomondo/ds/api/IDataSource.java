/**
 *
 */
package com.andrewregan.kodomondo.ds.api;


/**
 * TODO
 *
 * @author andrewregan
 *
 */
public interface IDataSource {

	IDataSource getParent();  // FIXME Should be multiple, i.e. List
	void setParent( IDataSource parent);

	String getName();

	void startup();  // FIXME - lifecycle!
	void shutdown();  // FIXME - lifecycle!
}