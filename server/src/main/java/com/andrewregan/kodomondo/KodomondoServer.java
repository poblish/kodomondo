package com.andrewregan.kodomondo;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewregan.kodomondo.config.ServerConfig;
import com.andrewregan.kodomondo.ds.api.DataSourceMeta;
import com.andrewregan.kodomondo.ds.api.IDataSource;
import com.andrewregan.kodomondo.ds.impl.DataSourceRegistry;
import com.andrewregan.kodomondo.es.EsUtils;
import com.andrewregan.kodomondo.handlers.SearchHandler;
import com.andrewregan.kodomondo.jetty.WebContexts;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import dagger.ObjectGraph;

/**
 * Hello world!
 *
 */
public class KodomondoServer 
{
	private final Server httpServer;  // Not injected for some reason
	@Inject WebContexts webContexts;

	@Inject ObjectMapper mapper;
	@Inject EsUtils esUtils;
	@Inject Client esClient;

	@Inject SearchHandler searchHandler;

	@Inject DataSourceRegistry dsRegistry;

	private static ObjectGraph SERVER_GRAPH;
	public static String[] APP_ARGS = new String[0];

	private final static Logger LOG = LoggerFactory.getLogger( KodomondoServer.class );


	public static void main(String[] args) throws Exception {
		APP_ARGS = args;

		SERVER_GRAPH = ObjectGraph.create( new ServerConfig() );
		SERVER_GRAPH.inject( new KodomondoServer() ).start();
	}

	public KodomondoServer() {
		httpServer = new Server(2000);
	}

	public void start() throws Exception {
		startDataSources();

		esUtils.waitForStatus();  // When it returns, ES will be up-and-running, so start listening...

		webContexts.addContext("/datasource", new DataSourceHandler());
		webContexts.addContext("/search", searchHandler);

		httpServer.setHandler(webContexts.getHandler());
		httpServer.start();
		httpServer.join();
	}

	private void startDataSources() {
		try {
			for ( IDataSource eachDS : dsRegistry.dataSources()) {
				final DataSourceMeta metadata = eachDS.getClass().getAnnotation(DataSourceMeta.class);
				if ( metadata != null) {
					LOG.info("Configuring " + eachDS + "...");
					SERVER_GRAPH.plus( metadata.daggerConfig().newInstance() ).inject(eachDS);
				}
				else {
					LOG.warn("No DataSourceMeta for " + eachDS);
				}

				eachDS.startup();
			}
		}
		catch (ReflectiveOperationException e) {
			Throwables.propagate(e);
		}
	}

	private class DataSourceHandler extends AbstractHandler {

		public void handle(final String target, final Request baseRequest, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {
			final String dsName = baseRequest.getRequestURI().substring(12);  // '/datasource/...'

			if (dsName.isEmpty()) {
				final byte[] output = mapper.writeValueAsBytes(dsRegistry);
				
				resp.setContentType("application/json;charset=utf-8");
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.setContentLength( output.length );
				resp.getOutputStream().write(output);
			}
			else {
				final IDataSource ds = dsRegistry.get(dsName);
	
				if ( ds != null) {
					final byte[] output = mapper.writeValueAsBytes(ds);
	
					resp.setContentType("application/json;charset=utf-8");
					resp.setStatus(HttpServletResponse.SC_OK);
					resp.setContentLength( output.length );
					resp.getOutputStream().write(output);
				}
				else {
					resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				}
			}
		}		
	}
}