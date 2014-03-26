package com.andrewregan.kodomondo.maven;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;

import org.testng.annotations.Test;

import com.andrewregan.kodomondo.maven.util.ArtifactDesc;

/**
 * 
 * TODO
 *
 * @author andrewregan
 *
 */
public class ArtifactDescTest {

	@Test
	public void testFilenameParsing() {
		assertThat( ArtifactDesc.forFile( new File("/com/google/guava/guava/16.0") ).toString(), is("ArtifactDesc{groupId=com.google.guava, artifactId=guava, version=16.0}"));
		assertThat( ArtifactDesc.forFile( new File("com/google/guava/guava/16.0") ).toString(), is("ArtifactDesc{groupId=com.google.guava, artifactId=guava, version=16.0}"));
		assertThat( ArtifactDesc.forFile( new File("com/google/guava/guava/16.0/guava-16.0.jar") ).toString(), is("ArtifactDesc{groupId=com.google.guava, artifactId=guava, version=16.0}"));
	}

	@Test
	public void testToPath() {
		ArtifactDesc a1 = ArtifactDesc.forFile( new File("com/google/guava/guava/16.0") );
		assertThat( a1.toPath(), is("com/google/guava/guava/16.0"));

		ArtifactDesc a2 = ArtifactDesc.forFile( new File("com/google/guava/guava/16.0/guava-16.0.jar") );
		assertThat( a2.toPath(), is("com/google/guava/guava/16.0"));
	}
}
