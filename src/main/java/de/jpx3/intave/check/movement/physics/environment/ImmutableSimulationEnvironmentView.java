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

import java.util.List;

public final class ImmutableSimulationEnvironmentView implements SimulationEnvironment {
	private final SimulationEnvironment delegate;

	public ImmutableSimulationEnvironmentView(
		SimulationEnvironment delegate
	) {
		this.delegate = delegate;
	}

	public static ImmutableSimulationEnvironmentView of(SimulationEnvironment delegate) {
		return new ImmutableSimulationEnvironmentView(delegate);
	}

	@Override
	public Pose pose() {
		return delegate.pose();
	}

	@Override
	public Vector lookVector() {
		return delegate.lookVector();
	}

	@Override
	public void updateMovement(
		double newPositionX, double newPositionY, double newPositionZ,
		float newRotationYaw, float newRotationPitch,
		boolean hasMovement, boolean hasRotation
	) {
		throw new UnsupportedOperationException("This environment view is unmodifiable");
	}

	@Override
	public void setRotation(float newRotationYaw, float newRotationPitch) {
		throw new UnsupportedOperationException("This environment view is unmodifiable");
	}

	@Override
	public Position position() {
		return delegate.position();
	}

	@Override
	public double positionX() {
		return delegate.positionX();
	}

	@Override
	public double positionY() {
		return delegate.positionY();
	}

	@Override
	public double positionZ() {
		return delegate.positionZ();
	}

	@Override
	public Position verifiedLastPosition() {
		return delegate.verifiedLastPosition();
	}

	@Override
	public double verifiedLastPositionX() {
		return delegate.verifiedLastPositionX();
	}

	@Override
	public double verifiedLastPositionY() {
		return delegate.verifiedLastPositionY();
	}

	@Override
	public double verifiedLastPositionZ() {
		return delegate.verifiedLastPositionZ();
	}

	@Override
	public void setVerifiedLastPosition(Position position, String reason) {
		throw new UnsupportedOperationException("This environment view is unmodifiable");
	}

	@Override
	public Position lastPosition() {
		return delegate.lastPosition();
	}

	@Override
	public double lastPositionX() {
		return delegate.lastPositionX();
	}

	@Override
	public double lastPositionY() {
		return delegate.lastPositionY();
	}

	@Override
	public double lastPositionZ() {
		return delegate.lastPositionZ();
	}

	@Override
	public float lastRotationYaw() {
		return delegate.lastRotationYaw();
	}

	@Override
	public float lastRotationPitch() {
		return delegate.lastRotationPitch();
	}

	@Override
	public void setLastRotation(float lastRotationYaw, float lastRotationPitch) {
		throw new UnsupportedOperationException("This environment view is unmodifiable");
	}

	@Override
	public void setLastPosition(double x, double y, double z) {
		throw new UnsupportedOperationException("This environment view is unmodifiable");
	}

	@Override
	public void setBoundingBox(BoundingBox boundingBox) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public BoundingBox boundingBox() {
		return delegate.boundingBox();
	}

	@Override
	public Motion sentOffsetMotion() {
		return delegate.sentOffsetMotion();
	}

	@Override
	public double motionX() {
		return delegate.motionX();
	}

	@Override
	public double motionY() {
		return delegate.motionY();
	}

	@Override
	public double motionZ() {
		return delegate.motionZ();
	}

	@Override
	public Motion mutableBaseMotionCopy() {
		return delegate.mutableBaseMotionCopy();
	}

	@Override
	public double baseMotionX() {
		return delegate.baseMotionX();
	}

	@Override
	public double baseMotionY() {
		return delegate.baseMotionY();
	}

	@Override
	public double baseMotionZ() {
		return delegate.baseMotionZ();
	}

	@Override
	public void setBaseMotion(Motion baseMotion) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public void setBaseMotion(double baseMotionX, double baseMotionY, double baseMotionZ) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public boolean motionXReset() {
		return delegate.motionXReset();
	}

