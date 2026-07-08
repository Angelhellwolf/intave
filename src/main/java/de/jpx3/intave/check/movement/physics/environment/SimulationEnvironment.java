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

package de.jpx3.intave.check.movement.physics.environment;

import de.jpx3.intave.annotate.Nullable;
import de.jpx3.intave.block.fluid.Fluid;
import de.jpx3.intave.check.movement.physics.*;
import de.jpx3.intave.check.movement.physics.update.CausalConstraint;
import de.jpx3.intave.check.movement.physics.update.TickAmbiguousUpdate;
import de.jpx3.intave.module.tracker.entity.Entity;
import de.jpx3.intave.player.collider.complex.SimulationResult;
import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.share.Rotation;
import de.jpx3.intave.world.border.WorldBorder;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface SimulationEnvironment {
  Pose pose();
  Vector lookVector();

  void updateMovement(
	  double newPositionX, double newPositionY, double newPositionZ,
	  float newRotationYaw, float newRotationPitch,
	  boolean hasMovement, boolean hasRotation
  );

  default void updateMovement(
    @Nullable Position newPosition,
    @Nullable Rotation newRotation
  ) {
    boolean hasMovement = newPosition != null;
    boolean hasRotation = newRotation != null;
    updateMovement(
      hasMovement ? newPosition.getX() : 0,
      hasMovement ? newPosition.getY() : 0,
      hasMovement ? newPosition.getZ() : 0,
      hasRotation ? newRotation.yaw() : 0,
      hasRotation ? newRotation.pitch() : 0,
      hasMovement,
      hasRotation
    );
  }

  void setRotation(float newRotationYaw, float newRotationPitch);

  default void setRotation(@Nullable Rotation newRotation) {
    if (newRotation != null) {
      setRotation(newRotation.yaw(), newRotation.pitch());
    }
  }

  default Position position() {
    return new Position(positionX(), positionY(), positionZ());
  }
  double positionX();
  double positionY();
  double positionZ();

  default Position verifiedLastPosition() {
    return new Position(verifiedLastPositionX(), verifiedLastPositionY(), verifiedLastPositionZ());
  }
  double verifiedLastPositionX();
  double verifiedLastPositionY();
  double verifiedLastPositionZ();

  void setVerifiedLastPosition(Position position, String reason);

  default Position lastPosition() {
    return new Position(lastPositionX(), lastPositionY(), lastPositionZ());
  }
  double lastPositionX();
  double lastPositionY();
  double lastPositionZ();

  default Rotation lastRotation() {
    return new Rotation(lastRotationYaw(), lastRotationPitch());
  }
  float lastRotationYaw();
  float lastRotationPitch();

  default void setLastRotation(Rotation rotation) {
    setLastRotation(rotation.yaw(), rotation.pitch());
  }

  void setLastRotation(float lastRotationYaw, float lastRotationPitch);

  default void setLastPosition(Position position) {
    setLastPosition(position.getX(), position.getY(), position.getZ());
  }

  void setLastPosition(double x, double y, double z);

  void setBoundingBox(BoundingBox boundingBox);
  BoundingBox boundingBox();

  default Motion sentOffsetMotion() {
    return new Motion(motionX(), motionY(), motionZ());
  }
  double motionX();
  double motionY();
  double motionZ();

  default Motion mutableBaseMotionCopy() {
    return new Motion(baseMotionX(), baseMotionY(), baseMotionZ());
  }
  double baseMotionX();
  double baseMotionY();
  double baseMotionZ();

  default void setBaseMotion(Motion baseMotion) {
    setBaseMotion(baseMotion.motionX(), baseMotion.motionY(), baseMotion.motionZ());
  }
  void setBaseMotion(
    double baseMotionX,
    double baseMotionY,
    double baseMotionZ
  );

  boolean motionXReset();
  boolean motionZReset();

  Vector motionMultiplier();
  void resetMotionMultiplier();

  WorldBorder border();
  void setWorldBorder(@NotNull WorldBorder worldBorder);

  default Rotation rotation() {
    return new Rotation(rotationYaw(), rotationPitch());
  }

  float rotationYaw();
  float yawSine();
  float yawCosine();

  float rotationPitch();

  float aiMoveSpeed(boolean sprinting);
  float friction(boolean sprinting);
  double stepHeight();
  double resetMotion();

  double jumpMotion();
  void setJumpMotion(double jumpMotion);
  boolean hasJumpedInTick();

  double gravity();

  float blockSpeedFactor();
  float jumpMovementFactor();

  boolean isSneaking();
  boolean isSprinting();
  boolean hasSprintSpeed();
  boolean sprintingAllowed();
  boolean inWater();
  void setInWater(boolean inWater);
  boolean inLava();
  boolean inWeb();
  void resetInWeb();
  boolean onGround();

  boolean lastOnGround();
  void setLastOnGround(boolean lastOnGround);
  boolean collidedHorizontally();
  boolean collidedVertically();

  void checkSupportingBlock(Motion motion);
  void clearSupportingBlock();
  void compileSpecialBlocks();

  boolean collidedWithBoat();
  double frictionPosSubtraction();
  float frictionMultiplier();
  boolean receivedFlyingPacketIn(int ticks);

  Material collideMaterial();
  Material frictionMaterial();
  Material previousCollideMaterial();
  Material previousFrictionMaterial();
  boolean blockOnPositionSoulSpeedAffected();

  double fallDistance();
  void resetFallDistance();
  void addFallDistance(double fallDistance);

  boolean isInVehicle();
  void dismountRidingEntity(String boatSetback);

  default Entity vehicle() {
    return null;
  }

  default Simulator simulator() {
    return Simulators.PLAYER;
  }

  void setPushedByEntity(boolean pushedByEntity);
  boolean pushedByEntity();

  void setSimulationResult(SimulationResult result);
  SimulationResult simulationResult();

  int ticks(MoveMetric metric);
  int ticksPast(MoveMetric metric);

  default void tick(MoveMetric metric, boolean active) {
    if (active) {
      activeTick(metric);
    } else {
      inactiveTick(metric);
    }
  }

  void activeTick(MoveMetric metric);
  void inactiveTick(MoveMetric metric);

  default void activeTick(MoveMetric first, MoveMetric... others) {
    activeTick(first);
    for (MoveMetric other : others) {
      activeTick(other);
    }
  }

  default void inactiveTick(MoveMetric first, MoveMetric... others) {
    inactiveTick(first);
    for (MoveMetric other : others) {
      inactiveTick(other);
    }
  }

  @Deprecated
  int reduceTicks();

  @Deprecated
  boolean denyJump();

  void resetPhysicsPacketRelinkFlyVL();

  default int physicsPacketRelinkFlyVL() {
    return 0;
  }

  default void setPhysicsPacketRelinkFlyVL(int physicsPacketRelinkFlyVL) {
    throw new UnsupportedOperationException("setPhysicsPacketRelinkFlyVL is not supported for this SimulationEnvironment");
  }

  default boolean motionResetX() {
    return motionXReset();
  }

  default void setMotionResetX(boolean reset) {
    throw new UnsupportedOperationException("setMotionResetX is not supported for this SimulationEnvironment");
  }

  default boolean motionResetZ() {
    return motionZReset();
  }

  default void setMotionResetZ(boolean reset) {
    throw new UnsupportedOperationException("setMotionResetZ is not supported for this SimulationEnvironment");
  }

  default double baseMoveSpeed() {
    return 0.271;
  }

  default int fireworkRocketsPower() {
    return 1;
  }

  default int shulkerXToleranceRemaining() {
    return 0;
  }

  default int shulkerYToleranceRemaining() {
    return 0;
  }

  default int shulkerZToleranceRemaining() {
    return 0;
  }

  default int lowestShulkerY() {
    return Integer.MAX_VALUE;
  }

  default int highestShulkerY() {
    return Integer.MIN_VALUE;
  }

  default int pistonMotionToleranceRemaining() {
    return 0;
  }

  default double pistonVerticalAllowance() {
    return 0.0;
  }

  default double pistonHorizontalAllowance() {
    return 0.0;
  }

  default BoundingBox pistonCollisionArea() {
    return null;
  }

  default boolean physicsUnpredictableVelocityExpected() {
    return false;
  }

  default boolean enforceBoatStep() {
    return false;
  }

  default void setEnforceBoatStep(boolean enforceBoatStep) {
    throw new UnsupportedOperationException("setEnforceBoatStep is not supported for this SimulationEnvironment");
  }

  default boolean lastSneaking() {
    return false;
  }

  default boolean currentlyInBlock() {
    return false;
  }

  default int highestLocalRiptideLevel() {
    return 0;
  }

  default boolean onGroundWithRiptide() {
    return false;
  }

  void updateEyesInWater();
  void aquaticUpdateLavaReset();

  float height();
  float width();
  double heightRounded();
  double widthRounded();
  float eyeHeight();

  Fluid interactingFluid();

  void assumeOccurred(Simulation simulation);
  void tickComplete(boolean hasMovement, boolean hasRotation, boolean isRealClientTick);

  long currentTick();
  long activeSequence();
  void setActiveSequence(long activeSequence);

  List<TickAmbiguousUpdate> tickAmbiguousUpdates();

  default List<TickAmbiguousUpdate> currentlyPossibleTickAmbiguousUpdates() {
    List<TickAmbiguousUpdate> relevantActions = new ArrayList<>();
    for (TickAmbiguousUpdate contextAction : tickAmbiguousUpdates()) {
      CausalConstraint constraint = contextAction.constraint();
      if (constraint.currentlyPossible(this)) {
        relevantActions.add(contextAction);
      }
    }
    return relevantActions;
  }

  void setTreatThisFlyPacketAsMovePacket(boolean treatThisFlyPacketAsMovePacket);

  default boolean tryMoveReinterpretation(Simulation simulation, double flyingLimit) {
    if (!simulation.resultsInFlyingPacket(this, flyingLimit)) {
      return false;
    }
    reinterpretMovePacket(simulation);
    return true;
  }

  default void reinterpretMovePacket(Simulation simulation) {
    Position verifiedLastPosition = verifiedLastPosition();
    Rotation lastRotation = lastRotation();
    Position subversivePosition = verifiedLastPosition.add(simulation.offsetMotion());

    updateMovement(subversivePosition, null);
    setLastPosition(verifiedLastPosition);
    setLastRotation(lastRotation);
    activeTick(MoveMetric.FLYING_PACKET_ACCURATE);
    setTreatThisFlyPacketAsMovePacket(true);
  }

  default SimulationEnvironment immutableView() {
    return ImmutableSimulationEnvironmentView.of(this);
  }

  default SimulationEnvironment immutableCopy() {
    return ImmutableSimulationEnvironmentCopy.of(this);
  }

  default SimulationEnvironment mutableView() {
    return MutableSimulationEnvironmentView.of(this);
  }

  default void commitTo(SimulationEnvironment other) {
    throw new UnsupportedOperationException("commitTo is not supported for this SimulationEnvironment");
  }

  default int depth() {
    return 0;
  }

  static SimulationEnvironment invalid() {
    return null;
  }
}
