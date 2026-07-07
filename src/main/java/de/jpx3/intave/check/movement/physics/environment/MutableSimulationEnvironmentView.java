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

import de.jpx3.intave.block.fluid.Fluid;
import de.jpx3.intave.check.movement.physics.MoveMetric;
import de.jpx3.intave.check.movement.physics.MovementConfiguration;
import de.jpx3.intave.check.movement.physics.Pose;
import de.jpx3.intave.check.movement.physics.Simulation;
import de.jpx3.intave.check.movement.physics.update.TickAmbiguousUpdate;
import de.jpx3.intave.player.collider.complex.SimulationResult;
import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.world.border.WorldBorder;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static de.jpx3.intave.check.movement.physics.MoveMetric.*;
import static de.jpx3.intave.share.ClientMath.cos;
import static de.jpx3.intave.share.ClientMath.sin;

public final class MutableSimulationEnvironmentView implements SimulationEnvironment {
  private final SimulationEnvironment delegate;
  private final List<EnvironmentMutation> deferredMutations = new ArrayList<>();

  private boolean positionOverridden;
  private double positionX, positionY, positionZ;
  private boolean verifiedLastPositionOverridden;
  private double verifiedLastPositionX, verifiedLastPositionY, verifiedLastPositionZ;
  private String verifiedLastPositionReason;
  private boolean lastPositionOverridden;
  private double lastPositionX, lastPositionY, lastPositionZ;
  private boolean motionOverridden;
  private double motionX, motionY, motionZ;
  private boolean baseMotionOverridden;
  private double baseMotionX, baseMotionY, baseMotionZ;
  private boolean motionXResetOverridden, motionXReset;
  private boolean motionZResetOverridden, motionZReset;
  private boolean motionMultiplierOverridden;
  private Vector motionMultiplier;
  private boolean rotationOverridden;
  private float rotationYaw, yawSine, yawCosine, rotationPitch;
  private boolean lastRotationOverridden;
  private float lastRotationYaw, lastRotationPitch;
  private Vector lookVector;
  private boolean boundingBoxOverridden;
  private BoundingBox boundingBox;
  private boolean jumpMotionOverridden;
  private double jumpMotion;
  private boolean hasJumpedInTickOverridden;
  private boolean hasJumpedInTick;
  private boolean inWaterOverridden, inWater;
  private boolean inLavaOverridden, inLava;
  private boolean inWebOverridden, inWeb;
  private boolean onGroundOverridden, onGround;
  private boolean lastOnGroundOverridden, lastOnGround;
  private boolean collidedHorizontallyOverridden, collidedHorizontally;
  private boolean collidedVerticallyOverridden, collidedVertically;
  private boolean fallDistanceOverridden;
  private double fallDistance;
  private boolean pushedByEntityOverridden, pushedByEntity;
  private boolean simulationResultOverriden;
  private SimulationResult simulationResult;
  private boolean interactingFluidOverridden;
  private Fluid interactingFluid;
  private boolean inVehicleOverridden, inVehicle;
  private WorldBorder worldBorder;
  private boolean worldBorderOverridden;

  private long currentTick = 0;
  private boolean currentTickOverridden;
  private long activeSequence = 0;
  private boolean activeSequenceOverridden;

  private final EnumMap<MoveMetric, Integer> activeTrackerOverrides = new EnumMap<>(MoveMetric.class);
  private final EnumMap<MoveMetric, Integer> pastTrackerOverrides = new EnumMap<>(MoveMetric.class);

  private MutableSimulationEnvironmentView(SimulationEnvironment delegate) {
    this.delegate = delegate;
  }

  public static MutableSimulationEnvironmentView of(SimulationEnvironment delegate) {
    return new MutableSimulationEnvironmentView(delegate);
  }

