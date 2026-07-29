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

package de.jpx3.intave.module.test.record;

import de.jpx3.intave.annotate.Nullable;
import de.jpx3.intave.check.movement.physics.environment.Pose;
import de.jpx3.intave.codec.ByteBufStreamCodecs;
import de.jpx3.intave.codec.StreamCodec;
import de.jpx3.intave.share.BlockPosition;
import de.jpx3.intave.share.Input;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.share.Rotation;
import io.netty.buffer.ByteBuf;

import java.util.*;

final class MoveFrame {
	private static final int VERSIONED_LIST_MARKER = Integer.MIN_VALUE;
	private static final int CURRENT_FORMAT_VERSION = 2;
	private static final int MAX_FRAME_COUNT = 1_048_576;
	private static final StreamCodec<ByteBuf, ByteBuf, Position> POSITION_CODEC =
		Position.STREAM_CODEC.nullable(ByteBufStreamCodecs.BOOLEAN);
	private static final StreamCodec<ByteBuf, ByteBuf, Rotation> ROTATION_CODEC =
		Rotation.STREAM_CODEC.nullable(ByteBufStreamCodecs.BOOLEAN);
	private static final StreamCodec<ByteBuf, ByteBuf, Pose> POSE_CODEC =
		ByteBufStreamCodecs.STRING.beforeAndAfter(Pose::valueOf, Pose::name).nullable(ByteBufStreamCodecs.BOOLEAN);
	private static final StreamCodec<ByteBuf, ByteBuf, Map<BlockPosition, MaterialVariantStore>> BLOCKS_CODEC =
		StreamCodec.mapCodec(
			BlockPosition.STREAM_CODEC,
			MaterialVariantStore.STREAM_CODEC,
			ByteBufStreamCodecs.INTEGER
		);

	private final Map<BlockPosition, MaterialVariantStore> dirtyBlocks = new HashMap<>();
	private final Position moveTo;
	private final Rotation rotateTo;
	private final Input input;
	private final boolean gliding;
	private final @Nullable Pose physicalPose;

	private static final StreamCodec<ByteBuf, ByteBuf, MoveFrame> LEGACY_STREAM_CODEC =
		StreamCodec.of(MoveFrame::encodeLegacy, MoveFrame::decodeLegacy);
	static final StreamCodec<ByteBuf, ByteBuf, List<MoveFrame>> LEGACY_LIST_STREAM_CODEC =
		ByteBufStreamCodecs.listCodecOf(LEGACY_STREAM_CODEC);

	static final StreamCodec<ByteBuf, ByteBuf, MoveFrame> VERSION_ONE_STREAM_CODEC =
		StreamCodec.of(MoveFrame::encodeVersionOne, MoveFrame::decodeVersionOne);

	private static final StreamCodec<ByteBuf, ByteBuf, MoveFrame> CURRENT_STREAM_CODEC = StreamCodec.of((buffer, frame) -> {
		VERSION_ONE_STREAM_CODEC.encode(buffer, frame);
		POSE_CODEC.encode(buffer, frame.physicalPose());
	}, buffer -> VERSION_ONE_STREAM_CODEC.decode(buffer).withPhysicalPose(POSE_CODEC.decode(buffer)));

	public static final StreamCodec<ByteBuf, ByteBuf, List<MoveFrame>> LIST_STREAM_CODEC = StreamCodec.of((buffer, frames) -> {
		ByteBufStreamCodecs.INTEGER.encode(buffer, VERSIONED_LIST_MARKER);
		ByteBufStreamCodecs.INTEGER.encode(buffer, CURRENT_FORMAT_VERSION);
		ByteBufStreamCodecs.INTEGER.encode(buffer, frames.size());
		for (MoveFrame frame : frames) {
			CURRENT_STREAM_CODEC.encode(buffer, frame);
		}
	}, buffer -> {
		int markerOrLegacySize = ByteBufStreamCodecs.INTEGER.decode(buffer);
		if (markerOrLegacySize >= 0) {
			return decodeFrames(buffer, markerOrLegacySize, LEGACY_STREAM_CODEC);
		}
		if (markerOrLegacySize != VERSIONED_LIST_MARKER) {
			throw new IllegalStateException("Unknown movement frame list marker: " + markerOrLegacySize);
		}
		int version = ByteBufStreamCodecs.INTEGER.decode(buffer);
		StreamCodec<ByteBuf, ByteBuf, MoveFrame> frameCodec;
		if (version == 1) {
			frameCodec = VERSION_ONE_STREAM_CODEC;
		} else if (version == CURRENT_FORMAT_VERSION) {
			frameCodec = CURRENT_STREAM_CODEC;
		} else {
			throw new IllegalStateException(
				"Unsupported movement frame format version: " + version
			);
		}
		int size = ByteBufStreamCodecs.INTEGER.decode(buffer);
		return decodeFrames(buffer, size, frameCodec);
	});

