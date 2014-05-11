/**
 * 
 */
package com.andrewregan.kodomondo.es;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequestBuilder;
import org.elasticsearch.client.Client;

import com.andrewregan.kodomondo.ds.impl.DataSourceRegistry;
import com.google.common.base.Throwables;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
@Singleton
public class EsUtils {

	@Inject Client esClient;
	@Inject DataSourceRegistry dsRegistry;

	public void waitForStatus() {
//		esClient.admin().cluster().prepareHealth("datasource.local-maven").setWaitForNodes(">=1").get();
		esClient.admin().cluster().prepareHealth( dsRegistry.indexNamesUsed() ).setWaitForYellowStatus().get();
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

	public void waitUntilTypesRefreshed( final String inIndex, final String... inTypes) {
		final IndicesStatsRequestBuilder reqBuilder = esClient.admin().indices().prepareStats(inIndex).setRefresh(true).setTypes(inTypes);
		final long currCount = reqBuilder.execute().actionGet().getTotal().getRefresh().getTotal();
		int waitsToGo = 10;

		try {
			do {
				Thread.sleep(250);
				waitsToGo--;
			}
			while ( waitsToGo > 0 && reqBuilder.execute().actionGet().getTotal().getRefresh().getTotal() == currCount);
		}
		catch (InterruptedException e) {
			Throwables.propagate(e);
		}

		if ( waitsToGo <= 0) {
			throw new RuntimeException("Timeout exceeded!");
		}
	}
}