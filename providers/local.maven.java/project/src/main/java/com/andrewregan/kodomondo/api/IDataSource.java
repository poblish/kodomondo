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

	String getName();
	Set<String> getStopwords();
}
