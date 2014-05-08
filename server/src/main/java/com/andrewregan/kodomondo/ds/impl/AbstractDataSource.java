/**
 * 
 */
package com.andrewregan.kodomondo.ds.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Set;

import com.andrewregan.kodomondo.ds.api.IDataSource;
import com.google.common.collect.Sets;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public /* not actually abstract */ class AbstractDataSource implements IDataSource {

	protected final String name;
	protected IDataSource parent;  // FIXME Should be multiple, i.e. List
	protected Set<String> keyTerms;
	protected Set<String> stopwords;

	public AbstractDataSource( String inName) {
		name = checkNotNull(inName);
	}

	/* (non-Javadoc)
	 * @see com.andrewregan.kodomondo.api.IDataSource#getParent()
	 */
	public IDataSource getParent() {
		return parent;
	}

	/* (non-Javadoc)
	 * @see com.andrewregan.kodomondo.api.IDataSource#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.andrewregan.kodomondo.api.IDataSource#getStopwords()
	 */
	public Set<String> getStopwords() {
		if ( getParent() != null) {  // FIXME - fairly dumb
			Set<String> sws = Sets.newLinkedHashSet( getParent().getStopwords() );
			sws.addAll(stopwords);
			return sws;
		}

		return stopwords;
	}

	/* (non-Javadoc)
	 * @see com.andrewregan.kodomondo.api.IDataSource#getKeyTerms()
	 */
	public Set<String> getKeyTerms() {
		if ( getParent() != null) {  // FIXME - fairly dumb
			Set<String> kts = Sets.newLinkedHashSet( getParent().getKeyTerms() );
			kts.addAll(keyTerms);
			return kts;
		}

		return keyTerms;
	}

	public void setKeyTerms( Collection<String> inKeyTerms) {
		keyTerms = Sets.newLinkedHashSet( checkNotNull(inKeyTerms) );
	}

	/* (non-Javadoc)
	 * @see com.andrewregan.kodomondo.api.IDataSource#setStopwords(java.util.Collection)
	 */
	public void setStopwords( Collection<String> inStopwords) {
		stopwords = Sets.newLinkedHashSet( checkNotNull(inStopwords) );
	}

	/* (non-Javadoc)
	 * @see com.andrewregan.kodomondo.api.IDataSource#setParent(com.andrewregan.kodomondo.api.IDataSource)
	 */
	public void setParent( IDataSource inParent) {
		parent = inParent;
	}
}