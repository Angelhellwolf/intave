package de.jpx3.intave.check.movement.physics.environment;

import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.share.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class MutableSimulationEnvironmentViewTest {
  @Test
  void readThroughFollowsDelegateUntilValueIsOverridden() {
    TestSimulationEnvironment delegate = new TestSimulationEnvironment();
    delegate.setPositionX(1.0);
    delegate.setPositionY(2.0);
    delegate.setPositionZ(3.0);

    SimulationEnvironment view = delegate.mutableView();
    assertEquals(1.0, view.positionX(), 0.0);

    delegate.setPositionX(4.0);
    assertEquals(4.0, view.positionX(), 0.0);

    view.setBaseMotion(7.0, 8.0, 9.0);
    delegate.setBaseMotion(10.0, 11.0, 12.0);

    assertEquals(7.0, view.baseMotionX(), 0.0);
    assertEquals(10.0, delegate.baseMotionX(), 0.0);
    assertEquals(4.0, view.positionX(), 0.0);
  }

  @Test
  void updateMovementChangesViewWithoutChangingDelegate() {
    TestSimulationEnvironment delegate = new TestSimulationEnvironment();
    delegate.setPositionX(1.0);
    delegate.setPositionY(2.0);
    delegate.setPositionZ(3.0);
    delegate.copyPositionToVerifiedPosition();

    SimulationEnvironment view = delegate.mutableView();
    view.updateMovement(4.0, 6.0, 8.0, 90.0F, 45.0F, true, true);

    assertEquals(1.0, delegate.positionX(), 0.0);
    assertEquals(4.0, view.positionX(), 0.0);
    assertEquals(1.0, view.lastPositionX(), 0.0);
    assertEquals(3.0, view.motionX(), 0.0);
    assertEquals(90.0F, view.rotationYaw(), 0.0F);
  }

  @Test
  void updateMovementPreservesPreviousRotationAsLastRotation() {
    TestSimulationEnvironment delegate = new TestSimulationEnvironment();
    delegate.setRotation(10.0F, 20.0F);

    SimulationEnvironment view = delegate.mutableView();
    view.updateMovement(0.0, 0.0, 0.0, 90.0F, 45.0F, false, true);

    assertEquals(90.0F, view.rotationYaw(), 0.0F);
    assertEquals(45.0F, view.rotationPitch(), 0.0F);
    assertEquals(10.0F, view.lastRotationYaw(), 0.0F);
    assertEquals(20.0F, view.lastRotationPitch(), 0.0F);
  }

  @Test
  void directRotationOverrideDoesNotRewriteLastRotation() {
    TestSimulationEnvironment delegate = new TestSimulationEnvironment();
    delegate.setRotation(10.0F, 20.0F);

    SimulationEnvironment view = delegate.mutableView();
    view.setRotation(90.0F, 45.0F);

    assertEquals(90.0F, view.rotationYaw(), 0.0F);
    assertEquals(45.0F, view.rotationPitch(), 0.0F);
    assertEquals(10.0F, view.lastRotationYaw(), 0.0F);
    assertEquals(20.0F, view.lastRotationPitch(), 0.0F);
  }

  @Test
  void commitToAnotherEnvironment() {
    TestSimulationEnvironment delegate = new TestSimulationEnvironment();
    delegate.setPositionX(1.0);
    delegate.setPositionY(2.0);
    delegate.setPositionZ(3.0);
    delegate.copyPositionToVerifiedPosition();

    SimulationEnvironment view = delegate.mutableView();
    BoundingBox box = BoundingBox.fromBounds(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
    view.updateMovement(4.0, 6.0, 8.0, 90.0F, 45.0F, true, true);
    view.setVerifiedLastPosition(new Position(9.0, 10.0, 11.0), "test");
    view.setBoundingBox(box);
    view.setBaseMotion(0.1, 0.2, 0.3);
    view.setInWater(true);
    view.setPushedByEntity(true);

    TestSimulationEnvironment target = new TestSimulationEnvironment();
    view.commitTo(target);

    assertEquals(4.0, target.positionX(), 0.0);
    assertEquals(6.0, target.positionY(), 0.0);
    assertEquals(8.0, target.positionZ(), 0.0);
    assertEquals(1.0, target.lastPositionX(), 0.0);
    assertEquals(9.0, target.verifiedLastPositionX(), 0.0);
    assertEquals(0.1, target.baseMotionX(), 0.0);
    assertEquals(box, target.boundingBox());
    assertEquals(90.0F, target.rotationYaw(), 0.0F);
    assertEquals(45.0F, target.rotationPitch(), 0.0F);
    assertFalse(delegate.inWater());
  }
}
