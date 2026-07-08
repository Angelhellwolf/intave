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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;


// We only allow accesses within 2 chunks
public final class CopyOnWriteArrayLocalBlockStore implements BlockStore {
	// A sector is a 8x8x8=512 block of blockstores.
	// A chunk has 16x384x16=98304 blocks, which is 192 sectors.
	// We store at most 1 chunk in each direction, so we hold 9*192=1728 sectors, which is 1728*512=884736 blocks.
	private final static int X_OFFSET_SECTORS = 16;
	private final static int Z_OFFSET_SECTORS = 16;
	private BlockState[][] sectors = null;

	private int sectorCenterX;
	private int sectorCenterZ;
	private AtomicInteger size = new AtomicInteger(0);

	private CopyOnWriteArrayLocalBlockStore() {
	}

	@Override
	public BlockState get(int x, int y, int z) {
		BlockState[] sector = sectorOf(x, y, z);
		if (sector == null) {
			return null;
		}
		return sector[(y & 7) * 64 + (z & 7) * 8 + (x & 7)];
	}

	@Override
	public boolean put(int x, int y, int z, BlockState state) {
		if (size.get() == 0) {
			sectorCenterX = x >> 3;
			sectorCenterZ = z >> 3;
			sectors = new BlockState[48 * Z_OFFSET_SECTORS * X_OFFSET_SECTORS][];
		}
		BlockState[] sector = getOrSetSectorOf(x, y, z);
		if (sector == null) {
			return false;
		}
		int index = (y & 7) * 64 + (z & 7) * 8 + (x & 7);
		if (sector[index] == null && state != null) {
			size.addAndGet(1);
		}
		if (sector[index] != null && state == null) {
			size.addAndGet(-1);
		}
		sector[index] = state;
		return true;
	}

	@Override
	public int size() {
		return size.get();
	}

	@Override
	public void removeIf(Predicate<BlockState> predicate) {
		if (size.get() == 0) {
			return;
		}
		for (BlockState[] sector : sectors) {
			if (sector == null) {
				continue;
			}
			for (int j = 0; j < sector.length; j++) {
				BlockState state = sector[j];
				if (state != null && predicate.test(state)) {
					sector[j] = null;
					size.addAndGet(-1);
				}
			}
		}
	}

	@Override
	public void clear() {
		sectors = null;
		size = new AtomicInteger(0);
	}

	private BlockState[] sectorOf(int x, int y, int z) {
		if (sectors == null) {
			return null;
		}
		int sectorX = x >> 3;
		int sectorY = y >> 3;
		int sectorZ = z >> 3;
		// 64-65=-1
		int offsetSectorX = sectorX - sectorCenterX + (X_OFFSET_SECTORS / 2);
		int offsetSectorY = sectorY + 8;
		int offsetSectorZ = sectorZ - sectorCenterZ + (Z_OFFSET_SECTORS / 2);
		if (offsetSectorX < 0 || offsetSectorX >= X_OFFSET_SECTORS || offsetSectorZ < 0 || offsetSectorZ >= Z_OFFSET_SECTORS || sectorY < 0 || sectorY >= 48) {
			return null;
		}
		return sectors[offsetSectorY * Z_OFFSET_SECTORS * X_OFFSET_SECTORS + offsetSectorZ * X_OFFSET_SECTORS + offsetSectorX];
	}

	private BlockState[] getOrSetSectorOf(int x, int y, int z) {
		int sectorX = x >> 3;
		int sectorY = y >> 3;
		int sectorZ = z >> 3;
		int offsetSectorX = sectorX - sectorCenterX + (X_OFFSET_SECTORS / 2);
		int offsetSectorY = sectorY + 8;
		int offsetSectorZ = sectorZ - sectorCenterZ + (Z_OFFSET_SECTORS / 2);
		if (offsetSectorX < 0 || offsetSectorX >= X_OFFSET_SECTORS || offsetSectorZ < 0 || offsetSectorZ >= Z_OFFSET_SECTORS || sectorY < 0 || sectorY >= 48) {
			return null;
		}
		int index = offsetSectorY * Z_OFFSET_SECTORS * X_OFFSET_SECTORS + offsetSectorZ * X_OFFSET_SECTORS + offsetSectorX;
		BlockState[] sector = sectors[index];
		if (sector == null) {
			sector = new BlockState[512];
			sectors[index] = sector;
		}
		return sector;
	}

	public static CopyOnWriteArrayLocalBlockStore of() {
		return new CopyOnWriteArrayLocalBlockStore();
	}
}
