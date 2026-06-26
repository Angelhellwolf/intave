package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.access.player.trust.TrustFactor;
import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.block.cache.MockFullBlockStaticPlane;
import de.jpx3.intave.block.shape.resolve.DrillResolver;
import de.jpx3.intave.block.shape.resolve.MockShapeResolverPipeline;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
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
import de.jpx3.intave.world.border.MockWorldBorder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TwoTickSimulationSearchTest {
	private static final UUID EMPTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	private static final double STRICT_DELTA = 1.0E-12;

	private final Simulator simulator = new LinearTestSimulator();
	private final SimulationSearch search = new TwoTickSimulationSearch(false, false);

	@BeforeEach
	void setUp() {
		MinecraftVersion.setCurrent(MinecraftVersions.VER1_17_0);
		DrillResolver.manualInit(MockShapeResolverPipeline.createStoneDefault());
	}

	@Test
	void reconstructsMovementAcrossFilteredFlyingPacketWithRotationChange() {
		Position initialPosition = new Position(0.0, 1.0, 0.0);
		Rotation hiddenRotation = Rotation.zero();
		Rotation visibleRotation = Rotation.of(90.0F, 0.0F);
		MovementConfiguration walkingForward = MovementConfiguration.blank().pressingW();

		User generatorUser = createUser(initialPosition, hiddenRotation);
		MovementMetadata generator = generatorUser.meta().movement();
		GeneratedTick hiddenTick = simulateGeneratedTick(generatorUser, generator, walkingForward);
		assertTrue(
			hiddenTick.simulation.resultsInFlyingPacket(generator, generatorUser.meta().protocol().flyingPacketUncertaintyRadius()),
			"Generated first tick should be filtered as a flying packet"
		);

		applyFilteredTick(generatorUser, generator, hiddenTick);
		generator.updateMovement(null, visibleRotation);
		GeneratedTick visibleTick = simulateGeneratedTick(generatorUser, generator, walkingForward);
		Position sentPosition = hiddenTick.position.add(visibleTick.simulation.motion());
		assertTrue(
			initialPosition.distance(sentPosition) > generatorUser.meta().protocol().flyingPacketUncertaintyRadius(),
			"Generated second tick should force a position packet after filtering"
		);

		User searchUser = createUser(initialPosition, hiddenRotation);
		MovementMetadata movement = searchUser.meta().movement();
		movement.updateMovement(sentPosition, visibleRotation);

		Simulation reconstructed = search.simulate(searchUser, simulator);

		assertTrue(reconstructed.details().contains("2t"), "Search should use the two-tick reconstruction path");
		assertEquals(
			0.0,
			reconstructed.motionDifference(visibleTick.simulation.motion()),
			STRICT_DELTA,
			() -> "Expected " + visibleTick.simulation.motion() + " but reconstructed " + reconstructed.motion()
		);
		assertEquals(0.0, reconstructed.positionDifference(hiddenTick.position, sentPosition), STRICT_DELTA);
		assertEquals(hiddenTick.position, movement.verifiedLastPosition());
	}

	private GeneratedTick simulateGeneratedTick(
		User user,
		MovementMetadata movement,
		MovementConfiguration configuration
	) {
		Simulation simulation = simulator.simulateTick(
			user,
			movement.mutableBaseMotionCopy(),
			movement.unmodifiable(),
			configuration
		);
		Position position = movement.verifiedLastPosition().add(simulation.motion());
		return new GeneratedTick(simulation.reusableCopy(), position);
	}

	private void applyFilteredTick(
		User user,
		MovementMetadata movement,
		GeneratedTick tick
	) {
		movement.assumeOccurred(tick.simulation);
		movement.updateMovement(tick.position, null);
		Motion motion = tick.simulation.motion().copy();
		simulator.simulateAfterTick(user, movement, tick.position, motion);
		movement.setBaseMotion(motion);
		movement.setLastOnGround(movement.onGround);
		movement.setVerifiedLastPosition(tick.position, "generated filtered flying packet");
		movement.tickComplete(false, false);
	}

	private User createUser(Position position, Rotation rotation) {
		WorldBorder worldBorder = MockWorldBorder.create();
		World world = FakeWorldFactory.createWorld(
			(methodName, _) -> switch (methodName) {
				case "isChunkLoaded", "isChunkInUse" -> true;
				case "isThundering", "hasStorm" -> false;
				case "getWorldBorder" -> worldBorder;
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
		Location location = position.toLocation(world);
		location.setYaw(rotation.yaw());
		location.setPitch(rotation.pitch());
		return location;
	}

	private record GeneratedTick(Simulation simulation, Position position) {}

	private static final class LinearTestSimulator extends Simulator {
		private static final double FORWARD_IMPULSE = 0.02;
		private static final double STRAFE_IMPULSE = 0.08;

		@Override
		public void simulatePreTick(
			User user,
			Motion baseMotion,
			SimulationEnvironment environment
		) {
		}

		@Override
		public Simulation simulateTick(
			User user,
			Motion motion,
			SimulationEnvironment environment,
			MovementConfiguration configuration
		) {
			Motion result = motion.copy();
			double yawRadians = Math.toRadians(environment.rotationYaw());
			double forward = configuration.forward() * FORWARD_IMPULSE;
			double strafe = configuration.strafe() * STRAFE_IMPULSE;
			result.motionX += strafe * Math.cos(yawRadians) - forward * Math.sin(yawRadians);
			result.motionZ += forward * Math.cos(yawRadians) + strafe * Math.sin(yawRadians);

			return Simulation.of(user, configuration, SimulationResult.untouched(result));
		}

		@Override
		public void simulateAfterTick(
			User user,
			SimulationEnvironment environment,
			Position position,
			Motion motion
		) {
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
