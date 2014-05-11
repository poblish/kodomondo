/**
 * 
 */
package com.andrewregan.kodomondo.ds.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.andrewregan.kodomondo.ds.api.DataSourceMeta;
import com.andrewregan.kodomondo.ds.api.IDataSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class DataSourceRegistry {

	private Map<String,IDataSource> registry = Maps.newHashMap();

	public Collection<IDataSource> dataSources() {
		return registry.values();
	}

	@JsonProperty("datasources")
	public Map<String,IDataSource> registry() {
		return registry;
	}

	public IDataSource get( String dsName) {
		return registry.get( checkNotNull(dsName) );
	}

	public void put( String dsName, IDataSource dsInst) {
		registry.put( checkNotNull(dsName), dsInst);
	}

	public String[] indexNamesUsed() {
		final Set<String> idxNames = Sets.newHashSet();

		// Can't use Batching for some reason
		for ( IDataSource eachDS : dataSources()) {
			final DataSourceMeta metadata = eachDS.getClass().getAnnotation(DataSourceMeta.class);
			if ( metadata != null) {
				idxNames.addAll( Arrays.asList( metadata.indexName() ) );
			}
		}

		return idxNames.toArray( new String[ idxNames.size() ] );
	}
}
