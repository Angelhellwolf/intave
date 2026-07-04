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

import de.jpx3.intave.annotate.Immutable;
import de.jpx3.intave.annotate.Mutable;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.share.Rotation;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.meta.MovementMetadata;

import static de.jpx3.intave.check.movement.physics.MoveMetric.FLYING_PACKET_ACCURATE;

public abstract class Simulator {
  public void simulateBetween(
    User user, MovementMetadata metadata,
    MovementConfiguration config
  ) {
    metadata.stepHeight = stepHeight();

    /*
     * Pre-tick
     */
    Motion lastMotion = metadata.mutableBaseMotionCopy();
    Motion afterPreTickMotion = simulatePreTick(user, lastMotion, metadata);

    /*
     * Tick
     */
    Simulation simulation = simulateTick(
      user, afterPreTickMotion.copy(), metadata.immutableView(), config
    );
    metadata.assumeOccurred(simulation);
    Motion afterSimulationMotion = simulation.offsetMotion().copy();
    Position newPosition = metadata.verifiedLastPosition().add(afterSimulationMotion);
    metadata.updateMovement(newPosition, Rotation.zero());

    /*
     * Post-tick
     */
    Motion afterPostTickMotion = simulateAfterTick(
      user, metadata, metadata.position(), afterSimulationMotion
    );
    metadata.setBaseMotion(afterPostTickMotion);
    metadata.lastOnGround = metadata.onGround;
    metadata.setVerifiedLastPosition(
      metadata.position(), "AUTOACCEPT"
    );
  }

  public void simulateAround(
    User user, SimulationEnvironment environment,
    Simulation simulation,
    Position sentPosition, Rotation sentRotation
  ) {
    // assume received
    Position verifiedLastPosition = environment.verifiedLastPosition();
    Motion motionOfSimulation = simulation.offsetMotion();
    Position firstTickPosition = verifiedLastPosition.add(motionOfSimulation);
    environment.updateMovement(firstTickPosition, null);
    environment.setLastPosition(verifiedLastPosition);
    environment.assumeOccurred(simulation);

    // after tick
    Motion afterTickMotion = simulateAfterTick(user, environment, firstTickPosition, motionOfSimulation);
    environment.setBaseMotion(afterTickMotion);
    environment.setLastOnGround(environment.onGround());
    environment.activeTick(FLYING_PACKET_ACCURATE);
    environment.setVerifiedLastPosition(firstTickPosition, "Two-tick flying simulation");
    environment.tickComplete(false, false, false);

    // receive new packet
    environment.updateMovement(sentPosition, sentRotation);
    Motion afterPreTick = simulatePreTick(user, environment.mutableBaseMotionCopy(), environment);
    environment.setBaseMotion(afterPreTick);
  }

  public abstract Motion simulatePreTick(
    User user,
    @Immutable Motion baseMotion,
    @Mutable SimulationEnvironment environment
  );

  /**
   * Simulate the entire movement until the position is updated.
   * This method is a function, so it does not change the metadata, the inputs or the motion.
   * It only returns the simulation result, which contains the new motion and collision results.
   */
  public abstract Simulation simulateTick(
    User user,
    @Immutable Motion motion,
    @Immutable SimulationEnvironment environment,
    @Immutable MovementConfiguration configuration
  );

  public abstract Motion simulateAfterTick(
    User user,
    @Mutable SimulationEnvironment environment,
    @Immutable Position position,
    @Immutable Motion motion
  );

  public abstract void setback(
    User user, SimulationEnvironment environment,
    double predictedX, double predictedY, double predictedZ
  );

  public float stepHeight() {
    return 0.6f;
  }

  public boolean affectedByMovementKeys() {
    return true;
  }
}