	@Override
	public boolean motionZReset() {
		return delegate.motionZReset();
	}

	@Override
	public Vector motionMultiplier() {
		return delegate.motionMultiplier();
	}

	@Override
	public void resetMotionMultiplier() {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public WorldBorder worldBorder() {
		return delegate.worldBorder();
	}

	@Override
	public void setWorldBorder(@NotNull WorldBorder worldBorder) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public float rotationYaw() {
		return delegate.rotationYaw();
	}

	@Override
	public float yawSine() {
		return delegate.yawSine();
	}

	@Override
	public float yawCosine() {
		return delegate.yawCosine();
	}

	@Override
	public float rotationPitch() {
		return delegate.rotationPitch();
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
		return delegate.jumpMotion();
	}

	@Override
	public void setJumpMotion(double jumpMotion) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public boolean hasJumpedInTick() {
		return delegate.hasJumpedInTick();
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
		return delegate.inWater();
	}

	@Override
	public void setInWater(boolean inWater) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public boolean inLava() {
		return delegate.inLava();
	}

	@Override
	public boolean inWeb() {
		return delegate.inWeb();
	}

	@Override
	public void resetInWeb() {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public boolean onGround() {
		return delegate.onGround();
	}

	@Override
	public boolean lastOnGround() {
		return delegate.lastOnGround();
	}

	@Override
	public void setLastOnGround(boolean lastOnGround) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public boolean collidedHorizontally() {
		return delegate.collidedHorizontally();
	}

	@Override
	public boolean collidedVertically() {
		return delegate.collidedVertically();
	}

	@Override
	public void checkSupportingBlock(Motion motion) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public void clearSupportingBlock() {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public void compileSpecialBlocks() {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
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
		return delegate.fallDistance();
	}

	@Override
	public void resetFallDistance() {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public void addFallDistance(double fallDistance) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public boolean isInVehicle() {
		return delegate.isInVehicle();
	}

	@Override
	public void dismountRidingEntity(String boatSetback) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public void setPushedByEntity(boolean pushedByEntity) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public boolean pushedByEntity() {
		return delegate.pushedByEntity();
	}

	@Override
	public void setBeforeMoveColliderResult(SimulationResult result) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public SimulationResult beforeMoveColliderResult() {
		return delegate.beforeMoveColliderResult();
	}

	@Override
	public int ticks(MoveMetric metric) {
		return delegate.ticks(metric);
	}

	@Override
	public int ticksPast(MoveMetric metric) {
		return delegate.ticksPast(metric);
	}

	@Override
	public void activeTick(MoveMetric metric) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public void inactiveTick(MoveMetric metric) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
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
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public void updateEyesInWater() {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public void aquaticUpdateLavaReset() {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
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
		return delegate.interactingFluid();
	}

	@Override
	public void assumeOccurred(Simulation simulation) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public void tickComplete(boolean hasMovement, boolean hasRotation, boolean isRealClientTick) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public long currentTick() {
		return delegate.currentTick();
	}

	@Override
	public long activeSequence() {
		return delegate.activeSequence();
	}

	@Override
	public void setActiveSequence(long activeSequence) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public List<TickAmbiguousUpdate> tickAmbiguousUpdates() {
		return delegate.tickAmbiguousUpdates();
	}

	@Override
	public void setTreatThisFlyPacketAsMovePacket(boolean treatThisFlyPacketAsMovePacket) {
		throw new UnsupportedOperationException("Cannot modify unmodifiable view");
	}

	@Override
	public SimulationEnvironment mutableView() {
		return delegate.mutableView();
	}

	@Override
	public int depth() {
		return delegate.depth() + 1;
	}

	@Override
	public void commitTo(SimulationEnvironment other) {
		delegate.commitTo(other);
	}

	@Override
	public SimulationEnvironment immutableView() {
		return this;
	}
}
