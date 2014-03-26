/**
 * 
 */
package com.andrewregan.kodomondo.es;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
@Singleton
public class EsUtils {

	@Inject Client esClient;

	public void waitForStatus() {
//		esClient.admin().cluster().prepareHealth("datasource.local-maven").setWaitForNodes(">=1").get();
		esClient.admin().cluster().prepareHealth("datasource.local-maven").setWaitForYellowStatus().get();
//		try {
//		do {
//			System.out.println("HERE");
//			try {
//				System.out.println( esClient.admin().cluster().prepareHealth("datasource.local-maven").setWaitForNodes(">=1").setWaitForYellowStatus().get().getStatus() == ClusterHealthStatus.RED );
//				Thread.sleep(200);
//			}
//			catch (InterruptedException e) {
//				e.printStackTrace();
//				Throwables.propagate(e);
//				break;
//			}
//		}
//		while (esClient.admin().cluster().prepareHealth("datasource.local-maven").get().getStatus() == ClusterHealthStatus.RED);
//		}
//		catch (Throwable e) {
//			e.printStackTrace();
//		}
	}
}
