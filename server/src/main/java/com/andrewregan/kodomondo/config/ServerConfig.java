/**
 * 
 */
package com.andrewregan.kodomondo.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.yaml.snakeyaml.Yaml;

import com.andrewregan.kodomondo.KodomondoServer;
import com.andrewregan.kodomondo.ds.api.IDataSource;
import com.andrewregan.kodomondo.ds.api.IKeyTermDataSource;
import com.andrewregan.kodomondo.ds.impl.DataSourceRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.Files;

import dagger.Module;
import dagger.Provides;
import freemarker.template.Configuration;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
@Module(library=true, injects={KodomondoServer.class})
public class ServerConfig {

	@Provides
	ExecutorService provideTaskExecutorService() {
		return Executors.newFixedThreadPool(5);
	}

	@Provides
	@Singleton
	Configuration provideConfiguration() {
		final Configuration theConfig = new Configuration();
		theConfig.setClassForTemplateLoading(this.getClass(), "/templates");
		return theConfig;
	}

	@Provides
	@Singleton
	Client provideEsClient( DataSourceRegistry registry) {
		final Optional<String> dataDir = Optional.fromNullable( Strings.emptyToNull( System.getenv("KODOMONDO_DATA_DIR") ) );

		final Node node = NodeBuilder.nodeBuilder().settings( ImmutableSettings.builder()
						.put( "path.data", dataDir.or("/usr/local/var/kodomondo/"))  // Will create 'elasticsearch/nodes/...'
						.put( "index.number_of_shards", 1)
						.build() ).node();
		final Client c = node.client();

		try {
			// Can't use Batching for some reason
			for ( String eachIdx : registry.indexNamesUsed()) {
				c.admin().indices().prepareCreate(eachIdx).setSettings("{}").execute().actionGet();
			}
		}
		catch (IndexAlreadyExistsException e) {
			// Ignore
		}

		return c;
	}

	@Provides
	@Singleton
	@SuppressWarnings("unchecked")
	DataSourceRegistry provideDataSources( @Named("configFilePath") final String configFilePath) {
		final DataSourceRegistry dataSources = new DataSourceRegistry();

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

				dataSources.put( dsName, dsInst);
			}
		}
		catch (IOException e) {
			Throwables.propagate(e);
		}
		catch (ReflectiveOperationException e) {
			Throwables.propagate(e);
		}

		return dataSources;
	}

	@Named("configFilePath")
	@Provides
	String provdeConfigPath() {
		for ( int i = 0; i < KodomondoServer.APP_ARGS.length; i++) {
			if (KodomondoServer.APP_ARGS[i].equals("--config")) {
				return KodomondoServer.APP_ARGS[i+1];
			}
 		}
		return null;
	}

	@Provides
	@Singleton
	ObjectMapper provideObjectMapper() {
		final ObjectMapper inst = new ObjectMapper();
		return inst;
	}
}