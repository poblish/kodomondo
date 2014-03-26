package com.andrewregan.kodomondo;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import dagger.ObjectGraph;

/**
 * Hello world!
 *
 */
public class LocalMavenServer 
{
	private final HttpServer server;
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
		try {
			readConfig();
			server = HttpServer.create(new InetSocketAddress(2000), 0);
		}
		catch (IOException e) {
			LOG.error( "", e);  // FIXME
			throw Throwables.propagate(e);
		}
	}

	public void addContexts() {
		server.createContext("/", listingsHandler);
		server.createContext("/launch", launchHandler);
		server.createContext("/datasource", new DataSourceHandler());
		server.createContext("/info", infoHandler);
		server.createContext("/search", searchHandler);
		server.setExecutor(null); // creates a default executor
	}

	public void start() {
		server.start();
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

	private class DataSourceHandler implements HttpHandler {

		/* (non-Javadoc)
		 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
		 */
		public void handle( HttpExchange t) throws IOException {
			final String dsName = t.getRequestURI().getPath().substring(12);  // '/datasource/...'
			final IDataSource ds = dataSources.get(dsName);

			if ( ds != null) {
				final byte[] bs = mapper.writeValueAsBytes(ds);
				t.getResponseHeaders().put( "Content-type", Lists.newArrayList("application/json"));
				t.sendResponseHeaders(200, bs.length);
				OutputStream os = t.getResponseBody();
				os.write(bs);
				os.close();
			}
			else {
				t.sendResponseHeaders( 404, 0);
				t.getResponseBody().close();
			}
		}		
	}
}