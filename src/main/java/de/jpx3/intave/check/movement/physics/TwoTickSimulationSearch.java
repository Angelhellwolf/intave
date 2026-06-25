package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.check.movement.physics.search.MovementSearchBranchers;
import de.jpx3.intave.check.movement.physics.search.MovementSearchConfig;
import de.jpx3.intave.check.movement.physics.search.MovementSearchInput;
import de.jpx3.intave.diagnostic.KeyPressStudy;
import de.jpx3.intave.diagnostic.timings.Timings;
import de.jpx3.intave.math.Hypot;
import de.jpx3.intave.player.ItemProperties;
import de.jpx3.intave.search.Searcher;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.share.Rotation;
import de.jpx3.intave.user.MessageChannel;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.meta.InventoryMetadata;
import de.jpx3.intave.user.meta.MetadataBundle;
import de.jpx3.intave.user.meta.MovementMetadata;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static de.jpx3.intave.check.movement.physics.MoveMetric.FLYING_PACKET_ACCURATE;
import static de.jpx3.intave.check.movement.physics.MoveMetric.TELEPORT;

public final class TwoTickSimulationSearch implements SimulationSearch {
	private final static Searcher<MovementSearchInput, MovementSearchConfig> SEARCHER = new Searcher<>(
		MovementSearchBranchers.normal(),
		MovementSearchConfig::blank,
		false
	);

	private final boolean itemUsageReset;
	private final boolean detectNoSlowdown;

	public TwoTickSimulationSearch(boolean itemUsageReset, boolean detectNoSlowdown) {
		this.itemUsageReset = itemUsageReset;
		this.detectNoSlowdown = detectNoSlowdown;
	}

	@Override
	public Simulation simulate(User user, Simulator simulator) {
		MovementMetadata movementData = user.meta().movement();
		Position sentPosition = movementData.position();
		Position lastPositionB4Flying = movementData.lastPosition();

		FPCSimulationContainer container = collectSimulations(
			user, simulator, movementData,
			Collector.of(
				FPCSimulationContainer::def,
				(c, o) -> c.add(o, user, movementData),
				(c1, c2) -> c1.mergedWith(c2, user, movementData),
				Function.identity()
			),
			sim -> sim.positionDifference(lastPositionB4Flying, sentPosition) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
		);

		List<Simulation> flyingSimulations = container.possibleFlyingSimulations();
		Simulation bestSimulation = container.bestSimulation();

		if (flyingSimulations.isEmpty() || bestSimulation.positionDifference(lastPositionB4Flying, sentPosition) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT) {
			applySimulation(user, bestSimulation);
			KeyPressStudy.enterKeyPress(movementData.keyForward, movementData.keyStrafe);
			return bestSimulation;
		}

		SimulationEnvironment bestEnvironment = null;
		double bestDistance = 2 * bestSimulation.positionDifference(lastPositionB4Flying, sentPosition);

		for (Simulation flyingSimulation : flyingSimulations) {
			SimulationEnvironment branchEnvironment = movementData.mutableView();
			flyingTickSimulation(
				user, simulator,
				movementData.verifiedLastPosition(),
				movementData.position(), movementData.rotation(),
				branchEnvironment, flyingSimulation
			);
			Motion remainingMotion = branchEnvironment.motion();
			Simulation secondTickSimulation = collectSimulations(
				user, simulator, branchEnvironment,
				Collectors.reducing(
					Simulation.invalid(),
					(o, o2) -> o.select(o2, remainingMotion)
				),
				sim -> sim.motionDifference(remainingMotion) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT &&
					sim.positionDifference(lastPositionB4Flying, sentPosition) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
			);

			secondTickSimulation.append("2t");
			Position lastPosition = branchEnvironment.lastPosition();
			double branchDistance = secondTickSimulation.positionDifference(lastPosition, sentPosition) +
				secondTickSimulation.motionDifference(remainingMotion);
			if (branchDistance < bestDistance) {
				bestSimulation = secondTickSimulation.reusableCopy();
				bestDistance = branchDistance;
				bestEnvironment = branchEnvironment;
			}
		}

		if (bestEnvironment != null) {
			bestEnvironment.commitTo(user.meta().movement());
		}

		applySimulation(user, bestSimulation);
		KeyPressStudy.enterKeyPress(movementData.keyForward, movementData.keyStrafe);
		return bestSimulation;
	}

