/*
 * Copyright 2026 Intave
 *
 * This software is licensed under the PolyForm Perimeter License 1.0.0.
 * You may use this software for any purpose, except for providing to
 * others any product that competes with the software.
 *
 * A copy of the license is available at:
 *   https://polyformproject.org/licenses/perimeter/1.0.0/
 */

package de.jpx3.intave.block.store;

import de.jpx3.intave.share.BlockState;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CopyOnWriteArrayLocalBlockStoreTest {

	@Test
	public void testBasicInsertion() {
		CopyOnWriteArrayLocalBlockStore store = CopyOnWriteArrayLocalBlockStore.of();
		assertTrue(store.put(0, 0, 0, BlockState.stone()));

		assertEquals(BlockState.stone(), store.get(0, 0, 0));
		assertNull(store.get(0, 1, 0));
		assertEquals(1, store.size());

		for (int i = 0; i < 1024; i++) {
			if (i <= 63) {
				assertTrue(store.put(i, 0, 0, BlockState.stone()));
			} else {
				assertFalse(store.put(i, 0, 0, BlockState.stone()));
			}
		}
	}

	@Test
	public void testSynchronizedStoreSurvivesConcurrentClearAndPut() throws Exception {
		BlockStore store = CopyOnWriteArrayLocalBlockStore.of().withSynchronization();
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> clear = executor.submit(() -> {
				for (int i = 0; i < 2000; i++) {
					store.clear();
				}
			});
			Future<?> put = executor.submit(() -> {
				for (int i = 0; i < 2000; i++) {
					assertTrue(store.put(i & 31, 64, 0, BlockState.stone()));
				}
			});
			clear.get(10, TimeUnit.SECONDS);
			put.get(10, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}
	}

}
