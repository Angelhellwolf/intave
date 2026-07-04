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

package de.jpx3.intave.check.movement.physics.search;

import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.check.movement.physics.MovementConfiguration;
import de.jpx3.intave.check.movement.physics.Simulation;
import de.jpx3.intave.check.movement.physics.Simulator;
import de.jpx3.intave.check.movement.physics.branch.MovementSearchBranchers;
import de.jpx3.intave.check.movement.physics.branch.MovementSearchConfig;
import de.jpx3.intave.check.movement.physics.branch.MovementSearchInput;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.diagnostic.timings.Timings;
import de.jpx3.intave.math.Hypot;
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

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static de.jpx3.intave.check.movement.physics.MoveMetric.TELEPORT;
import static de.jpx3.intave.math.MathHelper.formatDouble;

public final class ThreeTickSimulationSearch implements SimulationSearch {
	private final static Searcher<MovementSearchInput, MovementSearchConfig> SEARCHER = new Searcher<>(
		MovementSearchBranchers.normal(),
		MovementSearchConfig::blank
	);

	private final boolean itemUsageReset;
	private final boolean detectNoSlowdown;

	public ThreeTickSimulationSearch(boolean itemUsageReset, boolean detectNoSlowdown) {
		this.itemUsageReset = itemUsageReset;
		this.detectNoSlowdown = detectNoSlowdown;
	}

	@Override
	public Simulation greedyNarrowSearch(User user, SimulationEnvironment environment, Simulator simulator) {
		return greedySearch(user, environment, simulator, false);
	}

	@Override
	public Simulation greedyFullSearch(User user, SimulationEnvironment environment, Simulator simulator) {
		return greedySearch(user, environment, simulator, true);
	}

