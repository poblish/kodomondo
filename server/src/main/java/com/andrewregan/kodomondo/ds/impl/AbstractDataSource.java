/**
 * 
 */
package com.andrewregan.kodomondo.ds.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.andrewregan.kodomondo.ds.api.IDataSource;
import com.google.common.base.Objects;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public abstract class AbstractDataSource implements IDataSource {

	protected final String name;
	protected IDataSource parent;  // FIXME Should be multiple, i.e. List

	public AbstractDataSource( String inName) {
		name = checkNotNull(inName);
	}

	public IDataSource getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public void setParent( IDataSource inParent) {
		parent = inParent;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( name, parent);
	}
}