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

}