  @Override
  public void commitTo(SimulationEnvironment other) {
    if (other == this) {
      return;
    }
    if (delegate != other && delegate.depth() > 0) {
      delegate.commitTo(other);
    }
    for (EnvironmentMutation deferredMutation : deferredMutations) {
      deferredMutation.apply(other);
    }
    if (verifiedLastPositionOverridden) {
      other.setVerifiedLastPosition(
        new Position(verifiedLastPositionX, verifiedLastPositionY, verifiedLastPositionZ),
        verifiedLastPositionReason
      );
    }
    if (lastPositionOverridden) {
      other.setLastPosition(lastPositionX, lastPositionY, lastPositionZ);
    }
    if (lastOnGroundOverridden) {
      other.setLastOnGround(lastOnGround);
    }
    if (boundingBoxOverridden) {
      other.setBoundingBox(boundingBox);
    }
    if (baseMotionOverridden) {
      other.setBaseMotion(baseMotionX, baseMotionY, baseMotionZ);
    }
    if (motionMultiplierOverridden) {
      other.resetMotionMultiplier();
    }
    if (jumpMotionOverridden) {
      other.setJumpMotion(jumpMotion);
    }
    if (inWaterOverridden) {
      other.setInWater(inWater);
    }
    if (inLavaOverridden && !inLava) {
      other.aquaticUpdateLavaReset();
    }
    if (inWebOverridden && !inWeb) {
      other.resetInWeb();
    }
    if (fallDistanceOverridden && fallDistance == 0.0) {
      other.resetFallDistance();
    }
    if (pushedByEntityOverridden) {
      other.setPushedByEntity(pushedByEntity);
    }
    if (simulationResultOverriden) {
      other.setSimulationResult(simulationResult);
    }
  }

  @Override
  public Pose pose() {
    return delegate.pose();
  }

  @Override
  public Vector lookVector() {
    Vector vector = rotationOverridden ? lookVector : delegate.lookVector();
    return vector == null ? null : vector.clone();
  }

  @Override
  public void updateMovement(
    double newPositionX, double newPositionY, double newPositionZ,
    float newRotationYaw, float newRotationPitch,
    boolean hasMovement, boolean hasRotation
  ) {
    setLastPositionOverride(positionX(), positionY(), positionZ());
    if (hasMovement) {
      setPositionOverride(newPositionX, newPositionY, newPositionZ);
    }
    setLastRotationOverride(rotationYaw(), rotationPitch());
    if (hasRotation) {
      setRotationOverride(newRotationYaw, newRotationPitch);
    }
    if (hasMovement || hasRotation) {
      setMotionOverride(
        positionX() - verifiedLastPositionX(),
        positionY() - verifiedLastPositionY(),
        positionZ() - verifiedLastPositionZ()
      );
    }
    deferredMutations.add(environment -> environment.updateMovement(
      newPositionX, newPositionY, newPositionZ,
      newRotationYaw, newRotationPitch,
      hasMovement, hasRotation
    ));
  }

  @Override
  public void setRotation(float newRotationYaw, float newRotationPitch) {
    setRotationOverride(newRotationYaw, newRotationPitch);
    deferredMutations.add(environment -> environment.setRotation(newRotationYaw, newRotationPitch));
  }

  @Override
  public double positionX() {
    return positionOverridden ? positionX : delegate.positionX();
  }

  @Override
  public double positionY() {
    return positionOverridden ? positionY : delegate.positionY();
  }

  @Override
  public double positionZ() {
    return positionOverridden ? positionZ : delegate.positionZ();
  }

  @Override
  public double verifiedLastPositionX() {
    return verifiedLastPositionOverridden ? verifiedLastPositionX : delegate.verifiedLastPositionX();
  }

  @Override
  public double verifiedLastPositionY() {
    return verifiedLastPositionOverridden ? verifiedLastPositionY : delegate.verifiedLastPositionY();
  }

  @Override
  public double verifiedLastPositionZ() {
    return verifiedLastPositionOverridden ? verifiedLastPositionZ : delegate.verifiedLastPositionZ();
  }

