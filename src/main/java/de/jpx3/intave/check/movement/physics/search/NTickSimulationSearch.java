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
import de.jpx3.intave.check.movement.physics.branch.MovementSearchBranch;
import de.jpx3.intave.check.movement.physics.branch.MovementSearchBranchers;
import de.jpx3.intave.check.movement.physics.branch.MovementSearchInput;
import de.jpx3.intave.check.movement.physics.config.MovementConfiguration;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.check.movement.physics.search.collector.MergingSimulationCollector;
import de.jpx3.intave.check.movement.physics.simulator.Simulation;
import de.jpx3.intave.check.movement.physics.simulator.Simulator;
import de.jpx3.intave.diagnostic.timings.Timings;
import de.jpx3.intave.executor.RateLimiter;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static de.jpx3.intave.check.movement.physics.MoveMetric.TELEPORT;
import static de.jpx3.intave.math.MathHelper.formatDouble;

// untested
public final class NTickSimulationSearch implements SimulationSearch {
	private final static double STRICT_ACCURACY = 0.0001;

	private final static Searcher<MovementSearchInput, MovementSearchBranch> SEARCHER = new Searcher<>(
		MovementSearchBranchers.tick(),
		MovementSearchBranch::blank
	);

	private final int ticks;
	private final boolean itemUsageReset;
	private final boolean detectNoSlowdown;

	public NTickSimulationSearch(int ticks, boolean itemUsageReset, boolean detectNoSlowdown) {
		if (ticks < 1) {
			throw new IllegalArgumentException("ticks must be at least 1");
		}
		this.ticks = ticks;
		this.itemUsageReset = itemUsageReset;
		this.detectNoSlowdown = detectNoSlowdown;
	}

	@Override
	public Set<Simulation> exhaustiveTickSearch(User user, SimulationEnvironment environment, Simulator simulator) {
		return Collections.emptySortedSet();
	}

	@Override
	public Simulation tickSearch(
		User user, SimulationEnvironment movementData,
		Simulator simulator, SimulationSearchOptions options
	) {
		Position receivedPosition = movementData.position();
		Position lastPositionB4Flying = movementData.lastPosition();
		Rotation receivedRotation = movementData.rotation();

		boolean likelyInaccurate = likelyInaccurate(movementData);
		boolean allowFuzziness = options.allowFuzziness();
		double requiredAccuracyFirstTick = requiredAccuracyForTick(1, likelyInaccurate, allowFuzziness);

		RateLimiter ratelimiter = user.meta().movement().simulationRateLimiter;
		boolean rateLimited = ratelimiter.isOverLimit();
		int maxFlyingSimulations = rateLimited ? 4 : 36;

		MergingSimulationCollector firstTickContainer = collectSimulations(
			user, simulator, movementData,
			MergingSimulationCollector.forEnvironment(user, movementData, maxFlyingSimulations),
			sim -> sim.offsetDifference() < requiredAccuracyFirstTick
		);

		int totalSimulationsDone = firstTickContainer.simulationsDone();
		long start = System.nanoTime();
		Simulation bestSimulation = firstTickContainer.bestSimulation();
		double bestDistance = bestSimulation.offsetDifference();

		List<Simulation> firstTickFlyingSimulations = firstTickContainer.flyingSimulations();
		if ((firstTickFlyingSimulations.isEmpty() || bestDistance < requiredAccuracyFirstTick) && bestSimulation.canFinishExplicitTick()) {
			return finishSearch(
				user, ratelimiter, bestSimulation, totalSimulationsDone, start, firstTickFlyingSimulations.isEmpty(),
				searchSuffixForCompletedTicks(1)
			);
		}

		List<Simulation> flyingCandidates = firstTickFlyingSimulations;
		List<Integer> flyingCandidateCounts = new ArrayList<>(
			Collections.nCopies(firstTickFlyingSimulations.size(), firstTickFlyingSimulations.size())
		);
		int flyingDepth = 1;
		while (flyingDepth < ticks && !flyingCandidates.isEmpty()) {
			List<Simulation> nextFlyingCandidates = new ArrayList<>();
			List<Integer> nextFlyingCandidateCounts = new ArrayList<>();

			for (int i = 0; i < flyingCandidates.size(); i++) {
				Simulation flyingSimulation = flyingCandidates.get(i);
				if (totalSimulationsDone > simulationLimitForFlyingDepth(flyingDepth, rateLimited)) {
					continue;
				}

				SimulationEnvironment tickEnvironment = flyingSimulation.environment().mutableView();
				simulator.simulateAround(
					user, tickEnvironment, flyingSimulation,
					receivedPosition, receivedRotation
				);

				int completedTicks = flyingDepth + 1;
				Motion remainingMotion = tickEnvironment.sentOffsetMotion();
				double requiredAccuracy = requiredAccuracyForTick(completedTicks, likelyInaccurate, allowFuzziness);
				MergingSimulationCollector tickContainer = collectSimulations(
					user, simulator, tickEnvironment,
					MergingSimulationCollector.forEnvironmentWithCustomTargets(
						user, tickEnvironment, remainingMotion, lastPositionB4Flying, maxFlyingSimulations
					),
					sim -> sim.offsetDifference() < requiredAccuracy
				);
				totalSimulationsDone += tickContainer.simulationsDone();

				Simulation tickSimulation = tickContainer.bestSimulation();
				tickSimulation.appendBlue(flyingDepth + "f/" + flyingCandidateCounts.get(i) + "x");
				double tickDistance = tickSimulation.offsetDifference();
				if (tickDistance < bestDistance && tickSimulation.canFinishExplicitTick()) {
					bestSimulation = tickSimulation.reusableCopy();
					bestDistance = tickDistance;
				}

				if (bestDistance < STRICT_ACCURACY) {
					return finishSearch(
						user, ratelimiter, bestSimulation, totalSimulationsDone, start, false,
						searchSuffixForCompletedTicks(completedTicks)
					);
				}

				if (completedTicks < ticks) {
					List<Simulation> hiddenTickCandidates = tickContainer.flyingSimulations();
					nextFlyingCandidates.addAll(hiddenTickCandidates);
					nextFlyingCandidateCounts.addAll(
						Collections.nCopies(hiddenTickCandidates.size(), hiddenTickCandidates.size())
					);
				}
			}

			flyingCandidates = nextFlyingCandidates;
			flyingCandidateCounts = nextFlyingCandidateCounts;
			flyingDepth++;
		}

		return finishSearch(
			user, ratelimiter, bestSimulation, totalSimulationsDone, start, true,
			searchSuffixForCompletedTicks(ticks + 1)
		);
	}

