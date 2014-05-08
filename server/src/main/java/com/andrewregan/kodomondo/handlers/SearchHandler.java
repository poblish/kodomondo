/**
 * 
 */
package com.andrewregan.kodomondo.handlers;

import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewregan.kodomondo.modules.maven.tasks.IndexEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class SearchHandler extends AbstractHandler {

	@Inject Client esClient;
	@Inject ObjectMapper mapper;

	private static final List<Text> NO_HIGHLIGHTED_TEXT = Collections.emptyList();

	// FIXME Really need to move into JavaDocIndexingTask, so rather than index junk then clean it up, we index better (and more) context and speed up searches
	private static final Pattern EXTRA_WS_PATTERN = Pattern.compile("\\p{Z}+");  // Remove all conceivable whitespace runs
	private static final Pattern JUNK_PATTERN = Pattern.compile("(^[\\.:\\|>] |\\.$| [A-Z]$|Method Summary|extends Object|METHOD DETAIL: FIELD \\| CONSTR \\| METHOD)|(Modifier ?)and Type|(Field )?and Description|Methods Method", Pattern.CASE_INSENSITIVE);

	private final static Logger LOG = LoggerFactory.getLogger( SearchHandler.class );

	public void handle(final String target, final Request baseRequest, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {

		baseRequest.setHandled(true);

		String q = req.getParameter("q");
		int maxResults = req.getParameter("size") != null ? Integer.parseInt( req.getParameter("size") ) : 20;

		if ( q == null || q.isEmpty()) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		SearchHit[] sh = esClient.prepareSearch("datasource.local-maven").addHighlightedField("text", 200, 2).setQuery( matchPhraseQuery( "_all", q).cutoffFrequency(0.001f) ).setSize(maxResults).execute().actionGet().getHits().hits();
//		System.out.println("DONE " + sh.length);

		List<SearchResult> entries = Lists.newArrayList();

		for ( SearchHit each : sh) {
			try {
				final IndexEntry theEntry = mapper.readValue( each.source(), IndexEntry.class);
				if (theEntry.getName().isEmpty() && theEntry.getClassName().isEmpty()) {  // Ugh, why would this be?
					continue;
				}

				final Map<String,HighlightField> highlights = each.getHighlightFields();

				List<Text> texts = highlights.containsKey("text") ? Arrays.asList( highlights.get("text").fragments() ) : NO_HIGHLIGHTED_TEXT;

				entries.add( new SearchResult( theEntry, FluentIterable.from(texts).transform( new Function<Text,String>() {
					public String apply( Text input) {
						return cleanUpHighlight(input);
					}
				} ).toArray( String.class ), each.getScore()) );
			}
			catch (JsonProcessingException e) {
				LOG.error( "", e);  // FIXME
				Throwables.propagate(e);
			}
			catch (IOException e) {
				LOG.error( "", e);  // FIXME
				Throwables.propagate(e);
			}
		}

		try {
			byte[] output = mapper.writeValueAsBytes(entries);

			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType("application/json;charset=utf-8");
			resp.setContentLength( output.length );
			resp.getOutputStream().write(output);
		}
		catch (Throwable e) {
			Throwables.propagate(e);
		}
	}

	@VisibleForTesting
	protected String cleanUpHighlight( final Text input) {
		String s = input.string();
		int prevLen;

		do {
			prevLen = s.length();
			s = s.trim();
			s = EXTRA_WS_PATTERN.matcher(s).replaceAll(" ");
			s = JUNK_PATTERN.matcher(s).replaceAll("");
		}
		while (prevLen != s.length());

		return s;
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