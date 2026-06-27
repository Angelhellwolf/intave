package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.check.movement.physics.search.MovementSearchBranchers;
import de.jpx3.intave.check.movement.physics.search.MovementSearchConfig;
import de.jpx3.intave.check.movement.physics.search.MovementSearchInput;
import de.jpx3.intave.diagnostic.timings.Timings;
import de.jpx3.intave.math.Hypot;
import de.jpx3.intave.player.ActionBar;
import de.jpx3.intave.player.ItemProperties;
import de.jpx3.intave.search.Searcher;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.user.MessageChannel;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.meta.InventoryMetadata;
import de.jpx3.intave.user.meta.MetadataBundle;
import de.jpx3.intave.user.meta.MovementMetadata;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static de.jpx3.intave.check.movement.physics.MoveMetric.TELEPORT;

public final class ThreeTickSimulationSearch implements SimulationSearch {
	private final static Searcher<MovementSearchInput, MovementSearchConfig> SEARCHER = new Searcher<>(
		MovementSearchBranchers.normal(),
		MovementSearchConfig::blank,
		false
	);

	private final boolean itemUsageReset;
	private final boolean detectNoSlowdown;

	public ThreeTickSimulationSearch(boolean itemUsageReset, boolean detectNoSlowdown) {
		this.itemUsageReset = itemUsageReset;
		this.detectNoSlowdown = detectNoSlowdown;
	}

	@Override
	public Simulation simulate(User user, SimulationEnvironment movementData, Simulator simulator) {
		Position sentPosition = movementData.position();
		Position lastPositionB4Flying = movementData.lastPosition();

		FPCSimulationContainer firstTickContainer = collectSimulations(
			user, simulator, movementData,
			flyingPacketCandidates(user, movementData),
			sim -> sim.positionDifference(lastPositionB4Flying, sentPosition) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
		);

		List<Simulation> firstTickFlyingSimulations = firstTickContainer.possibleFlyingSimulations();
		Simulation bestSimulation = firstTickContainer.bestSimulation();

		if (firstTickFlyingSimulations.isEmpty() || bestSimulation.positionDifference(lastPositionB4Flying, sentPosition) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT) {
			applySimulation(user, bestSimulation);
			return bestSimulation;
		}

		SimulationEnvironment bestEnvironment = null;
		double bestDistance = 2 * bestSimulation.positionDifference(lastPositionB4Flying, sentPosition);

		int firstFlyTickRuns = 0;
		int secondFlyTickRuns = 0;

		for (Simulation firstTickSimulation : firstTickFlyingSimulations) {
			SimulationEnvironment firstTickEnvironment = movementData.mutableView();
			simulator.simulateAround(
				user, firstTickEnvironment, firstTickSimulation,
				sentPosition, movementData.rotation()
			);

			Motion secondTickRemainingMotion = firstTickEnvironment.motion();
			MotionTrackingFPCSimulationContainer secondTickContainer = collectSimulations(
				user, simulator, firstTickEnvironment,
				motionTrackingFlyingPacketCandidates(
					user, firstTickEnvironment, secondTickRemainingMotion, lastPositionB4Flying
				),
				sim -> sim.motionDifference(secondTickRemainingMotion) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT &&
					sim.positionDifference(lastPositionB4Flying, sentPosition) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
			);

			firstFlyTickRuns++;

			Simulation secondTickSimulation = secondTickContainer.bestSimulation();
			secondTickSimulation.append("2t");
			double secondTickDistance = simulationDistance(
				secondTickSimulation, firstTickEnvironment,
				sentPosition, secondTickRemainingMotion
			);
			if (secondTickDistance < bestDistance) {
				bestSimulation = secondTickSimulation.reusableCopy();
				bestDistance = secondTickDistance;
				bestEnvironment = firstTickEnvironment;
			}

			for (Simulation secondTickFlyingSimulation : secondTickContainer.possibleFlyingSimulations()) {
				SimulationEnvironment secondTickEnvironment = movementData.mutableView();
				simulator.simulateAround(
					user, secondTickEnvironment, firstTickSimulation,
					sentPosition, movementData.rotation()
				);
				simulator.simulateAround(
					user, secondTickEnvironment, secondTickFlyingSimulation,
					sentPosition, movementData.rotation()
				);

				Motion thirdTickRemainingMotion = secondTickEnvironment.motion();
				Simulation thirdTickSimulation = collectSimulations(
					user, simulator, secondTickEnvironment,
					Collectors.reducing(
						Simulation.invalid(),
						(o, o2) -> o.select(o2, thirdTickRemainingMotion)
					),
					sim -> sim.motionDifference(thirdTickRemainingMotion) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT &&
						sim.positionDifference(secondTickEnvironment.lastPosition(), sentPosition) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
				);

				secondFlyTickRuns++;

				thirdTickSimulation.append("3t");
				double thirdTickDistance = simulationDistance(
					thirdTickSimulation, secondTickEnvironment,
					sentPosition, thirdTickRemainingMotion
				);
				if (thirdTickDistance < bestDistance) {
					bestSimulation = thirdTickSimulation.reusableCopy();
					bestDistance = thirdTickDistance;
					bestEnvironment = secondTickEnvironment;
				}
			}
		}

		if (bestEnvironment != null) {
			bestEnvironment.commitTo(movementData);
		}

		ActionBar.sendActionBar(
			user.player(),
			ChatColor.GRAY + "L-"+firstFlyTickRuns+"-"+secondFlyTickRuns+"-|?"
		);
		applySimulation(user, bestSimulation);
		return bestSimulation;
	}

