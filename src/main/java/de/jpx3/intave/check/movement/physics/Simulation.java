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

import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.player.collider.complex.SimulationResult;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.user.ThreadUserLocal;
import de.jpx3.intave.user.User;

import java.util.Objects;

import static de.jpx3.intave.math.MathHelper.distanceOf;

public final class Simulation {
  private static final Simulation INVALID_SIMULATION = new Simulation(MovementConfiguration.blank(), SimulationEnvironment.invalid(), SimulationResult.invalid());
  private static final ThreadUserLocal<Simulation> SIMULATION_OBJ_CACHE = ThreadUserLocal.withInitial(Simulation::new);

  private MovementConfiguration configuration;
  private SimulationResult simulationResult;
  private SimulationEnvironment environment;
  private String details = "";

	private final boolean mustBeCopied;
  private boolean canFinishExplicitTick;

	private Simulation() {
    this.mustBeCopied = true;
  }

  private Simulation(
    MovementConfiguration configuration,
    SimulationEnvironment environment,
    SimulationResult simulationResult
  ) {
    this.configuration = configuration;
    this.environment = environment;
    this.simulationResult = simulationResult;
    this.mustBeCopied = false;
  }

  public void flush(
    MovementConfiguration configuration,
    SimulationEnvironment environment,
    SimulationResult simulationResult
  ) {
    this.configuration = configuration;
    this.environment = environment;
    this.simulationResult = simulationResult;
    this.details = "";
  }

  public void expire() {
    this.configuration = MovementConfiguration.blank();
    this.environment = SimulationEnvironment.invalid();
    this.simulationResult = SimulationResult.invalid();
    this.canFinishExplicitTick = false;
    this.details = "";
  }

  public void setEnvironment(SimulationEnvironment myEnv) {
    this.environment = myEnv;
  }

  public void setCanFinishExplicitTick(boolean canFinishExplicitTick) {
    this.canFinishExplicitTick = canFinishExplicitTick;
  }

  public boolean canFinishExplicitTick() {
    return canFinishExplicitTick;
  }

  public SimulationEnvironment environment() {
    return environment;
  }

  public boolean wasSprinting() {
    return configuration.isSprinting();
  }

  public double offsetDifference() {
    if (this == INVALID_SIMULATION) {
      return 100_000.d;
    }
    return distanceOf(offsetMotion(), environment.sentOffsetMotion());
  }

  public double positionDifference(Position sentPosition) {
    if (this == INVALID_SIMULATION) {
      return 100_000.d;
    }
    return distanceOf(environment.lastPosition().add(offsetMotion()), sentPosition);
  }

  public Motion offsetMotion() {
    return simulationResult.offsetMotion();
  }

  public void append(String details) {
    this.details += details;
  }

  public String details() {
    return details;
  }

  public boolean resultsInFlyingPacket(
    SimulationEnvironment environment, double limit
  ) {
    Position lastReportedPosition = environment.lastPosition();
    Position newPosition = environment.verifiedLastPosition().add(offsetMotion());
    double distance = lastReportedPosition.distance(newPosition);
    return distance < limit;
  }

  public SimulationResult result() {
    return simulationResult;
  }

  public MovementConfiguration configuration() {
    return configuration;
  }

  public Simulation reusableCopy() {
    Simulation copy = new Simulation(configuration, environment, simulationResult);
    copy.details = details;
    copy.canFinishExplicitTick = canFinishExplicitTick;
    return copy;
  }

  public Simulation select(Simulation other, Motion sentMotion) {
    if (this == INVALID_SIMULATION) {
      return other.reusableCopy();
    }
    if (other == INVALID_SIMULATION) {
      return this.reusableCopy();
    }
    if (this.canFinishExplicitTick && !other.canFinishExplicitTick) {
      return this.reusableCopy();
    } else if (!this.canFinishExplicitTick && other.canFinishExplicitTick) {
      return other.reusableCopy();
    }
    double thisDistance = offsetDifference();
    double otherDistance = other.offsetDifference();
    Simulation selectedSimulation = thisDistance < otherDistance ? this : other;
    if (selectedSimulation.mustBeCopied) {
      selectedSimulation = selectedSimulation.reusableCopy();
    }
    return selectedSimulation;
  }

  public Simulation select(Simulation other, Position sentPosition, Position lastPosition) {
    if (this == INVALID_SIMULATION) {
      return other.reusableCopy();
    }
    if (other == INVALID_SIMULATION) {
      return this.reusableCopy();
    }
    if (this.canFinishExplicitTick && !other.canFinishExplicitTick) {
      return this.reusableCopy();
    } else if (!this.canFinishExplicitTick && other.canFinishExplicitTick) {
      return other.reusableCopy();
    }
    double thisDistance = positionDifference(sentPosition);
    double otherDistance = other.positionDifference(sentPosition);
    Simulation selectedSimulation = thisDistance < otherDistance ? this : other;
    if (selectedSimulation.mustBeCopied) {
      selectedSimulation = selectedSimulation.reusableCopy();
    }
    return selectedSimulation;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Simulation)) {
      return false;
    }
    Simulation other = (Simulation) obj;
    return configuration.equals(other.configuration) &&
      simulationResult.equals(other.simulationResult) &&
      canFinishExplicitTick == other.canFinishExplicitTick;
  }

  @Override
  public int hashCode() {
    return Objects.hash(configuration, simulationResult, canFinishExplicitTick);
  }

  static Simulation of(User user, MovementConfiguration configuration, SimulationEnvironment environment, SimulationResult simulationResult) {
    Simulation simulation = SIMULATION_OBJ_CACHE.get(user);
    simulation.flush(configuration, environment, simulationResult);
    return simulation;
  }

  public static Simulation invalid() {
    return INVALID_SIMULATION;
  }
}