  @Override
  public void setVerifiedLastPosition(Position position, String reason) {
    verifiedLastPositionOverridden = true;
    verifiedLastPositionX = position.getX();
    verifiedLastPositionY = position.getY();
    verifiedLastPositionZ = position.getZ();
    verifiedLastPositionReason = reason;
    deferredMutations.add(environment -> environment.setVerifiedLastPosition(position, reason));
  }

  @Override
  public double lastPositionX() {
    return lastPositionOverridden ? lastPositionX : delegate.lastPositionX();
  }

  @Override
  public double lastPositionY() {
    return lastPositionOverridden ? lastPositionY : delegate.lastPositionY();
  }

  @Override
  public double lastPositionZ() {
    return lastPositionOverridden ? lastPositionZ : delegate.lastPositionZ();
  }

  @Override
  public float lastRotationYaw() {
    return lastRotationOverridden ? lastRotationYaw : delegate.lastRotationYaw();
  }

  @Override
  public float lastRotationPitch() {
    return lastRotationOverridden ? lastRotationPitch : delegate.lastRotationPitch();
  }

  @Override
  public void setLastRotation(float lastRotationYaw, float lastRotationPitch) {
    setLastRotationOverride(lastRotationYaw, lastRotationPitch);
    deferredMutations.add(environment -> environment.setLastRotation(lastRotationYaw, lastRotationPitch));
  }

  @Override
  public void setLastPosition(double x, double y, double z) {
    setLastPositionOverride(x, y, z);
    deferredMutations.add(environment -> environment.setLastPosition(x, y, z));
  }

  @Override
  public void setBoundingBox(BoundingBox boundingBox) {
    boundingBoxOverridden = true;
    this.boundingBox = boundingBox;
    deferredMutations.add(environment -> environment.setBoundingBox(boundingBox));
  }

  @Override
  public BoundingBox boundingBox() {
    return boundingBoxOverridden ? boundingBox : delegate.boundingBox();
  }

  @Override
  public double motionX() {
    return motionOverridden ? motionX : delegate.motionX();
  }

  @Override
  public double motionY() {
    return motionOverridden ? motionY : delegate.motionY();
  }

  @Override
  public double motionZ() {
    return motionOverridden ? motionZ : delegate.motionZ();
  }

  @Override
  public double baseMotionX() {
    return baseMotionOverridden ? baseMotionX : delegate.baseMotionX();
  }

  @Override
  public double baseMotionY() {
    return baseMotionOverridden ? baseMotionY : delegate.baseMotionY();
  }

  @Override
  public double baseMotionZ() {
    return baseMotionOverridden ? baseMotionZ : delegate.baseMotionZ();
  }

  @Override
  public void setBaseMotion(double baseMotionX, double baseMotionY, double baseMotionZ) {
    baseMotionOverridden = true;
    this.baseMotionX = baseMotionX;
    this.baseMotionY = baseMotionY;
    this.baseMotionZ = baseMotionZ;
    deferredMutations.add(environment -> environment.setBaseMotion(baseMotionX, baseMotionY, baseMotionZ));
  }

  @Override
  public boolean motionXReset() {
    return motionXResetOverridden ? motionXReset : delegate.motionXReset();
  }

  @Override
  public boolean motionZReset() {
    return motionZResetOverridden ? motionZReset : delegate.motionZReset();
  }

  @Override
  public Vector motionMultiplier() {
    Vector vector = motionMultiplierOverridden ? motionMultiplier : delegate.motionMultiplier();
    return vector == null ? null : vector.clone();
  }

  @Override
  public void resetMotionMultiplier() {
    motionMultiplierOverridden = true;
    motionMultiplier = null;
    deferredMutations.add(SimulationEnvironment::resetMotionMultiplier);
  }

  @Override
  public WorldBorder worldBorder() {
    return worldBorderOverridden ? worldBorder : delegate.worldBorder();
  }

