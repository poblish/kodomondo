/**
 * 
 */
package com.andrewregan.kodomondo;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.fs.impl.LocalFileSystem;
import com.andrewregan.kodomondo.tasks.JavaDocIndexerFactory;
import com.andrewregan.kodomondo.tasks.JavaDocIndexingTask;
import com.andrewregan.kodomondo.tasks.PomIndexerFactory;
import com.andrewregan.kodomondo.tasks.PomIndexingTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import dagger.Module;
import dagger.Provides;
import freemarker.template.Configuration;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
@Module(injects={LocalMavenServer.class})
public class DaggerModule {

	@Provides
	@Singleton
	Configuration provideConfiguration() {
		final Configuration theConfig = new Configuration();
		try {
			theConfig.setDirectoryForTemplateLoading( new File("src/main/resources/templates"));
		}
		catch (IOException e) {
			Throwables.propagate(e);
		}
		return theConfig;
	}

	@Provides
	@Singleton
	Client provideEsClient() {
		 final Node node = NodeBuilder.nodeBuilder().settings( ImmutableSettings.builder()
						 		.put( "index.number_of_shards", 1)
						 	.build() ).node();
		 return node.client();
	}

	@Provides
	@Singleton
	IFileSystem provideFileSystemManager() {
		return new LocalFileSystem();
	}

	@Provides
	@Singleton
	ObjectMapper provideObjectMapper() {
		final ObjectMapper inst = new ObjectMapper();
		return inst;
	}

	@Provides
	@Singleton
	JavaDocIndexerFactory provideJavaDocIndexerFactory( @Named("mvnRoot") final Provider<String> mvnRoot, final Provider<ObjectMapper> mapper, final Provider<Client> esClient) {
		return new JavaDocIndexerFactory() {
			@Override public JavaDocIndexingTask create( File artifact, File docJar) {
				return new JavaDocIndexingTask( artifact, docJar, mvnRoot.get(), esClient.get(), mapper.get());
			}
		};
	}

	@Provides
	@Singleton
	PomIndexerFactory providePomIndexerFactory( @Named("mvnRoot") final Provider<String> mvnRoot, final Provider<ObjectMapper> mapper, final Provider<Client> esClient) {
		return new PomIndexerFactory() {
			@Override public PomIndexingTask create( File artifact, File docJar) {
				return new PomIndexingTask( artifact, docJar, mvnRoot.get(), esClient.get(), mapper.get());
			}
		};
	}

	@Provides
	@Named("mvnRoot")
	String provideMavenRoot() {
		String origMvnRoot = System.getenv("M2_HOME");
		return (( origMvnRoot != null && !origMvnRoot.isEmpty()) ? origMvnRoot : "~/.m2").replace("~",System.getProperty("user.home")) + "/repository/";
	}
}