	private Simulation greedySearch(User user, SimulationEnvironment movementData, Simulator simulator, boolean full) {
		Position receivedPosition = movementData.position();
		Position lastPositionB4Flying = movementData.lastPosition();

		int maxFlyingSimulations = full ? Integer.MAX_VALUE : 3;

		// Go through all this-tick possibilities
		SimulationCollector firstTickContainer = collectSimulations(
			user, simulator, movementData,
			SimulationCollector.forEnvironment(user, movementData, maxFlyingSimulations),
			sim -> sim.offsetDifference() < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
		);

		int totalSimulationsDone = firstTickContainer.simulationsDone();
		long start = System.nanoTime();

		List<Simulation> firstTickFlyingSimulations = firstTickContainer.flyingSimulations();
		Simulation bestSimulation = firstTickContainer.bestSimulation();

		if (firstTickFlyingSimulations.isEmpty() || bestSimulation.offsetDifference() < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT) {
			if (bestSimulation.canFinishExplicitTick()) {
				if (totalSimulationsDone > 1) {
					bestSimulation.append(totalSimulationsDone + "as");
				}
				double durationMs = ((double)System.nanoTime() - start) / 1_000_000d;
				bestSimulation.append(formatDouble(durationMs, 4) + "ms");
				applySimulation(user, bestSimulation);
				return bestSimulation;
			}
		}
		double bestDistance = bestSimulation.offsetDifference();

		for (Simulation firstTickSimulation : firstTickFlyingSimulations) {
			SimulationEnvironment firstTickEnvironment = firstTickSimulation.environment().mutableView();
			simulator.simulateAround(
				user, firstTickEnvironment, firstTickSimulation,
				receivedPosition, movementData.rotation()
			);

			// If simulating take too long, we can not search that deep
			if (totalSimulationsDone > 512 && !full) {
				continue;
			}

			Motion secondTickRemainingMotion = firstTickEnvironment.sentOffsetMotion();
			SimulationCollector secondTickContainer = collectSimulations(
				user, simulator, firstTickEnvironment,
				SimulationCollector.forEnvironmentWithCustomTargets(
					user, firstTickEnvironment, secondTickRemainingMotion, lastPositionB4Flying, maxFlyingSimulations
				),
				sim -> sim.positionDifference(receivedPosition) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
			);
			totalSimulationsDone += secondTickContainer.simulationsDone();

			Simulation secondTickSimulation = secondTickContainer.bestSimulation();
			secondTickSimulation.append("1f");
			double secondTickDistance = secondTickSimulation.positionDifference(receivedPosition);
			if (secondTickDistance < bestDistance && secondTickSimulation.canFinishExplicitTick()) {
				bestSimulation = secondTickSimulation.reusableCopy();
				bestDistance = secondTickDistance;
			}

			if (bestDistance < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT) {
				if (totalSimulationsDone > 1) {
					bestSimulation.append(totalSimulationsDone + "bs");
				}
				double durationMs = ((double)System.nanoTime() - start) / 1_000_000d;
				bestSimulation.append(formatDouble(durationMs, 4) + "ms");
				applySimulation(user, bestSimulation);
				return bestSimulation;
			}

			for (Simulation secondTickFlyingSimulation : secondTickContainer.flyingSimulations()) {
				SimulationEnvironment secondTickEnvironment = secondTickFlyingSimulation.environment().mutableView();
				simulator.simulateAround(
					user, secondTickEnvironment, secondTickFlyingSimulation,
					receivedPosition, movementData.rotation()
				);

				// If simulating take too long, we can not search that deep
				if (totalSimulationsDone > 256 && !full) {
					continue;
				}

				Motion thirdTickRemainingMotion = secondTickEnvironment.sentOffsetMotion();
				SimulationCollector thirdTickSimulator = collectSimulations(
					user, simulator, secondTickEnvironment,
					SimulationCollector.forEnvironmentWithCustomTargets(
						user, secondTickEnvironment, thirdTickRemainingMotion, lastPositionB4Flying, maxFlyingSimulations
					),
					sim -> sim.positionDifference(receivedPosition) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
				);
				totalSimulationsDone += thirdTickSimulator.simulationsDone();

				Simulation thirdTickSimulation = thirdTickSimulator.bestSimulation();
				thirdTickSimulation.append("2f");
				double thirdTickDistance = thirdTickSimulation.positionDifference(receivedPosition);
				if (thirdTickDistance < bestDistance && thirdTickSimulation.canFinishExplicitTick()) {
					bestSimulation = thirdTickSimulation.reusableCopy();
					bestDistance = thirdTickDistance;
				}

				if (bestDistance < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT) {
					if (totalSimulationsDone > 1) {
						bestSimulation.append(totalSimulationsDone + "cs");
					}
					double durationMs = ((double)System.nanoTime() - start) / 1_000_000d;
					bestSimulation.append(formatDouble(durationMs, 4) + "ms");
					applySimulation(user, bestSimulation);
					return bestSimulation;
				}
			}
		}
		if (totalSimulationsDone > 1) {
			bestSimulation.append(totalSimulationsDone + "ds");
		}
		double durationMs = ((double)System.nanoTime() - start) / 1_000_000d;
		bestSimulation.append(formatDouble(durationMs, 4) + "ms");
		applySimulation(user, bestSimulation);
		return bestSimulation;
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
		Set<MovementSearchConfig> possibleConfigs = SEARCHER.searchConfigurationsFor(
			MovementSearchInput.from(user, simulator, environment, detectNoSlowdown)
		);
		Timings.CHECK_PHYSICS_PROC_ITR_BUILD_CONFIGS.stop();

		C container = collector.supplier().get();
		Function<C, R> finisher = collector.finisher();
		BiConsumer<C, Simulation> accumulator = collector.accumulator();

		for (MovementSearchConfig config : possibleConfigs) {
			boolean canFinishExplicitTick = config.canFinishExplicitTick();
			SimulationEnvironment myEnv = environment.mutableView();
			myEnv = config.applyTo(myEnv);
			Simulation simulation = simulator.simulateTick(
				user, myEnv.mutableBaseMotionCopy(),
				myEnv.immutableView(), config.moveConfig()
			);
			simulation.setEnvironment(myEnv);
			simulation.setCanFinishExplicitTick(canFinishExplicitTick);
			accumulator.accept(container, simulation);
			if (canFinishExplicitTick && earlyStop.test(simulation)) {
				break;
			}
			simulation.expire();
		}
		Timings.CHECK_PHYSICS_PROC_ITR.stop();
		return finisher.apply(container);
	}
}
