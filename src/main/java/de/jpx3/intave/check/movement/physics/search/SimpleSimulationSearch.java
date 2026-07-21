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
import de.jpx3.intave.check.movement.physics.simulator.Simulation;
import de.jpx3.intave.check.movement.physics.simulator.Simulator;
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static de.jpx3.intave.check.movement.physics.environment.MoveMetric.TELEPORT;

public final class SimpleSimulationSearch implements SimulationSearch {
  private final static Searcher<MovementSearchInput, MovementSearchBranch> TICK_SEARCHER = new Searcher<>(
    MovementSearchBranchers.tick(),
    MovementSearchBranch::blank
  );
	private final static Searcher<MovementSearchInput, MovementSearchBranch> POST_TICK_SEARCHER = new Searcher<>(
		MovementSearchBranchers.afterTick(),
		MovementSearchBranch::blank
	);

  private final boolean itemUsageReset;
  private final boolean detectNoSlowdown;

  public SimpleSimulationSearch(boolean itemUsageReset, boolean detectNoSlowdown) {
    this.itemUsageReset = itemUsageReset;
    this.detectNoSlowdown = detectNoSlowdown;
  }

	@Override
	public Set<Simulation> exhaustiveTickSearch(User user, SimulationEnvironment environment, Simulator simulator) {
		return collectTickSimulations(
			user, simulator, environment,
			Collectors.toSet(),
			sim -> sim.offsetDifference() < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
		);
	}

	public Simulation tickSearch(User user, SimulationEnvironment environment, Simulator simulator, SimulationSearchOptions options) {
		Simulation simulation = collectTickSimulations(
			user, simulator, environment,
			Collectors.reducing(
				Simulation.invalid(),
				Simulation::select
			),
			sim -> sim.offsetDifference() < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
		);
		applySimulation(user, simulation);
		return simulation;
	}

	@Override
	@Deprecated
	public List<Motion> afterTickMotionCandidates(User user, SimulationEnvironment environment, Simulator simulator, Position newPosition, PostTickMotionType motionType) {
		return Collections.emptyList();
	}

	private static final double REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT = 0.001;

	private <C, R> R collectTickSimulations(
		User user, Simulator simulator,
		SimulationEnvironment environment,
		Collector<Simulation, C, R> collector,
		Predicate<Simulation> earlyStop
	) {
		Timings.CHECK_PHYSICS_PROC_ITR.start();
		Timings.CHECK_PHYSICS_PROC_ITR_BUILD_CONFIGS.start();
		Set<MovementSearchBranch> possibleConfigs = TICK_SEARCHER.searchConfigurationsFor(
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

  private void applySimulation(User user, Simulation simulation) {
    MetadataBundle meta = user.meta();
    MovementMetadata movementData = meta.movement();
    InventoryMetadata inventoryData = meta.inventory();

	  /* misplaced - please solve this otherwise */
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
}
