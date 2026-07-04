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

package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.access.player.trust.TrustFactor;
import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.block.cache.MockFullBlockStaticPlane;
import de.jpx3.intave.block.shape.resolve.DrillResolver;
import de.jpx3.intave.block.shape.resolve.MockShapeResolverPipeline;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.check.movement.physics.search.SimulationSearch;
import de.jpx3.intave.check.movement.physics.search.ThreeTickSimulationSearch;
import de.jpx3.intave.player.collider.complex.SimulationResult;
import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.share.Rotation;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class NTickSimulationSearchTest {
	private static final UUID EMPTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	@BeforeEach
	void setUp() {
		MinecraftVersion.setCurrent(MinecraftVersions.VER1_17_0);
		DrillResolver.manualInit(MockShapeResolverPipeline.createStoneDefault());
	}

	@Test
	void threeTickSearchReconstructsMovementAcrossTwoFilteredFlyingPacketsWithRotationChange() {
		Simulator simulator = new SlowHorizontalTestSimulator();
		reconstructsMovementAcrossFilteredFlyingPackets(
			new ThreeTickSimulationSearch(false, false),
			simulator,
			exampleSlowHorizontalFlyingPacketScenario(simulator, 500),
			2,
			"2f"
		);
	}

	@Test
	void threeTickSearchKeepsLastReportedPositionAcrossNestedFlyingPackets() {
		Simulator simulator = new ReversingVerticalTestSimulator();
		reconstructsMovementAcrossFilteredFlyingPackets(
			new ThreeTickSimulationSearch(false, false),
			simulator,
			exampleReversingVerticalFlyingPacketScenario(simulator),
			2,
			"2f",
			new Motion(0.0, 0.02, 0.0)
		);
	}

	private void reconstructsMovementAcrossFilteredFlyingPackets(
		SimulationSearch search,
		Simulator simulator,
		List<Position> realPositions,
		int maxDroppedFlyingPacketsInRow,
		String requiredDetail
	) {
		reconstructsMovementAcrossFilteredFlyingPackets(
			search,
			simulator,
			realPositions,
			maxDroppedFlyingPacketsInRow,
			requiredDetail,
			Motion.newEmpty()
		);
	}

	private void reconstructsMovementAcrossFilteredFlyingPackets(
		SimulationSearch search,
		Simulator simulator,
		List<Position> realPositions,
		int maxDroppedFlyingPacketsInRow,
		String requiredDetail,
		Motion initialBaseMotion
	) {
		List<Position> positions = dropFlyingPackets(realPositions, 0.03, maxDroppedFlyingPacketsInRow);

		assertFalse(positions.size() == realPositions.size(), "Expected some flying packets to be dropped");

		User user = createUser(Position.immutableEmpty(), Rotation.zero());
		MovementMetadata movement = user.meta().movement();
		movement.setBaseMotion(initialBaseMotion);
		boolean foundRequiredDetail = requiredDetail == null;

		for (int i = 0; i < positions.size(); i++) {
			Position position = positions.get(i);
			movement.updateMovement(
				position, null
			);

			Motion motion = movement.mutableBaseMotionCopy();
			Motion preTickMotion = simulator.simulatePreTick(user, motion, movement);
			movement.setBaseMotion(preTickMotion);

			Simulation simulate = search.greedyNarrowSearch(
				user, movement, simulator
			);
			if (!foundRequiredDetail && simulate.details().contains(requiredDetail)) {
				foundRequiredDetail = true;
			}

			double motionDifference = simulate.offsetDifference();
			if (motionDifference > 0.00001) {
				fail("Packet " + i +
					": difference=" + motionDifference +
					", expected=" + movement.sentOffsetMotion() +
					", simulated=" + simulate.offsetMotion() +
					", configuration=" + simulate.configuration() +
					", details=" + simulate.details());
			}

			movement.assumeOccurred(simulate);

			Motion afterMotion = simulator.simulateAfterTick(
				user, movement,
				movement.position(),
				simulate.offsetMotion()
			);

			movement.setBaseMotion(afterMotion);
			movement.tickComplete(true, false, true);
			movement.lastKeyStrafe = movement.keyStrafe;
			movement.lastKeyForward = movement.keyForward;
			movement.lastOnGround = movement.onGround;
			movement.setVerifiedLastPosition(movement.position(), "two-tick search test accepted");
		}
		assertTrue(foundRequiredDetail, "Expected at least one simulation to use " + requiredDetail);
	}

	private List<Position> dropFlyingPackets(
		List<Position> positions, double limit, int maxDroppedInRow
	) {
		Position lastReported = Position.immutableEmpty();
		List<Position> result = new ArrayList<>();

		int droppedInRow = 0;
		for (Position position : positions) {
			if (flyingPacket(lastReported, position, limit) && droppedInRow < maxDroppedInRow) {
				droppedInRow++;
				continue;
			}
			lastReported = position;
			result.add(position);
			droppedInRow = 0;
		}
		return result;
	}

	private boolean flyingPacket(Position lastReported, Position currentReported, double limit) {
		double distance = lastReported.distance(currentReported);
		return distance < limit;
	}

	private List<Position> exampleFlyingPacketScenario(Simulator simulator, int ticks) {
		User user = createUser(Position.immutableEmpty(), Rotation.zero());
		MovementConfiguration configW = MovementConfiguration.blank().pressingW();
		MovementConfiguration configS = MovementConfiguration.blank().pressingS();
		List<Position> position = new ArrayList<>();
		for (int i = 0; i < ticks; i++) {
			simulator.simulateBetween(
				user, user.meta().movement(),
				(i % 2 == 0 ? configW : configS)
			);
			position.add(
				user.meta().movement().position()
			);
		}
		return position;
	}

	private List<Position> exampleSlowHorizontalFlyingPacketScenario(Simulator simulator, int ticks) {
		User user = createUser(Position.immutableEmpty(), Rotation.zero());
		MovementConfiguration config = MovementConfiguration.blank().pressingW();
		List<Position> position = new ArrayList<>();
		for (int i = 0; i < ticks; i++) {
			simulator.simulateBetween(user, user.meta().movement(), config);
			position.add(user.meta().movement().position());
		}
		return position;
	}

	private List<Position> exampleReversingVerticalFlyingPacketScenario(Simulator simulator) {
		User user = createUser(Position.immutableEmpty(), Rotation.zero());
		user.meta().movement().setBaseMotion(new Motion(0.0, 0.02, 0.0));
		List<Position> position = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			simulator.simulateBetween(user, user.meta().movement(), MovementConfiguration.blank());
			position.add(user.meta().movement().position());
		}
		return position;
	}

	private User createUser(Position position, Rotation rotation) {
		World world = FakeWorldFactory.createWorld(
			(methodName, _) -> switch (methodName) {
				case "isChunkLoaded", "isChunkInUse" -> true;
				case "isThundering", "hasStorm" -> false;
				default -> null;
			}
		);
		AtomicReference<Location> currentLocation = new AtomicReference<>(locationOf(world, position, rotation));
		Player player = FakePlayerFactory.createPlayer(
			(methodName, _) -> switch (methodName) {
				case "getWorld" -> world;
				case "getLocation" -> currentLocation.get().clone();
				case "getUniqueId" -> EMPTY_ID;
				case "isOnGround" -> true;
				default -> null;
			}
		);
		MockFullBlockStaticPlane blockCache = MockFullBlockStaticPlane.createWithHorizontalPlaneAt(0);
		User user = UserFactory.createTestUserFor(player, (usr, key) -> switch (key) {
			case "blockCache" -> blockCache;
			case "trustFactor" -> TrustFactor.RED;
			case "justJoined" -> false;
			case "joined" -> 0L;
			case "latency", "latencyJitter" -> 0;
			case "shouldIgnoreNextInboundPacket", "shouldIgnoreNextOutboundPacket" -> false;
			case "protocolVersion" -> ProtocolMetadata.VER_1_17;
			default -> null;
		});
		UserRepository.manuallyRegisterUser(player, user);

		MovementMetadata movement = user.meta().movement();
		movement.updateMovement(position, rotation);
		movement.setVerifiedLastPosition(position, "two-tick search test seed");
		movement.setLastPosition(position);
		movement.setBaseMotion(Motion.newEmpty());
		movement.setBoundingBox(BoundingBox.fromPosition(user, movement, position));
		movement.onGround = true;
		movement.lastOnGround = true;
		return user;
	}

	private static Location locationOf(World world, Position position, Rotation rotation) {
		return position.toLocation(world, rotation);
	}

	private static final class LinearTestSimulator extends Simulator {
		@Override
		public Motion simulatePreTick(
			User user,
			Motion baseMotion,
			SimulationEnvironment environment
		) {
			baseMotion = baseMotion.copy();
			if (environment.verifiedLastPositionY() <= 0.1) {
				baseMotion.motionY += 0.1;
			}
			return baseMotion;
		}

		@Override
		public Simulation simulateTick(
			User user,
			Motion motion,
			SimulationEnvironment environment,
			MovementConfiguration configuration
		) {
			motion = motion.copy();
			float yawCosine = environment.yawCosine();
			float yawSine = environment.yawSine();
			float moveForward = configuration.forward();
			float moveStrafe = configuration.strafe();
			float f = moveStrafe * moveStrafe + moveForward * moveForward;
			if (f >= 0.0001f) {
				f = (float) Math.sqrt(f);
				f = 0.02f / Math.max(1.0f, f);
				moveStrafe *= f;
				moveForward *= f;
				motion.motionX += moveStrafe * yawCosine - moveForward * yawSine;
				motion.motionZ += moveForward * yawCosine + moveStrafe * yawSine;
			}
			return Simulation.of(user, configuration, environment, SimulationResult.untouched(motion));
		}

		@Override
		public Motion simulateAfterTick(
			User user,
			SimulationEnvironment environment,
			Position position,
			Motion motion
		) {
			motion = motion.copy();
			motion.motionY -= 0.01;
			motion.motionX *= 0.91;
			motion.motionY *= 0.98;
			motion.motionZ *= 0.91;
			return motion;
		}

		@Override
		public void setback(
			User user,
			SimulationEnvironment environment,
			double predictedX,
			double predictedY,
			double predictedZ
		) {
		}
	}

	private static final class SlowHorizontalTestSimulator extends Simulator {
		@Override
		public Motion simulatePreTick(
			User user,
			Motion baseMotion,
			SimulationEnvironment environment
		) {
			return baseMotion.copy();
		}

		@Override
		public Simulation simulateTick(
			User user,
			Motion motion,
			SimulationEnvironment environment,
			MovementConfiguration configuration
		) {
			motion = motion.copy();
			float yawCosine = environment.yawCosine();
			float yawSine = environment.yawSine();
			float moveForward = configuration.forward();
			float moveStrafe = configuration.strafe();
			float f = moveStrafe * moveStrafe + moveForward * moveForward;
			if (f >= 0.0001f) {
				f = (float) Math.sqrt(f);
				f = 0.005f / Math.max(1.0f, f);
				moveStrafe *= f;
				moveForward *= f;
				motion.motionX += moveStrafe * yawCosine - moveForward * yawSine;
				motion.motionZ += moveForward * yawCosine + moveStrafe * yawSine;
			}
			return Simulation.of(user, configuration, environment, SimulationResult.untouched(motion));
		}

		@Override
		public Motion simulateAfterTick(
			User user,
			SimulationEnvironment environment,
			Position position,
			Motion motion
		) {
			motion = motion.copy();
			motion.motionX *= 0.91;
			motion.motionZ *= 0.91;
			return motion;
		}

		@Override
		public void setback(
			User user,
			SimulationEnvironment environment,
			double predictedX,
			double predictedY,
			double predictedZ
		) {
		}
	}

	private static final class ReversingVerticalTestSimulator extends Simulator {
		@Override
		public Motion simulatePreTick(
			User user,
			Motion baseMotion,
			SimulationEnvironment environment
		) {
			return baseMotion.copy();
		}

		@Override
		public Simulation simulateTick(
			User user,
			Motion motion,
			SimulationEnvironment environment,
			MovementConfiguration configuration
		) {
			return Simulation.of(user, configuration, environment, SimulationResult.untouched(motion.copy()));
		}

		@Override
		public Motion simulateAfterTick(
			User user,
			SimulationEnvironment environment,
			Position position,
			Motion motion
		) {
			if (motion.motionY() > 0.0) {
				return new Motion(0.0, -0.04, 0.0);
			}
			if (motion.motionY() < -0.03) {
				return new Motion(0.0, -0.02, 0.0);
			}
			return Motion.newEmpty();
		}

		@Override
		public void setback(
			User user,
			SimulationEnvironment environment,
			double predictedX,
			double predictedY,
			double predictedZ
		) {
		}
	}
}
