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

package de.jpx3.intave.benchmark;

import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.block.cache.MockFullBlockStaticPlane;
import de.jpx3.intave.block.fluid.FluidFlow;
import de.jpx3.intave.block.fluid.Fluids;
import de.jpx3.intave.block.shape.resolve.DrillResolver;
import de.jpx3.intave.block.shape.resolve.MockShapeResolverPipeline;
import de.jpx3.intave.check.movement.physics.config.MovementConfiguration;
import de.jpx3.intave.check.movement.physics.simulator.Simulator;
import de.jpx3.intave.check.movement.physics.simulator.Simulators;
import de.jpx3.intave.player.collider.Colliders;
import de.jpx3.intave.player.collider.complex.Collider;
import de.jpx3.intave.player.collider.simple.SimpleCollider;
import de.jpx3.intave.test.FakePlayerFactory;
import de.jpx3.intave.test.FakeWorldFactory;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserFactory;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.MovementMetadata;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public final class SimulatorBenchmark {
	private static final int DEFAULT_TICKS = 4_000_000;
	private static final int DEFAULT_WARMUP_TICKS = 10_000;
	private static volatile double sink;

	private SimulatorBenchmark() {
	}

	public static void main(String[] args) {
		BenchmarkOptions options = BenchmarkOptions.from(args);
		setupMinecraftVersion();

		BenchmarkState warmupState = BenchmarkState.create();
		runTicks(warmupState, options.warmupTicks);

		BenchmarkState measuredState = BenchmarkState.create();
		long start = System.nanoTime();
		double checksum = runTicks(measuredState, options.ticks);
		long elapsedNanos = System.nanoTime() - start;
		sink = checksum;

		double nanosPerTick = elapsedNanos / (double) options.ticks;
		double ticksPerSecond = 1_000_000_000.0 / nanosPerTick;
		System.out.println("Simulator benchmark");
		System.out.println("  ticks: " + options.ticks);
		System.out.println("  warmup ticks: " + options.warmupTicks);
		System.out.println("  elapsed: " + formatMillis(elapsedNanos) + " ms");
		System.out.println("  ns/tick: " + format(nanosPerTick));
		System.out.println("  ticks/s: " + format(ticksPerSecond));
		System.out.println("  checksum: " + format(sink));

		System.out.println("We can process " + format(ticksPerSecond / (20 * 100)) + " sim/s per player for 100 players on this netty thread w/o lag");
	}

	private static void setupMinecraftVersion() {
		MinecraftVersion.setCurrent(MinecraftVersions.VER1_21_4);
		com.comphenix.protocol.utility.MinecraftVersion.setCurrentVersion(
			com.comphenix.protocol.utility.MinecraftVersion.v1_21_4
		);
		DrillResolver.manualInit(MockShapeResolverPipeline.createStoneDefault());
	}

	private static double runTicks(BenchmarkState state, int ticks) {
		double checksum = 0.0;
		MovementConfiguration[] configurations = state.configurations;
		for (int tick = 0; tick < ticks; tick++) {
			state.simulator.simulateBetween(
				state.user,
				state.metadata,
				configurations[tick & (configurations.length - 1)]
			);
			checksum += state.metadata.verifiedLastPositionX();
			checksum += state.metadata.verifiedLastPositionY();
			checksum += state.metadata.verifiedLastPositionZ();
		}
		return checksum;
	}

	private static String formatMillis(long nanos) {
		return format(nanos / 1_000_000.0);
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.3f", value);
	}

	private record BenchmarkState(
		User user, MovementMetadata metadata, Simulator simulator,
		MovementConfiguration[] configurations
	) {
		private static BenchmarkState create() {
			User user = createUser();
			MovementMetadata metadata = user.meta().movement();
			metadata.sneaking = true;
			Simulator simulator = Simulators.PLAYER;
			return new BenchmarkState(
				user,
				metadata,
				simulator,
				new MovementConfiguration[]{
					MovementConfiguration.blank().pressingW(),
					MovementConfiguration.blank().pressingA(),
					MovementConfiguration.blank().pressingS(),
					MovementConfiguration.blank().pressingD()
				}
			);
		}

		private static User createUser() {
			Collider collider = Colliders.anyCollider();
			FluidFlow waterflow = Fluids.anyWaterflow();
			SimpleCollider simpleCollider = Colliders.anySimpleCollider();
			World world = FakeWorldFactory.createWorld(
				(methodName, _) -> switch (methodName) {
					case "isChunkLoaded", "isChunkInUse" -> true;
					case "isThundering", "hasStorm" -> false;
					default -> null;
				}
			);
			Location location = new Location(world, 0, 50, 0);
			Player player = FakePlayerFactory.createPlayer(
				(methodName, _) -> switch (methodName) {
					case "getWorld" -> world;
					case "getLocation" -> location;
					case "getUniqueId" -> UUID.fromString("00000000-0000-0000-0000-000000000000");
					default -> null;
				}
			);
			MockFullBlockStaticPlane plane = MockFullBlockStaticPlane.createWithHorizontalPlaneAt(0);
			User user = UserFactory.createTestUserFor(player, (usr, key) -> switch (key) {
				case "collider" -> collider;
				case "waterflow" -> waterflow;
				case "simplifiedCollider" -> simpleCollider;
				case "blockCache" -> plane;
				case "protocolVersion" -> ProtocolMetadata.VER_1_21_3;
				default -> null;
			});
			UserRepository.manuallyRegisterUser(player, user);
			return user;
		}
	}

	private static final class BenchmarkOptions {
		private final int ticks;
		private final int warmupTicks;

		private BenchmarkOptions(int ticks, int warmupTicks) {
			this.ticks = ticks;
			this.warmupTicks = warmupTicks;
		}

		private static BenchmarkOptions from(String[] args) {
			int ticks = args.length >= 1 ? parsePositiveInt(args[0], "ticks") : DEFAULT_TICKS;
			int warmupTicks = args.length >= 2 ? parsePositiveInt(args[1], "warmup ticks") : DEFAULT_WARMUP_TICKS;
			return new BenchmarkOptions(ticks, warmupTicks);
		}

		private static int parsePositiveInt(String text, String name) {
			try {
				int value = Integer.parseInt(text);
				if (value <= 0) {
					throw new IllegalArgumentException(name + " must be greater than zero");
				}
				return value;
			} catch (NumberFormatException exception) {
				throw new IllegalArgumentException(name + " must be an integer: " + text, exception);
			}
		}
	}
}