  @Override
  public void setWorldBorder(@NotNull WorldBorder worldBorder) {
    worldBorderOverridden = true;
    this.worldBorder = worldBorder;
    deferredMutations.add(environment -> environment.setWorldBorder(worldBorder));
  }

  @Override
  public float rotationYaw() {
    return rotationOverridden ? rotationYaw : delegate.rotationYaw();
  }

  @Override
  public float yawSine() {
    return rotationOverridden ? yawSine : delegate.yawSine();
  }

  @Override
  public float yawCosine() {
    return rotationOverridden ? yawCosine : delegate.yawCosine();
  }

  @Override
  public float rotationPitch() {
    return rotationOverridden ? rotationPitch : delegate.rotationPitch();
  }

  @Override
  public float aiMoveSpeed(boolean sprinting) {
    return delegate.aiMoveSpeed(sprinting);
  }

  @Override
  public float friction(boolean sprinting) {
    return delegate.friction(sprinting);
  }

  @Override
  public double stepHeight() {
    return delegate.stepHeight();
  }

  @Override
  public double resetMotion() {
    return delegate.resetMotion();
  }

  @Override
  public double jumpMotion() {
    return jumpMotionOverridden ? jumpMotion : delegate.jumpMotion();
  }

  @Override
  public void setJumpMotion(double jumpMotion) {
    jumpMotionOverridden = true;
    this.jumpMotion = jumpMotion;
    deferredMutations.add(environment -> environment.setJumpMotion(jumpMotion));
  }

  @Override
  public boolean hasJumpedInTick() {
    return hasJumpedInTickOverridden ? hasJumpedInTick : delegate.hasJumpedInTick();
  }

  @Override
  public double gravity() {
    return delegate.gravity();
  }

  @Override
  public float blockSpeedFactor() {
    return delegate.blockSpeedFactor();
  }

  @Override
  public float jumpMovementFactor() {
    return delegate.jumpMovementFactor();
  }

  @Override
  public boolean isSneaking() {
    return delegate.isSneaking();
  }

  @Override
  public boolean isSprinting() {
    return delegate.isSprinting();
  }

  @Override
  public boolean hasSprintSpeed() {
    return delegate.hasSprintSpeed();
  }

  @Override
  public boolean sprintingAllowed() {
    return delegate.sprintingAllowed();
  }

  @Override
  public boolean inWater() {
    return inWaterOverridden ? inWater : delegate.inWater();
  }

  @Override
  public void setInWater(boolean inWater) {
    inWaterOverridden = true;
    this.inWater = inWater;
    if (inWater) {
      fallDistanceOverridden = true;
      fallDistance = 0.0;
    }
    deferredMutations.add(environment -> environment.setInWater(inWater));
  }

  @Override
  public boolean inLava() {
    return inLavaOverridden ? inLava : delegate.inLava();
  }

  @Override
  public boolean inWeb() {
    return inWebOverridden ? inWeb : delegate.inWeb();
  }

  @Override
  public void resetInWeb() {
    inWebOverridden = true;
    inWeb = false;
    deferredMutations.add(SimulationEnvironment::resetInWeb);
  }

  @Override
  public boolean onGround() {
    return onGroundOverridden ? onGround : delegate.onGround();
  }

  @Override
  public boolean lastOnGround() {
    return lastOnGroundOverridden ? lastOnGround : delegate.lastOnGround();
  }

  @Override
  public void setLastOnGround(boolean lastOnGround) {
    lastOnGroundOverridden = true;
    this.lastOnGround = lastOnGround;
  }

  @Override
  public boolean collidedHorizontally() {
    return collidedHorizontallyOverridden ? collidedHorizontally : delegate.collidedHorizontally();
  }

  @Override
  public boolean collidedVertically() {
    return collidedVerticallyOverridden ? collidedVertically : delegate.collidedVertically();
  }

