package com.andrewregan.kodomondo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.andrewregan.kodomondo.api.IDataSource;
import com.andrewregan.kodomondo.ds.AbstractDataSource;
import com.andrewregan.kodomondo.es.EsUtils;
import com.andrewregan.kodomondo.handlers.InfoHandler;
import com.andrewregan.kodomondo.handlers.LaunchHandler;
import com.andrewregan.kodomondo.handlers.ListingsHandler;
import com.andrewregan.kodomondo.handlers.SearchHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import dagger.ObjectGraph;

/**
 * Hello world!
 *
 */
public class LocalMavenServer 
{
	private final Server httpServer;
	private final Map<String,IDataSource> dataSources = Maps.newHashMap();

	@Inject ObjectMapper mapper;
	@Inject EsUtils esUtils;
	@Inject Client esClient;

	@Inject IndexerService indexer;

	@Inject InfoHandler infoHandler;
	@Inject LaunchHandler launchHandler;
	@Inject ListingsHandler listingsHandler;
	@Inject SearchHandler searchHandler;

	private final static Logger LOG = LoggerFactory.getLogger( LocalMavenServer.class );


	public static void main(String[] args) throws Exception {
		final LocalMavenServer server = new LocalMavenServer();
		ObjectGraph.create( new DaggerModule() ).inject(server);

		server.esUtils.waitForStatus();  // When it returns, ES will be up-and-running, so start listening...

		server.indexer.startAsync();

		server.addContexts();
		server.start();
	}

	public LocalMavenServer() {
		readConfig();
		httpServer = new Server(2000);
	}

	public void addContexts() {
		final ContextHandlerCollection coll = new ContextHandlerCollection();
		createContext("/", listingsHandler, coll);
		createContext("/launch", launchHandler, coll);
		createContext("/datasource", new DataSourceHandler(), coll);
		createContext("/info", infoHandler, coll);
		createContext("/search", searchHandler, coll);
		httpServer.setHandler(coll);
	}

	private void createContext( String prefix, Handler handler, ContextHandlerCollection coll) {
		final ContextHandler listingsCtxt = new ContextHandler(prefix);
		listingsCtxt.setHandler(handler);
		coll.addHandler(listingsCtxt);
	}

	public void start() throws Exception {
		httpServer.start();
		httpServer.join();
	}

	@SuppressWarnings("unchecked")
	private void readConfig() {
		try {
			for ( Object eachEntry : new Yaml().loadAll( Files.toString( new File( "src/main/resources/conf", "ds.yaml"), Charset.forName("utf-8")))) {
				Map<String,Object> eachDsEntry = (Map<String,Object>) eachEntry;

				final String dsName = Strings.emptyToNull((String) eachDsEntry.get("name"));
				final String className = Strings.emptyToNull((String) eachDsEntry.get("implClass"));

				final IDataSource dsInst = ( className != null) ? (IDataSource) Class.forName(className).getConstructor( String.class ).newInstance(dsName) : new AbstractDataSource(dsName);
				dsInst.setKeyTerms((List<String>) eachDsEntry.get("key-terms") );
				dsInst.setStopwords((List<String>) eachDsEntry.get("stopwords") );

				final String parentName = Strings.emptyToNull((String) eachDsEntry.get("inherit"));  // FIXME Should be *list* for multiple!
				final IDataSource parent = ( parentName != null) ? /* FIXME error handling! */ dataSources.get(parentName) : null;
				dsInst.setParent(parent);

				dataSources.put( dsName, dsInst);
			}
		}
		catch (IOException e) {
			Throwables.propagate(e);
		}
		catch (ReflectiveOperationException e) {
			Throwables.propagate(e);
		}
	}

	private class DataSourceHandler extends AbstractHandler {

		/* (non-Javadoc)
		 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
		 */
		public void handle(final String target, final Request baseRequest, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {
			final String dsName = baseRequest.getRequestURI().substring(12);  // '/datasource/...'
			final IDataSource ds = dataSources.get(dsName);

			if ( ds != null) {
				final String output = mapper.writeValueAsString(ds);

				resp.setContentType("application/json;charset=utf-8");
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.setContentLength( output.length() );
				resp.getWriter().println(output);
			}
			else {
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		}		
	}
}