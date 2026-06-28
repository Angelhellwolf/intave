package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.player.collider.complex.SimulationResult;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserLocal;

import java.util.Objects;

import static de.jpx3.intave.math.MathHelper.distanceOf;

public final class Simulation {
  private static final Simulation INVALID_SIMULATION = new Simulation(MovementConfiguration.blank(), SimulationEnvironment.invalid(), SimulationResult.invalid());
  private static final UserLocal<Simulation> SIMULATION_OBJ_CACHE = UserLocal.withInitial(Simulation::new);

  private MovementConfiguration configuration;
  private SimulationResult simulationResult;
  private SimulationEnvironment environment;
  private String details = "";

	private final boolean mustBeCopied;

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

  public void setEnvironment(SimulationEnvironment myEnv) {
    this.environment = myEnv;
  }

  public SimulationEnvironment environment() {
    return environment;
  }

  public boolean wasSprinting() {
    return configuration.isSprinting();
  }

  public double motionDifference(Motion motionVector) {
    if (this == INVALID_SIMULATION) {
      return 100_000.d;
    }
    return distanceOf(motion(), motionVector);
  }

  public double positionDifference(Position lastPosition, Position sentPosition) {
    if (this == INVALID_SIMULATION) {
      return 100_000.d;
    }
    return distanceOf(lastPosition.add(motion()), sentPosition);
  }

  public Motion motion() {
    return simulationResult.motion();
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
    Position newPosition = environment.verifiedLastPosition().add(motion());
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
    return copy;
  }

  public Simulation select(Simulation other, Motion sentMotion) {
    if (this == INVALID_SIMULATION) {
      return other.reusableCopy();
    }
    if (other == INVALID_SIMULATION) {
      return this.reusableCopy();
    }
    double thisDistance = motionDifference(sentMotion);
    double otherDistance = other.motionDifference(sentMotion);
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
    double thisDistance = positionDifference(lastPosition, sentPosition);
    double otherDistance = other.positionDifference(lastPosition, sentPosition);
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
      simulationResult.equals(other.simulationResult);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configuration, simulationResult);
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