  @Override
  public void checkSupportingBlock(Motion motion) {
    deferredMutations.add(environment -> environment.checkSupportingBlock(motion));
  }

  @Override
  public void clearSupportingBlock() {
    deferredMutations.add(SimulationEnvironment::clearSupportingBlock);
  }

  @Override
  public void compileSpecialBlocks() {
    deferredMutations.add(SimulationEnvironment::compileSpecialBlocks);
  }

  @Override
  public boolean collidedWithBoat() {
    return delegate.collidedWithBoat();
  }

  @Override
  public double frictionPosSubtraction() {
    return delegate.frictionPosSubtraction();
  }

  @Override
  public float frictionMultiplier() {
    return delegate.frictionMultiplier();
  }

  @Override
  public boolean receivedFlyingPacketIn(int ticks) {
    if (hasMetricOverride(FLYING_PACKET_ACCURATE) || hasMetricOverride(FLYING_PACKET_CLIENT)) {
      return ticksPast(FLYING_PACKET_ACCURATE) <= ticks || ticksPast(FLYING_PACKET_CLIENT) <= ticks;
    }
    return delegate.receivedFlyingPacketIn(ticks);
  }

  @Override
  public Material collideMaterial() {
    return delegate.collideMaterial();
  }

  @Override
  public Material frictionMaterial() {
    return delegate.frictionMaterial();
  }

  @Override
  public Material previousCollideMaterial() {
    return delegate.previousCollideMaterial();
  }

  @Override
  public Material previousFrictionMaterial() {
    return delegate.previousFrictionMaterial();
  }

  @Override
  public boolean blockOnPositionSoulSpeedAffected() {
    return delegate.blockOnPositionSoulSpeedAffected();
  }

  @Override
  public double fallDistance() {
    return fallDistanceOverridden ? fallDistance : delegate.fallDistance();
  }

  @Override
  public void resetFallDistance() {
    fallDistanceOverridden = true;
    fallDistance = 0.0;
    deferredMutations.add(SimulationEnvironment::resetFallDistance);
  }

  @Override
  public void addFallDistance(double fallDistance) {
    double currentFallDistance = fallDistance();
    fallDistanceOverridden = true;
    this.fallDistance = currentFallDistance + fallDistance;
    deferredMutations.add(environment -> environment.addFallDistance(fallDistance));
  }

  @Override
  public boolean isInVehicle() {
    return inVehicleOverridden ? inVehicle : delegate.isInVehicle();
  }

  @Override
  public void dismountRidingEntity(String boatSetback) {
    inVehicleOverridden = true;
    inVehicle = false;
    deferredMutations.add(environment -> environment.dismountRidingEntity(boatSetback));
  }

  @Override
  public void setPushedByEntity(boolean pushedByEntity) {
    pushedByEntityOverridden = true;
    this.pushedByEntity = pushedByEntity;
    deferredMutations.add(environment -> environment.setPushedByEntity(pushedByEntity));
  }

  @Override
  public boolean pushedByEntity() {
    return pushedByEntityOverridden ? pushedByEntity : delegate.pushedByEntity();
  }

  @Override
  public void setSimulationResult(SimulationResult result) {
    simulationResultOverriden = true;
    simulationResult = result;
    deferredMutations.add(environment -> environment.setSimulationResult(result));
  }

  @Override
  public SimulationResult simulationResult() {
    return simulationResultOverriden ? simulationResult : delegate.simulationResult();
  }

  @Override
  public int ticks(MoveMetric metric) {
    return activeTrackerOverrides.getOrDefault(metric, delegate.ticks(metric));
  }

  @Override
  public int ticksPast(MoveMetric metric) {
    return pastTrackerOverrides.getOrDefault(metric, delegate.ticksPast(metric));
  }

  @Override
  public void activeTick(MoveMetric metric) {
    activeTickOverride(metric);
    deferredMutations.add(environment -> environment.activeTick(metric));
  }

