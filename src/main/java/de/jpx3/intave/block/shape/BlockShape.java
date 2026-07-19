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

package de.jpx3.intave.block.shape;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.jpx3.intave.annotate.Nullable;
import de.jpx3.intave.codec.ByteBufStreamCodecs;
import de.jpx3.intave.codec.JsonStreamCodecs;
import de.jpx3.intave.codec.StreamCodec;
import de.jpx3.intave.share.BlockPosition;
import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.share.Direction;
import de.jpx3.intave.share.Position;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.doubles.DoubleSet;

import java.util.List;

public interface BlockShape {
  StreamCodec<ByteBuf, ByteBuf, BlockShape> STREAM_CODEC =
    StreamCodec.dispatchBuilder(BlockShape.class, ByteBufStreamCodecs.UNSIGNED_BYTE)
      .subtype(0, EmptyBlockShape.class, () -> EmptyBlockShape.STREAM_CODEC)
      .subtype(1, CubeShape.class, () -> CubeShape.STREAM_CODEC)
      .subtype(2, BoundingBox.class, () -> BoundingBox.STREAM_CODEC)
      .subtype(3, VoxelShape.class, () -> VoxelShape.STREAM_CODEC)
      .subtype(4, ArrayBlockShape.class, () -> ArrayBlockShape.STREAM_CODEC)
      .subtype(5, MergeBlockShape.class, () -> MergeBlockShape.STREAM_CODEC)
      .subtype(6, ComparisonAlertShape.class, () -> ComparisonAlertShape.STREAM_CODEC)
      .build();
  StreamCodec<JsonReader, JsonWriter, BlockShape> JSON_CODEC = JsonStreamCodecs.lazy(() ->
    JsonStreamCodecs.listCodecOf(BoundingBox.JSON_CODEC).beforeAndAfter(
      BlockShapes::fromElementaryBoxes,
      BlockShape::elementaryBoxes
    )
  );

  double allowedOffset(Direction.Axis axis, BoundingBox entity, double offset);
  double min(Direction.Axis axis);
  double max(Direction.Axis axis);

  boolean intersectsWith(BoundingBox boundingBox);
  BlockShape contextualized(int posX, int posY, int posZ);

  default BlockShape contextualized(BlockPosition position) {
    return contextualized(position.getBlockX(), position.getBlockY(), position.getBlockZ());
  }

  BlockShape normalized(int posX, int posY, int posZ);

  default BlockShape normalized(BlockPosition position) {
    return normalized(position.getBlockX(), position.getBlockY(), position.getBlockZ());
  }

  void appendUnsortedCoordsTo(Direction.Axis axis, DoubleSet appendTo);

  @Nullable
  BlockRaytrace raytrace(Position origin, Position target);
  BoundingBox outline();

  List<BoundingBox> elementaryBoxes();
  boolean isEmpty();
  boolean isCubic();

	boolean strictlyInside(double positionX, double positionY, double positionZ);
}
