/**
 * 
 */
package com.andrewregan.kodomondo.modules.maven.ds;

import javax.inject.Inject;

import com.andrewregan.kodomondo.ds.api.DataSourceMeta;
import com.andrewregan.kodomondo.ds.impl.KeyTermDataSource;
import com.andrewregan.kodomondo.handlers.SearchHandler;
import com.andrewregan.kodomondo.jetty.WebContexts;
import com.andrewregan.kodomondo.modules.maven.IndexerService;
import com.andrewregan.kodomondo.modules.maven.config.LocalMavenConfig;
import com.andrewregan.kodomondo.modules.maven.handlers.InfoHandler;
import com.andrewregan.kodomondo.modules.maven.handlers.LaunchHandler;
import com.andrewregan.kodomondo.modules.maven.handlers.ListingsHandler;


/**
 * TODO
 *
 * @author andrewregan
 *
 */
@DataSourceMeta(indexName="datasource.local-maven", daggerConfig=LocalMavenConfig.class)
public class LocalMavenDataSource extends KeyTermDataSource {

	@Inject WebContexts webContexts;

	@Inject IndexerService indexer;

	@Inject InfoHandler infoHandler;
	@Inject LaunchHandler launchHandler;
	@Inject ListingsHandler listingsHandler;
	@Inject SearchHandler searchHandler;


	public LocalMavenDataSource( String inName) {
		super(inName);
	}

	@Override
	public void startup() {
		indexer.startAsync();

		webContexts.addContext("/", listingsHandler);
		webContexts.addContext("/launch", launchHandler);
		webContexts.addContext("/info", infoHandler);
		webContexts.addContext("/search", searchHandler);
	}

	@Override
	public void shutdown() {
		// FIXME - remove contexts, stop indexer!
	}
}