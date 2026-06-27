package de.jpx3.intave.check.movement.physics;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

final class MovementConfigurationTest {
  @Test
  void testKeys() {
    MovementConfiguration conf = MovementConfiguration.blank();
    conf = conf.withReduceTicks(1);
    conf = conf.withForward(1);
    conf = conf.withSprinting();

    assertEquals(1, conf.forward());
    assertEquals(0, conf.strafe());
    assertTrue(conf.isReducing());
    assertEquals(1, conf.reduceTicks());
    assertTrue(conf.isSprinting());

    conf = conf.withoutSprinting();
    assertFalse(conf.isSprinting());
  }

  @Test
  void testSprint() {
    for (MovementConfiguration value : MovementConfiguration.values()) {
      value = value.withSprinting();
      assertTrue(value.isSprinting());
    }
    for (MovementConfiguration value : MovementConfiguration.values()) {
      value = value.withoutSprinting();
      assertFalse(value.isSprinting());
    }
    for (MovementConfiguration value : MovementConfiguration.values()) {
      value = value.withForward(ThreadLocalRandom.current().nextInt(-1, 2));
      value = value.withStrafe(ThreadLocalRandom.current().nextInt(-1, 2));
      value = value.withReduceTicks(ThreadLocalRandom.current().nextInt(0, 2));
      value = value.withJump();

      value = value.withSprintingSetTo(true);
      assertTrue(value.isSprinting());

      value = value.withSprintingSetTo(false);
      assertFalse(value.isSprinting());
    }
    for (MovementConfiguration value : MovementConfiguration.values()) {
      value = value.withJump();
      assertTrue(value.isJumping());
    }
    for (MovementConfiguration value : MovementConfiguration.values()) {
      value = value.withoutJump();
      assertFalse(value.isJumping());
    }
    for (MovementConfiguration value : MovementConfiguration.values()) {
      value = value.withStrafe(1);
      assertEquals(1, value.strafe());
    }
    for (MovementConfiguration value : MovementConfiguration.values()) {
      value = value.withStrafe(-1);
      assertEquals(-1, value.strafe());
    }
    for (MovementConfiguration value : MovementConfiguration.values()) {
      value = value.withStrafe(0);
      assertEquals(0, value.strafe());
    }
    for (MovementConfiguration value : MovementConfiguration.values()) {
      value = value.withForward(1);
      assertEquals(1, value.forward());
    }
    for (MovementConfiguration value : MovementConfiguration.values()) {
      value = value.withForward(-1);
      assertEquals(-1, value.forward());
    }
  }

  @Test
  void testReduce() {
    for (MovementConfiguration value : MovementConfiguration.values()) {
      int randomTicks = ThreadLocalRandom.current().nextInt(0, 2);
      value = value.withReduceTicks(randomTicks);
      assertEquals(randomTicks, value.reduceTicks());
      value = value.withReduceTicks(0);
      assertEquals(0, value.reduceTicks());
    }

    MovementConfiguration configuration = MovementConfiguration.blank();
    configuration = configuration.withForward(1);
    configuration = configuration.withStrafe(1);
    configuration = configuration.withReduceTicks(0);
    configuration = configuration.withSprintingSetTo(false);
    configuration = configuration.withJumped(true);
    configuration = configuration.withHandActive(false);
  }
}