	public MoveFrame(
		@Nullable Position moveTo,
		@Nullable Rotation rotateTo,
		Map<BlockPosition, MaterialVariantStore> dirtyBlocks,
		Input input,
		boolean gliding,
		@Nullable Pose physicalPose
	) {
		this.moveTo = moveTo;
		this.rotateTo = rotateTo;
		this.dirtyBlocks.putAll(dirtyBlocks);
		this.input = input;
		this.gliding = gliding;
		this.physicalPose = physicalPose;
	}

	public Map<BlockPosition, MaterialVariantStore> blocks() {
		return dirtyBlocks;
	}

	public @Nullable Position moveTo() {
		return moveTo;
	}

	public @Nullable Rotation rotateTo() {
		return rotateTo;
	}

	public Input input() {
		return input;
	}

	public boolean gliding() {
		return gliding;
	}

	public @Nullable Pose physicalPose() {
		return physicalPose;
	}

	private MoveFrame withPhysicalPose(@Nullable Pose physicalPose) {
		return new MoveFrame(moveTo, rotateTo, dirtyBlocks, input, gliding, physicalPose);
	}

	@Override
	public String toString() {
		return "MoveFrame{" +
			"moveTo=" + moveTo +
			", rotateTo=" + rotateTo +
			", dirtyBlocks=" + dirtyBlocks +
			", input=" + input +
			", gliding=" + gliding +
			", physicalPose=" + physicalPose +
			'}';
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		MoveFrame moveFrame = (MoveFrame) obj;
		if (!dirtyBlocks.equals(moveFrame.dirtyBlocks)) return false;
		if (!Objects.equals(moveTo, moveFrame.moveTo)) return false;
		if (!Objects.equals(rotateTo, moveFrame.rotateTo)) return false;
		if (!Objects.equals(input, moveFrame.input)) return false;
		if (gliding != moveFrame.gliding) return false;
		return physicalPose == moveFrame.physicalPose;
	}

	@Override
	public int hashCode() {
		int result = dirtyBlocks.hashCode();
		result = 31 * result + (moveTo != null ? moveTo.hashCode() : 0);
		result = 31 * result + (rotateTo != null ? rotateTo.hashCode() : 0);
		result = 31 * result + input.hashCode();
		result = 31 * result + Boolean.hashCode(gliding);
		result = 31 * result + (physicalPose != null ? physicalPose.hashCode() : 0);
		return result;
	}

	private static void encodeLegacy(ByteBuf buffer, MoveFrame frame) {
		POSITION_CODEC.encode(buffer, frame.moveTo());
		ROTATION_CODEC.encode(buffer, frame.rotateTo());
		BLOCKS_CODEC.encode(buffer, frame.blocks());
		Input.STREAM_CODEC.encode(buffer, frame.input());
	}

	private static MoveFrame decodeLegacy(ByteBuf buffer) {
		return new MoveFrame(
			POSITION_CODEC.decode(buffer),
			ROTATION_CODEC.decode(buffer),
			BLOCKS_CODEC.decode(buffer),
			Input.STREAM_CODEC.decode(buffer),
			false,
			null
		);
	}

	private static void encodeVersionOne(ByteBuf buffer, MoveFrame frame) {
		encodeLegacy(buffer, frame);
		ByteBufStreamCodecs.BOOLEAN.encode(buffer, frame.gliding());
	}

	private static MoveFrame decodeVersionOne(ByteBuf buffer) {
		MoveFrame legacy = decodeLegacy(buffer);
		return new MoveFrame(
			legacy.moveTo,
			legacy.rotateTo,
			legacy.dirtyBlocks,
			legacy.input,
			ByteBufStreamCodecs.BOOLEAN.decode(buffer),
			null
		);
	}

	private static List<MoveFrame> decodeFrames(ByteBuf buffer, int size, StreamCodec<ByteBuf, ByteBuf, MoveFrame> frameCodec) {
		if (size < 0 || size > MAX_FRAME_COUNT) {
			throw new IllegalStateException("Invalid movement frame count: " + size);
		}
		List<MoveFrame> frames = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			frames.add(frameCodec.decode(buffer));
		}
		return frames;
	}
}
