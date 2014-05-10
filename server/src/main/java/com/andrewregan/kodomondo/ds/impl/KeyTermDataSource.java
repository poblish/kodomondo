/**
 * 
 */
package com.andrewregan.kodomondo.ds.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Set;

import com.andrewregan.kodomondo.ds.api.IKeyTermDataSource;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;


/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class KeyTermDataSource extends AbstractDataSource implements IKeyTermDataSource {

	protected Set<String> keyTerms;
	protected Set<String> stopwords;

	public KeyTermDataSource( String inName) {
		super(inName);
	}

	public Set<String> getStopwords() {
		if ( getParent() != null) {  // FIXME - fairly dumb
			Set<String> sws = Sets.newLinkedHashSet( ((IKeyTermDataSource) getParent()).getStopwords() );
			sws.addAll(stopwords);
			return sws;
		}

		return stopwords;
	}

	public Set<String> getKeyTerms() {
		if ( getParent() != null) {  // FIXME - fairly dumb
			Set<String> kts = Sets.newLinkedHashSet( ((IKeyTermDataSource) getParent()).getKeyTerms() );
			kts.addAll(keyTerms);
			return kts;
		}

		return keyTerms;
	}

	public void setKeyTerms( Collection<String> inKeyTerms) {
		keyTerms = Sets.newLinkedHashSet( checkNotNull(inKeyTerms) );
	}

	public void setStopwords( Collection<String> inStopwords) {
		stopwords = Sets.newLinkedHashSet( checkNotNull(inStopwords) );
	}

	@Override
	public void startup() {
		// NOOP
	}

	@Override
	public void shutdown() {
		// NOOP
	}	

	@Override
	public int hashCode() {
		return Objects.hashCode( super.hashCode(), keyTerms, stopwords);
	}

	@Override
	public boolean equals( Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof KeyTermDataSource)) {
			return false;
		}
		final KeyTermDataSource other = (KeyTermDataSource) obj;
		return Objects.equal( keyTerms, other.keyTerms) && Objects.equal( stopwords, other.stopwords);
	}
}