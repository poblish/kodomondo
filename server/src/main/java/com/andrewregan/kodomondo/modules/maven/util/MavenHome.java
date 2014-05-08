/**
 * 
 */
package com.andrewregan.kodomondo.modules.maven.util;

import java.io.File;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public class MavenHome {

	/**
	 * Where does .../bin/mvn live?
	 * @return
	 */
	public static File getMavenHome() {
		final String m2Loc = System.getenv("M2");
		if (m2Loc != null) {
			return new File(m2Loc);
		}

		final File ulm = new File("/usr/local/maven");
		if (ulm.exists()) {
			return ulm;
		}

		return new File("/usr/local");
	}
}