	private void flyingTickSimulation(
		User user, Simulator simulator,
		Position verifiedLastPosition,
		Position sentPosition, Rotation sentRotation,
		SimulationEnvironment environment,
		Simulation flyingSimulation
	) {
		environment.assumeOccurred(flyingSimulation);
		Motion firstTickMotion = flyingSimulation.motion().copy();
		Position firstTickPosition = environment.verifiedLastPosition().add(firstTickMotion);
		environment.updateMovement(firstTickPosition, null);
		environment.setLastPosition(verifiedLastPosition);
		simulator.simulateAfterTick(user, environment, firstTickPosition, firstTickMotion);
		environment.setBaseMotion(firstTickMotion);
		environment.setLastOnGround(environment.onGround());
		environment.activeTick(FLYING_PACKET_ACCURATE);
		environment.setVerifiedLastPosition(firstTickPosition, "Two-tick flying simulation");
		environment.tickComplete(false, false);
		environment.updateMovement(sentPosition, sentRotation);
		Motion secondTickBaseMotion = environment.mutableBaseMotionCopy();
		simulator.simulatePreTick(user, secondTickBaseMotion, environment);
		environment.setBaseMotion(secondTickBaseMotion);
	}

	private void applySimulation(User user, Simulation simulation) {
		MetadataBundle meta = user.meta();
		MovementMetadata movementData = meta.movement();
		InventoryMetadata inventoryData = meta.inventory();

		MovementConfiguration configuration = simulation.configuration();

		boolean movementSuggestsHandIsActive = configuration.isHandActive();
		boolean packetsSuggestsHandIsActive = inventoryData.handActive();
		if (packetsSuggestsHandIsActive && !movementSuggestsHandIsActive) {
			boolean releaseHandConditions = Hypot.fast(movementData.motionX(), movementData.motionZ()) > 0.3 || movementData.ticksPast(TELEPORT) >= 2;
			boolean itemIsBow = ItemProperties.isBow(meta.inventory().activeItemType()) || ItemProperties.isBow(meta.inventory().offhandItemType());
			boolean viaVersionBlockReplacement = meta.protocol().viaVersionShieldBlockReplacement();
			if (releaseHandConditions && (!itemIsBow || (inventoryData.handActiveTicks > 3 && !viaVersionBlockReplacement)) && itemUsageReset) {
				meta.inventory().releaseItemNextTick();

				if (user.receives(MessageChannel.DEBUG_ITEM_RESETS)) {
					user.player().sendMessage(IntavePlugin.prefix() + "Requesting item usage reset as " + ChatColor.RED + "movement/state discrepancy ");
				}
			}
		}
	}

	private static final double REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT = 0.001;

	private <C, R> R collectSimulations(
		User user, Simulator simulator,
		SimulationEnvironment environment,
		Collector<Simulation, C, R> collector,
		Predicate<Simulation> earlyStop
	) {
		Timings.CHECK_PHYSICS_PROC_ITR.start();
		Timings.CHECK_PHYSICS_PROC_ITR_BUILD_CONFIGS.start();
		Set<MovementSearchConfig> configs = SEARCHER.searchConfigurationsFor(
			MovementSearchInput.from(user, simulator, environment, detectNoSlowdown)
		);
		Timings.CHECK_PHYSICS_PROC_ITR_BUILD_CONFIGS.stop();

		C container = collector.supplier().get();
		Function<C, R> finisher = collector.finisher();
		BiConsumer<C, Simulation> accumulator = collector.accumulator();

		for (MovementSearchConfig config : configs) {
			SimulationEnvironment simulationEnvironment = environment.mutableView();
			Simulation simulation = simulator.simulateTick(
				user, simulationEnvironment.mutableBaseMotionCopy(),
				simulationEnvironment, config.moveConfig()
			);
			accumulator.accept(container, simulation);
			if (earlyStop.test(simulation)) {
				break;
			}
		}
		Timings.CHECK_PHYSICS_PROC_ITR.stop();
		return finisher.apply(container);
	}

}