  @Override
  public void inactiveTick(MoveMetric metric) {
    inactiveTickOverride(metric);
    deferredMutations.add(environment -> environment.inactiveTick(metric));
  }

  @Override
  public int reduceTicks() {
    return delegate.reduceTicks();
  }

  @Override
  public boolean denyJump() {
    return delegate.denyJump();
  }

  @Override
  public void resetPhysicsPacketRelinkFlyVL() {
    deferredMutations.add(SimulationEnvironment::resetPhysicsPacketRelinkFlyVL);
  }

  @Override
  public void updateEyesInWater() {
    interactingFluidOverridden = true;
    interactingFluid = null;
    deferredMutations.add(SimulationEnvironment::updateEyesInWater);
  }

  @Override
  public void aquaticUpdateLavaReset() {
    inLavaOverridden = true;
    inLava = false;
    deferredMutations.add(SimulationEnvironment::aquaticUpdateLavaReset);
  }

  @Override
  public float height() {
    return delegate.height();
  }

  @Override
  public float width() {
    return delegate.width();
  }

  @Override
  public double heightRounded() {
    return delegate.heightRounded();
  }

  @Override
  public double widthRounded() {
    return delegate.widthRounded();
  }

  @Override
  public float eyeHeight() {
    return delegate.eyeHeight();
  }

  @Override
  public Fluid interactingFluid() {
    return interactingFluidOverridden ? interactingFluid : delegate.interactingFluid();
  }

  @Override
  public void assumeOccurred(Simulation simulation) {
    applySimulation(simulation);
    deferredMutations.add(environment -> environment.assumeOccurred(simulation));
  }

  @Override
  public void tickComplete(boolean hasMovement, boolean hasRotation, boolean isRealClientTick) {
    activeTickOverride(ALIVE);
    tickOverride(IN_WEB, inWeb());
    tickOverride(IN_WATER, inWater());
    tickOverride(SNEAKING, isSneaking());
    tickOverride(SPRINTING, isSprinting());
    tickOverride(TELEPORT, false);
    inactiveTickOverride(
      STEP,
      IN_LAVA,
      VELOCITY,
      EDGE_SNEAKING,
      RECEIVED_VELOCITY_PACKET
    );
    currentTick = currentTick() + 1;
    currentTickOverridden = true;
    inactiveTickOverride(INVENTORY_OPEN);
    if (hasMovement || hasRotation) {
      inactiveTickOverride(MoveMetric.EXTERNAL_VELOCITY);
    }
    deferredMutations.add(environment -> environment.tickComplete(hasMovement, hasRotation, true));
  }

  @Override
  public long currentTick() {
    if (currentTickOverridden) {
      return currentTick;
    }
    return delegate.currentTick();
  }

  @Override
  public long activeSequence() {
    if (activeSequenceOverridden) {
      return activeSequence;
    }
    return delegate.activeSequence();
  }

  @Override
  public void setActiveSequence(long activeSequence) {
    activeSequenceOverridden = true;
    this.activeSequence = activeSequence;
    deferredMutations.add(environment -> environment.setActiveSequence(activeSequence));
  }

  @Override
  public List<TickAmbiguousUpdate> tickAmbiguousUpdates() {
    return delegate.tickAmbiguousUpdates();
  }

  @Override
  public void setTreatThisFlyPacketAsMovePacket(boolean treatThisFlyPacketAsMovePacket) {
    deferredMutations.add(environment ->
      environment.setTreatThisFlyPacketAsMovePacket(treatThisFlyPacketAsMovePacket)
    );
  }

  private final SimulationEnvironment unmodifiableView =
    ImmutableSimulationEnvironmentView.of(this);

  @Override
  public SimulationEnvironment immutableView() {
    return unmodifiableView;
  }

  @Override
  public int depth() {
    return delegate.depth() + 1;
  }

