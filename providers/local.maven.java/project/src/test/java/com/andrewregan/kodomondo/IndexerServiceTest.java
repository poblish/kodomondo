package com.andrewregan.kodomondo;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.es.EsUtils;
import com.google.common.base.Throwables;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

/**
 * 
 * TODO
 *
 * @author andrewregan
 *
 */
public class IndexerServiceTest {

	@Inject EsUtils esUtils;
	@Inject IndexerService indexer;

	@BeforeClass
    void injectDependencies() {
        ObjectGraph.create( new TestModule() ).inject(this);
    }

	@Test
	public void testIndex() throws InterruptedException {
		// esUtils.waitForStatus();
		indexer.startAsync();

		while (indexer.isRunning()) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				Throwables.propagate(e);
			}
		}

		Thread.sleep(15000);

		indexer.stopAsync();
	}

	@Module( includes=DaggerModule.class, overrides=true, injects=IndexerServiceTest.class)
	static class TestModule {
		
		@Provides
		@Singleton
		Client provideEsClient() {
			GetResponse getResult = mock( GetResponse.class );
			when(getResult.isExists()).thenReturn(false);

			final GetRequestBuilder grb = mock( GetRequestBuilder.class );
			when(grb.get()).thenReturn(getResult);

			final GetRequestBuilder docGrb = mock( GetRequestBuilder.class );
			when(docGrb.get()).thenReturn(getResult);

			final GetRequestBuilder metaGrb = mock( GetRequestBuilder.class );
			when(metaGrb.get()).thenReturn(getResult);

			final IndexRequestBuilder irb = mock( IndexRequestBuilder.class );
			when(irb.setSource( anyString() )).thenReturn(irb);
			when(irb.setSource( any(byte[].class) )).thenReturn(irb);
			when(irb.setTTL( anyLong() )).thenReturn(irb);
			when(irb.get()).thenReturn( mock( IndexResponse.class ) );

			final BulkRequestBuilder brb = mock( BulkRequestBuilder.class );

			//////////////////////////////////////////

			final Client c = mock( Client.class );
			when(c.prepareGet( anyString(), eq("dir-visit"), anyString())).thenReturn(grb);
			when(c.prepareGet( anyString(), eq("javadoc"), anyString())).thenReturn(docGrb);
			when(c.prepareGet( anyString(), eq("metadata"), anyString())).thenReturn(metaGrb);

			when(c.prepareIndex( anyString(), eq("dir-visit"), anyString())).thenReturn(irb);
			when(c.prepareIndex( anyString(), eq("javadoc"), anyString())).thenReturn(irb);
			when(c.prepareIndex( anyString(), eq("metadata"), anyString())).thenReturn(irb);

			when(c.prepareBulk()).thenReturn(brb);
			return c;
		}
	}
}