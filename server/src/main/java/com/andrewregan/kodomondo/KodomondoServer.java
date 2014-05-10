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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.andrewregan.kodomondo.config.ServerConfig;
import com.andrewregan.kodomondo.ds.api.DataSourceMeta;
import com.andrewregan.kodomondo.ds.api.IDataSource;
import com.andrewregan.kodomondo.ds.api.IKeyTermDataSource;
import com.andrewregan.kodomondo.es.EsUtils;
import com.andrewregan.kodomondo.jetty.WebContexts;
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
public class KodomondoServer 
{
	@Inject WebContexts webContexts;
	private final Server httpServer;

	@Inject ObjectMapper mapper;
	@Inject EsUtils esUtils;
	@Inject Client esClient;

	private final Map<String,IDataSource> dataSources = Maps.newHashMap();

	private final static ObjectGraph SERVER_GRAPH = ObjectGraph.create( new ServerConfig() );

	private final static Logger LOG = LoggerFactory.getLogger( KodomondoServer.class );


	public static void main(String[] args) throws Exception {

		String configFilePath = null;
		for ( int i = 0; i < args.length; i++) {
			if (args[i].equals("--config")) {
				configFilePath = args[i+1];
				break;
			}
 		}

		final KodomondoServer server = new KodomondoServer(configFilePath);
		SERVER_GRAPH.inject(server);

		server.esUtils.waitForStatus();  // When it returns, ES will be up-and-running, so start listening...

		server.addContexts();

		server.httpServer.setHandler(server.webContexts.getHandler());

		server.start();
	}

	public KodomondoServer( String configFilePath) {
		readConfig(configFilePath);
		httpServer = new Server(2000);
	}

	public void addContexts() {
		webContexts.addContext("/datasource", new DataSourceHandler());
	}

	public void start() throws Exception {
		httpServer.start();
		httpServer.join();
	}

	@SuppressWarnings("unchecked")
	private void readConfig( String configFilePath) {
		try {
			final String pathToUse = Strings.emptyToNull(configFilePath) != null ? configFilePath : "src/main/resources/conf/ds.yaml";

			for ( Object eachEntry : new Yaml().loadAll( Files.toString( new File(pathToUse), Charset.forName("utf-8")))) {
				Map<String,Object> eachDsEntry = (Map<String,Object>) eachEntry;

				final String dsName = Strings.emptyToNull((String) eachDsEntry.get("name"));
				final String className = Strings.emptyToNull((String) eachDsEntry.get("implClass"));

				final Class<?> dsClazz = Class.forName(className);
				final IDataSource dsInst = (IDataSource) dsClazz.getConstructor( String.class ).newInstance(dsName);

				if (dsInst instanceof IKeyTermDataSource) {  // FIXME
					((IKeyTermDataSource) dsInst).setKeyTerms((List<String>) eachDsEntry.get("key-terms") );
					((IKeyTermDataSource) dsInst).setStopwords((List<String>) eachDsEntry.get("stopwords") );
				}

				final String parentName = Strings.emptyToNull((String) eachDsEntry.get("inherit"));  // FIXME Should be *list* for multiple!
				final IDataSource parent = ( parentName != null) ? /* FIXME error handling! */ dataSources.get(parentName) : null;
				dsInst.setParent(parent);

				final DataSourceMeta metadata = dsClazz.getAnnotation(DataSourceMeta.class);
				if ( metadata != null) {
					LOG.info("Configuring " + dsInst + "...");
					SERVER_GRAPH.plus( metadata.daggerConfig().newInstance() ).inject(dsInst);
				}
				else {
					LOG.warn("No DataSourceMeta for " + dsInst);
				}

				dsInst.startup();

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