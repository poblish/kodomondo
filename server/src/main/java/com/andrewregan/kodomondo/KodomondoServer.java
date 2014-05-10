package com.andrewregan.kodomondo;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
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
import com.andrewregan.kodomondo.es.EsUtils;
import com.andrewregan.kodomondo.jetty.WebContexts;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

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

	@Named("dataSources")
	@Inject Map<String,IDataSource> dataSources;

	private static ObjectGraph SERVER_GRAPH;

	private final static Logger LOG = LoggerFactory.getLogger( KodomondoServer.class );


	public static void main(String[] args) throws Exception {
		SERVER_GRAPH = ObjectGraph.create( new AppArgsModule(args) );
		SERVER_GRAPH.inject( new KodomondoServer() ).start();
	}

	public KodomondoServer() {
		httpServer = new Server(2000);
	}

	public void start() throws Exception {
		startDataSources();

		esUtils.waitForStatus();  // When it returns, ES will be up-and-running, so start listening...

		webContexts.addContext("/datasource", new DataSourceHandler());

		httpServer.setHandler(webContexts.getHandler());
		httpServer.start();
		httpServer.join();
	}

	private void startDataSources() {
		try {
			for ( IDataSource eachDS : dataSources.values()) {
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

	@Module(overrides=true, includes=ServerConfig.class)
	static class AppArgsModule {

		private final String[] args;

		public AppArgsModule( String[] inArgs) {
			args = inArgs;
		}

		@Named("configFilePath")
		@Provides
		String provdeConfigPath() {
			for ( int i = 0; i < args.length; i++) {
				if (args[i].equals("--config")) {
					return args[i+1];
				}
	 		}
			return null;
		}
	}

	private class DataSourceHandler extends AbstractHandler {

		public void handle(final String target, final Request baseRequest, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {
			final String dsName = baseRequest.getRequestURI().substring(12);  // '/datasource/...'
			final IDataSource ds = dataSources.get(dsName);

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