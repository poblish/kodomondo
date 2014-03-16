/**
 * 
 */
package com.andrewregan.kodomondo.handlers;

import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;

import com.andrewregan.kodomondo.tasks.IndexEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
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
		int maxResults = 20;

		for ( NameValuePair each : URLEncodedUtils.parse( t.getRequestURI(), "utf-8")) {
			if ( each.getValue() == null) {
				q = each.getName();
			}
			else if ( each.getName().equals("size")) {
				maxResults = Integer.parseInt( each.getValue() );
			}
		}

		while (esClient.admin().cluster().prepareHealth("datasource.local-maven").get().getStatus() == ClusterHealthStatus.RED) ;

		SearchHit[] sh = esClient.prepareSearch("datasource.local-maven").addHighlightedField("text", 200, 2).setQuery( matchPhraseQuery( "_all", q).cutoffFrequency(0.001f) ).setSize(maxResults).execute().actionGet().getHits().hits();
//		System.out.println("DONE " + sh.length);

		List<SearchResult> entries = Lists.newArrayList();

		for ( SearchHit each : sh) {
			try {
				// System.out.println( each.getScore() + " / " + each.getHighlightFields() + " / " + mapper.readValue( each.source(), IndexEntry.class) );
				Text[] hs = each.getHighlightFields().get("text").fragments();
				entries.add( new SearchResult( mapper.readValue( each.source(), IndexEntry.class), FluentIterable.from( Arrays.asList(hs)).transform( new Function<Text,String>() {
					public String apply( Text input) {
						return input.string().trim().replaceAll("\\s+", " "); 
					}
				} ).toArray( String.class ), each.getScore()) );
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

		try {
			String output = mapper.writeValueAsString(entries);
			byte[] b = output.getBytes();
			t.getResponseHeaders().put( "Content-type", Lists.newArrayList("application/json"));
			t.sendResponseHeaders(200, b.length);
			OutputStream os = t.getResponseBody();
			os.write(b);
			os.close();
		}
		catch (Throwable e) {
			e.printStackTrace();  // FIXME
			// Throwables.propagate(e);
		}
	}

	private static class SearchResult {
		private final IndexEntry entry;
		private final String[] highlights;
		private final float score;

		public SearchResult( IndexEntry entry, String[] highlights, float score) {
			this.entry = entry;
			this.highlights = highlights;
			this.score = score;
		}

		@SuppressWarnings("unused")
		public IndexEntry getEntry() {
			return new IndexEntry( /* FIXME Block out 'text' */ null, entry.getArtifact(), entry.getClassName(), entry.getName());
		}

		@SuppressWarnings("unused")
		public String[] getHighlights() {
			return highlights;
		}

		@SuppressWarnings("unused")
		public float getScore() {
			return score;
		}
	}
}