/**
 * 
 */
package com.andrewregan.kodomondo.handlers;

import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import com.andrewregan.kodomondo.tasks.IndexEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class SearchHandler implements HttpHandler {

	@Inject Client esClient;
	@Inject ObjectMapper mapper;

	/* (non-Javadoc)
	 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
	 */
	public void handle( HttpExchange t) throws IOException {

		String q = null;

		for ( NameValuePair each : URLEncodedUtils.parse( t.getRequestURI(), "utf-8")) {
			if ( each.getValue() == null) {
				q = each.getName();
				break;
			}
		}

		System.out.println("q = " + q);

		while (esClient.admin().cluster().prepareHealth("datasource.local-maven").get().getStatus() == ClusterHealthStatus.RED) ;

		SearchHit[] sh = esClient.prepareSearch("datasource.local-maven").addHighlightedField("text").setQuery( matchPhraseQuery( "_all", q).cutoffFrequency(0.001f) ).setSize(999).execute().actionGet().getHits().hits();
		System.out.println("DONE " + sh.length);

		for ( SearchHit each : sh) {
			try {
				System.out.println( each.getScore() + " / " + each.getHighlightFields() + " / " + mapper.readValue( each.source(), IndexEntry.class) );
			}
			catch (JsonProcessingException e) {
				e.printStackTrace();  // FIXME
				// Throwables.propagate(e);
			}
			catch (IOException e) {
				e.printStackTrace();  // FIXME
				// Throwables.propagate(e);
			}
		}
	}
}