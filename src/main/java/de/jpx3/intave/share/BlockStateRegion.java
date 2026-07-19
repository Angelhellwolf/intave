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

package de.jpx3.intave.share;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.jpx3.intave.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static de.jpx3.intave.codec.JsonStreamCodecs.*;

/**
 * Immutable block-state snapshot for an inclusive region. Positions absent from
 * {@link #nonAirBlocks()} are air when {@link #complete()} is true.
 */
public final class BlockStateRegion {
  public static final StreamCodec<JsonReader, JsonWriter, BlockStateRegion> JSON_CODEC = object(
    field("minInclusive", BlockPosition.JSON_CODEC, BlockStateRegion::minInclusive, BlockPosition.ORIGIN),
    field("maxInclusive", BlockPosition.JSON_CODEC, BlockStateRegion::maxInclusive, BlockPosition.ORIGIN),
    booleanField("complete", BlockStateRegion::complete),
    field("nonAirBlocks", PositionedBlockState.LIST_JSON_CODEC, BlockStateRegion::nonAirBlocks, Collections.emptyList()),
    BlockStateRegion::new
  );

  private final BlockPosition minInclusive;
  private final BlockPosition maxInclusive;
  private final boolean complete;
  private final List<PositionedBlockState> nonAirBlocks;

  public BlockStateRegion(
    BlockPosition minInclusive,
    BlockPosition maxInclusive,
    boolean complete,
    List<PositionedBlockState> nonAirBlocks
  ) {
    this.minInclusive = minInclusive;
    this.maxInclusive = maxInclusive;
    this.complete = complete;
    this.nonAirBlocks = Collections.unmodifiableList(new ArrayList<>(nonAirBlocks));
  }

  public BlockPosition minInclusive() {
    return minInclusive;
  }

  public BlockPosition maxInclusive() {
    return maxInclusive;
  }

  public boolean complete() {
    return complete;
  }

  public List<PositionedBlockState> nonAirBlocks() {
    return nonAirBlocks;
  }
}
