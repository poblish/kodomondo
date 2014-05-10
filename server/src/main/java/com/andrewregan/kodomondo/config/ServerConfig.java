/**
 * 
 */
package com.andrewregan.kodomondo.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.andrewregan.kodomondo.KodomondoServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

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
	Client provideEsClient() {
		final Optional<String> dataDir = Optional.fromNullable( Strings.emptyToNull( System.getenv("KODOMONDO_DATA_DIR") ) );

		final Node node = NodeBuilder.nodeBuilder().settings( ImmutableSettings.builder()
						.put( "path.data", dataDir.or("/usr/local/var/kodomondo/"))  // Will create 'elasticsearch/nodes/...'
						.put( "index.number_of_shards", 1)
						.build() ).node();
		final Client c = node.client();

		// FIXME: Try to factor this out into LocalMavenConfig somehow
		try {
			c.admin().indices().prepareCreate("datasource.local-maven").setSettings("{}").execute().actionGet();
		}
		catch (IndexAlreadyExistsException e) {
			// Ignore
		}

		return c;
	}

	@Provides
	@Singleton
	ObjectMapper provideObjectMapper() {
		final ObjectMapper inst = new ObjectMapper();
		return inst;
	}
}