package com.andrewregan.kodomondo;

import javax.inject.Inject;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.andrewregan.kodomondo.es.EsUtils;
import com.google.common.base.Throwables;

import dagger.Module;
import dagger.ObjectGraph;

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
	public void testIndex() {
		esUtils.waitForStatus();
		indexer.startAsync();

		while (indexer.isRunning()) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				Throwables.propagate(e);
			}
		}

		indexer.stopAsync();
	}

	@Module( includes=DaggerModule.class, overrides=true, injects=IndexerServiceTest.class)
	static class TestModule {}
}
