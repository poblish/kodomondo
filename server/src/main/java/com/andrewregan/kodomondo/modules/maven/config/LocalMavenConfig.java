/**
 * 
 */
package com.andrewregan.kodomondo.modules.maven.config;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;

import com.andrewregan.kodomondo.config.ServerConfig;
import com.andrewregan.kodomondo.fs.api.IFileObject;
import com.andrewregan.kodomondo.fs.api.IFileSystem;
import com.andrewregan.kodomondo.fs.impl.LocalFileSystem;
import com.andrewregan.kodomondo.modules.maven.ds.LocalMavenDataSource;
import com.andrewregan.kodomondo.modules.maven.tasks.JavaDocDownloadTask;
import com.andrewregan.kodomondo.modules.maven.tasks.JavaDocDownloaderFactory;
import com.andrewregan.kodomondo.modules.maven.tasks.JavaDocIndexerFactory;
import com.andrewregan.kodomondo.modules.maven.tasks.JavaDocIndexingTask;
import com.andrewregan.kodomondo.modules.maven.tasks.PomIndexerFactory;
import com.andrewregan.kodomondo.modules.maven.tasks.PomIndexingTask;
import com.andrewregan.kodomondo.modules.maven.tasks.SourceDownloadTask;
import com.andrewregan.kodomondo.modules.maven.tasks.SourceDownloaderFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import dagger.Module;
import dagger.Provides;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
@Module(includes=ServerConfig.class, injects={LocalMavenDataSource.class})
public class LocalMavenConfig {

	@Provides
	@Singleton
	JavaDocIndexerFactory provideJavaDocIndexerFactory( @Named("mvnRoot") final Provider<IFileObject> mvnRoot, final Provider<ObjectMapper> mapper, final Provider<Client> esClient, final Provider<IFileSystem> fs) {
		return new JavaDocIndexerFactory() {
			@Override public JavaDocIndexingTask create( String artifact, IFileObject docJar) {
				return new JavaDocIndexingTask( artifact, docJar, mvnRoot.get(), esClient.get(), mapper.get(), fs.get());
			}
		};
	}

	@Provides
	@Singleton
	JavaDocDownloaderFactory provideJavaDocDownloaderFactory( @Named("mvnRoot") final Provider<IFileObject> mvnRoot, final Provider<IFileSystem> fs, final Provider<JavaDocIndexerFactory> indexer) {
		return new JavaDocDownloaderFactory() {
			@Override public JavaDocDownloadTask create( IFileObject artifact) {
				return new JavaDocDownloadTask( artifact, fs.get(), mvnRoot.get(), indexer.get());
			}
		};
	}

	@Provides
	@Singleton
	PomIndexerFactory providePomIndexerFactory( @Named("mvnRoot") final Provider<IFileObject> mvnRoot, final Provider<ObjectMapper> mapper, final Provider<Client> esClient) {
		return new PomIndexerFactory() {
			@Override public PomIndexingTask create( IFileObject artifact, IFileObject docJar) {
				return new PomIndexingTask( artifact, docJar, mvnRoot.get(), esClient.get(), mapper.get());
			}
		};
	}

	@Provides
	@Singleton
	SourceDownloaderFactory provideSourceDownloaderFactory( @Named("mvnRoot") final Provider<IFileObject> mvnRoot, final Provider<IFileSystem> fs) {
		return new SourceDownloaderFactory() {
			@Override public SourceDownloadTask create( IFileObject artifact) {
				return new SourceDownloadTask( artifact, fs.get(), mvnRoot.get());
			}
		};
	}

	@Provides
	@Named("mvnRoot")
	IFileObject provideMavenRoot(IFileSystem inFS) {
		String origMvnRoot = System.getenv("M2_HOME");
		return inFS.resolveFile((( origMvnRoot != null && !origMvnRoot.isEmpty()) ? origMvnRoot : "~/.m2").replace("~",System.getProperty("user.home")) + "/repository/");
	}

	@Provides
	@Singleton
	IFileSystem provideFileSystemManager() {
		return new LocalFileSystem();
	}
}