  private void applySimulation(Simulation simulation) {
    MovementConfiguration configuration = simulation.configuration();
    SimulationResult collider = simulation.result();
    onGroundOverridden = true;
    onGround = collider.onGround();
    collidedHorizontallyOverridden = true;
    collidedHorizontally = collider.collidedHorizontally();
    collidedVerticallyOverridden = true;
    collidedVertically = collider.collidedVertically();
    motionXResetOverridden = true;
    motionXReset = collider.resetMotionX();
    motionZResetOverridden = true;
    motionZReset = collider.resetMotionZ();
    hasJumpedInTickOverridden = true;
    hasJumpedInTick = configuration.isJumping();

    if (collider.step()) {
      activeTickOverride(STEP);
    }
    if (collider.edgeSneak()) {
      activeTickOverride(EDGE_SNEAKING);
    }
    setBeforeMoveColliderResultOverride(collider);
  }

  private void tickOverride(MoveMetric metric, boolean active) {
    if (active) {
      activeTickOverride(metric);
    } else {
      inactiveTickOverride(metric);
    }
  }

  private void activeTickOverride(MoveMetric metric) {
    activeTrackerOverrides.put(metric, ticks(metric) + 1);
    pastTrackerOverrides.put(metric, 0);
  }

  private void inactiveTickOverride(MoveMetric metric) {
    activeTrackerOverrides.put(metric, 0);
    pastTrackerOverrides.put(metric, ticksPast(metric) + 1);
  }

  private void inactiveTickOverride(MoveMetric first, MoveMetric... others) {
    inactiveTickOverride(first);
    for (MoveMetric other : others) {
      inactiveTickOverride(other);
    }
  }

  private boolean hasMetricOverride(MoveMetric metric) {
    return activeTrackerOverrides.containsKey(metric) || pastTrackerOverrides.containsKey(metric);
  }

  private void setPositionOverride(double positionX, double positionY, double positionZ) {
    positionOverridden = true;
    this.positionX = positionX;
    this.positionY = positionY;
    this.positionZ = positionZ;
  }

  private void setLastPositionOverride(double lastPositionX, double lastPositionY, double lastPositionZ) {
    lastPositionOverridden = true;
    this.lastPositionX = lastPositionX;
    this.lastPositionY = lastPositionY;
    this.lastPositionZ = lastPositionZ;
  }

  private void setMotionOverride(double motionX, double motionY, double motionZ) {
    motionOverridden = true;
    this.motionX = motionX;
    this.motionY = motionY;
    this.motionZ = motionZ;
  }

  private void setRotationOverride(float rotationYaw, float rotationPitch) {
    rotationOverridden = true;
    this.rotationYaw = rotationYaw;
    this.rotationPitch = rotationPitch;
    float rotationYawInRadians = rotationYaw * (float) Math.PI / 180.0F;
    yawSine = sin(rotationYawInRadians);
    yawCosine = cos(rotationYawInRadians);
    lookVector = vectorForRotation(rotationYaw, rotationPitch);
  }

  private void setLastRotationOverride(float lastRotationYaw, float lastRotationPitch) {
    lastRotationOverridden = true;
    this.lastRotationYaw = lastRotationYaw;
    this.lastRotationPitch = lastRotationPitch;
  }

  private void setBeforeMoveColliderResultOverride(SimulationResult result) {
    simulationResultOverriden = true;
    simulationResult = result;
  }

  private Vector vectorForRotation(float yaw, float pitch) {
    float f = pitch * ((float) Math.PI / 180F);
    float f1 = -yaw * ((float) Math.PI / 180F);
    float f2 = cos(f1);
    float f3 = sin(f1);
    float f4 = cos(f);
    float f5 = sin(f);
    return new Vector(f3 * f4, -f5, (double) (f2 * f4));
  }

  private interface EnvironmentMutation {
    void apply(SimulationEnvironment environment);
  }
}