	@Override
	public List<Motion> afterTickMotionCandidates(User user, SimulationEnvironment environment, Simulator simulator, Position newPosition, PostTickMotionType motionType) {
		return Collections.emptyList();
	}

	private Simulation finishSearch(
		User user,
		RateLimiter ratelimiter,
		Simulation bestSimulation,
		int totalSimulationsDone,
		long start,
		boolean exhaustive,
		String simulationCountSuffix
	) {
		if (totalSimulationsDone > 1) {
			bestSimulation.appendBlue(totalSimulationsDone + simulationCountSuffix + "s");
		}
		double durationMs = ((double)System.nanoTime() - start) / 1_000_000d;
		bestSimulation.appendBlue(formatDouble(durationMs, 4) + "ms");
		if (exhaustive) {
			bestSimulation.setWasFromExhaustiveSearch();
		}
		applySimulation(user, bestSimulation);
		ratelimiter.noteAcquired(totalSimulationsDone);
		return bestSimulation;
	}

	private double requiredAccuracyForTick(int tick, boolean likelyInaccurate, boolean allowFuzziness) {
		if (!likelyInaccurate || !allowFuzziness) {
			return STRICT_ACCURACY;
		}
		return tick == 1 ? 0.001 : tick == 2 ? 0.03 : 0.04;
	}

	private int simulationLimitForFlyingDepth(int flyingDepth, boolean rateLimited) {
		return flyingDepth == 1 ? (rateLimited ? 256 : 512) : (rateLimited ? 0 : 256);
	}

	private String searchSuffixForCompletedTicks(int completedTicks) {
		int suffixOffset = Math.max(0, Math.min(25, completedTicks - 1));
		return Character.toString((char)('a' + suffixOffset));
	}

	private void applySimulation(User user, Simulation simulation) {
		MetadataBundle meta = user.meta();
		MovementMetadata movementData = meta.movement();
		InventoryMetadata inventoryData = meta.inventory();

		MovementConfiguration configuration = simulation.configuration();

		boolean movementSuggestsHandIsActive = configuration.isHandActive();
		boolean packetsSuggestsHandIsActive = inventoryData.handActive();
		if (packetsSuggestsHandIsActive && !movementSuggestsHandIsActive) {
			boolean releaseHandConditions = Hypot.fast(movementData.offsetMotionX(), movementData.offsetMotionZ()) > 0.3 || movementData.ticksPast(TELEPORT) >= 2;
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

	private boolean likelyInaccurate(SimulationEnvironment movementData) {
		if (Math.abs(movementData.offsetMotionY()) < 0.05
			&& Math.abs(movementData.baseMotionX()) < 0.03 && Math.abs(movementData.baseMotionZ()) < 0.03) {
			return true;
		}
		return movementData.isSneaking() || movementData.inWater();
	}

	private <C, R> R collectSimulations(
		User user, Simulator simulator,
		SimulationEnvironment environment,
		Collector<Simulation, C, R> collector,
		Predicate<Simulation> earlyStop
	) {
		Timings.CHECK_PHYSICS_PROC_ITR.start();
		Timings.CHECK_PHYSICS_PROC_ITR_BUILD_CONFIGS.start();
		Set<MovementSearchBranch> possibleConfigs = SEARCHER.searchConfigurationsFor(
			MovementSearchInput.forTick(user, simulator, environment, detectNoSlowdown)
		);
		Timings.CHECK_PHYSICS_PROC_ITR_BUILD_CONFIGS.stop();

		C container = collector.supplier().get();
		Function<C, R> finisher = collector.finisher();
		BiConsumer<C, Simulation> accumulator = collector.accumulator();

		for (MovementSearchBranch config : possibleConfigs) {
			boolean canFinishExplicitTick = config.canFinishExplicitTick();
			SimulationEnvironment myEnv = config.modifiedMutableView(environment);
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
