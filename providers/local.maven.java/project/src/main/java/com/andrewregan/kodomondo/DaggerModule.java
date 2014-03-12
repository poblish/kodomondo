/**
 * 
 */
package com.andrewregan.kodomondo;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

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
	ObjectMapper provideObjectMapper() {
		final ObjectMapper inst = new ObjectMapper();
		return inst;
	}

	@Provides
	@Named("mvnRoot")
	String provideMavenRoot() {
		String origMvnRoot = System.getenv("M2_HOME");
		return (( origMvnRoot != null && !origMvnRoot.isEmpty()) ? origMvnRoot : "~/.m2").replace("~",System.getProperty("user.home")) + "/repository/";
	}
}
