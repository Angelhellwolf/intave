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
import de.jpx3.intave.user.MessageChannel;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.meta.InventoryMetadata;
import de.jpx3.intave.user.meta.MetadataBundle;
import de.jpx3.intave.user.meta.MovementMetadata;
import org.bukkit.ChatColor;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static de.jpx3.intave.check.movement.physics.MoveMetric.TELEPORT;

public final class SimpleSimulationSearch implements SimulationSearch {
  private final static Searcher<MovementSearchInput, MovementSearchConfig> SEARCHER = new Searcher<>(
    MovementSearchBranchers.normal(),
    MovementSearchConfig::blank,
    false
  );

  private final boolean itemUsageReset;
  private final boolean detectNoSlowdown;

  public SimpleSimulationSearch(boolean itemUsageReset, boolean detectNoSlowdown) {
    this.itemUsageReset = itemUsageReset;
    this.detectNoSlowdown = detectNoSlowdown;
  }

  @Override
  public Simulation simulate(User user, Simulator simulator) {
    MovementMetadata movementData = user.meta().movement();
    Motion sentMotion = movementData.motion();

    Simulation simulation = collectSimulations(
      user, simulator,
      Collectors.reducing(
        Simulation.invalid(),
        (o, o2) -> o.select(o2, sentMotion)
      ),
      sim -> sim.motionDifference(sentMotion) < REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT
    );

    applySimulation(user, simulation);
//    simulation.append("i" + simulationCollector.trials());
//    simulation.append("fe" + simulationCollector.flyingEligibleTrials());
    KeyPressStudy.enterKeyPress(movementData.keyForward, movementData.keyStrafe);
    return simulation;
  }

  private static final double REQUIRED_ACCURACY_FOR_QUICK_PROC_EXIT = 0.008;

  private <C, R> R collectSimulations(
    User user, Simulator simulator,
    Collector<Simulation, C, R> collector,
    Predicate<Simulation> earlyStop
  ) {
    SimulationEnvironment environment = user.meta().movement();

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
      Simulation simulation = simulator.simulateTick(
        user, environment.mutableBaseMotionCopy(),
        environment, config.moveConfig()
      );
      accumulator.accept(container, simulation);
      if (earlyStop.test(simulation)) {
        break;
      }
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
}