	private Collector<Simulation, FPCSimulationContainer, FPCSimulationContainer> flyingPacketCandidates(
		User user, SimulationEnvironment environment
	) {
		return Collector.of(
			FPCSimulationContainer::def,
			(c, o) -> c.add(o, user, environment),
			(c1, c2) -> c1.mergedWith(c2, user, environment),
			Function.identity()
		);
	}

	private Collector<Simulation, MotionTrackingFPCSimulationContainer, MotionTrackingFPCSimulationContainer> motionTrackingFlyingPacketCandidates(
		User user,
		SimulationEnvironment environment,
		Motion targetMotion,
		Position lastReportedPosition
	) {
		return Collector.of(
			() -> new MotionTrackingFPCSimulationContainer(user, environment, targetMotion, lastReportedPosition),
			MotionTrackingFPCSimulationContainer::add,
			MotionTrackingFPCSimulationContainer::mergedWith,
			Function.identity()
		);
	}

	private double simulationDistance(
		Simulation simulation,
		SimulationEnvironment environment,
		Position sentPosition,
		Motion remainingMotion
	) {
		return simulation.positionDifference(environment.lastPosition(), sentPosition) +
			simulation.motionDifference(remainingMotion);
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

	private static final double REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT = 0.0001;

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
			SimulationEnvironment myEnv = environment;
			if (config.rotation() != null) {
				myEnv = myEnv.mutableView();
				myEnv.setRotation(config.rotation());
			}
			Simulation simulation = simulator.simulateTick(
				user, myEnv.mutableBaseMotionCopy(),
				myEnv.unmodifiable(), config.moveConfig()
			);
			accumulator.accept(container, simulation);
			if (earlyStop.test(simulation)) {
				break;
			}
		}
		Timings.CHECK_PHYSICS_PROC_ITR.stop();
		return finisher.apply(container);
	}

	private static final class MotionTrackingFPCSimulationContainer {
		private final User user;
		private final SimulationEnvironment environment;
		private final Motion targetMotion;
		private final Position lastReportedPosition;
		private Simulation best = Simulation.invalid();
		private List<Simulation> possibleFlyingSimulations;
		private int totalFlyingPacketSimulations;

		private MotionTrackingFPCSimulationContainer(
			User user,
			SimulationEnvironment environment,
			Motion targetMotion,
			Position lastReportedPosition
		) {
			this.user = user;
			this.environment = environment;
			this.targetMotion = targetMotion;
			this.lastReportedPosition = lastReportedPosition;
		}

		private void add(Simulation simulation) {
			double flyingLimit = user.meta().protocol().flyingPacketUncertaintyRadius();
			Position simulatedPosition = environment.verifiedLastPosition().add(simulation.motion());

			if (lastReportedPosition.distance(simulatedPosition) < flyingLimit) {
				if (possibleFlyingSimulations == null) {
					possibleFlyingSimulations = new ArrayList<>(8);
				}
				boolean contained = false;
				for (Simulation flyingSimulation : possibleFlyingSimulations) {
					if (flyingSimulation.result().almostIdenticalTo(simulation.result())) {
						contained = true;
						break;
					}
				}
				if (!contained && possibleFlyingSimulations.size() < 64) {
					possibleFlyingSimulations.add(simulation.reusableCopy());
				}
				totalFlyingPacketSimulations++;
			}
			best = best.select(simulation, targetMotion);
		}

		private MotionTrackingFPCSimulationContainer mergedWith(
			MotionTrackingFPCSimulationContainer other
		) {
			MotionTrackingFPCSimulationContainer merged =
				new MotionTrackingFPCSimulationContainer(user, environment, targetMotion, lastReportedPosition);
			merged.add(best);
			merged.add(other.best);
			if (possibleFlyingSimulations != null) {
				for (Simulation simulation : possibleFlyingSimulations) {
					merged.add(simulation);
				}
			}
			if (other.possibleFlyingSimulations != null) {
				for (Simulation simulation : other.possibleFlyingSimulations) {
					merged.add(simulation);
				}
			}
			merged.totalFlyingPacketSimulations = totalFlyingPacketSimulations + other.totalFlyingPacketSimulations;
			return merged;
		}

		private Simulation bestSimulation() {
			return best;
		}

		private List<Simulation> possibleFlyingSimulations() {
			return possibleFlyingSimulations == null ? Collections.emptyList() : possibleFlyingSimulations;
		}
	}
}
