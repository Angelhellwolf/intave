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

import de.jpx3.intave.block.cache.MockFullBlockStaticPlane;
import de.jpx3.intave.block.fluid.FluidFlow;
import de.jpx3.intave.block.fluid.Fluids;
import de.jpx3.intave.check.movement.physics.environment.TestSimulationEnvironment;
import de.jpx3.intave.player.collider.Colliders;
import de.jpx3.intave.player.collider.complex.Collider;
import de.jpx3.intave.player.collider.simple.SimpleCollider;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.test.*;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserFactory;
import de.jpx3.intave.user.UserRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.UUID;

import static org.bukkit.GameMode.SURVIVAL;

public final class SimulatorBasicTests extends IntegrationTests {
  private static final UUID EMPTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private User testUser;
  private Player player;
  private final Collider collider = Colliders.anyCollider();
  private final FluidFlow waterflow = Fluids.anyWaterflow();
  private final SimpleCollider simpleCollider = Colliders.anySimpleCollider();
  private final PlayerInventory inventory = new MockEmptyInventory();

  public SimulatorBasicTests() {
    super("SB");
  }

  @Before
  public void setupMovementTest() {
    player = FakePlayerFactory.createPlayer(
      (s, objects) -> {
        switch (s) {
          case "getInventory":
            return inventory;
          case "getWorld":
            return Bukkit.getWorlds().get(0);
          case "getUniqueId":
            return EMPTY_ID;
          case "getActivePotionEffects":
            return Collections.emptyList();
          case "isFlying":
          case "getAllowFlight":
          case "isSprinting":
          case "isSneaking":
            return false;
          case "getFallDistance":
            return 0.0f;
          case "getGameMode":
            return SURVIVAL;
          case "getFlySpeed":
          case "getWalkSpeed":
            return 0.2f;
        }
        return null;
      }
    );

    MockFullBlockStaticPlane plane = new MockFullBlockStaticPlane();
    plane.horizontalFill(1);
    testUser = UserFactory.createTestUserFor(player, (usr, s) -> {
      switch (s) {
        case "collider":
          return collider;
        case "waterflow":
          return waterflow;
        case "simplifiedCollider":
          return simpleCollider;
        case "blockCache":
          return plane;
        case "protocolVersion":
          return 47;
      }
      return null;
    });
    UserRepository.manuallyRegisterUser(player, testUser);
  }

  @Test(
    testCode = "A",
    severity = Severity.ERROR
  )
  public void simpleFallingTest() {
    // prepare data
    double[][] relativeMotion = new double[200][3];
    double[][] positions = new double[200][3];

    positions[0][1] = 250;

    for (int i = 1; i < relativeMotion.length; i++) {
      relativeMotion[i][0] = 0;
      relativeMotion[i][1] = (relativeMotion[i - 1][1] - 0.08) * 0.98;
      relativeMotion[i][2] = 0;
      positions[i][0] = positions[i - 1][0] + relativeMotion[i][0];
      positions[i][1] = positions[i - 1][1] + relativeMotion[i][1];
      positions[i][2] = positions[i - 1][2] + relativeMotion[i][2];
    }

    Simulator simulator = Simulators.PLAYER;
    MovementConfiguration configuration = MovementConfiguration.blank();
    TestSimulationEnvironment environment = new TestSimulationEnvironment();

    environment.setPositionY(250);
    environment.copyPositionToLastPosition();
    environment.copyPositionToVerifiedPosition();
    environment.setGravity(0.08f);
    environment.setFriction(0.09998f);
    environment.setAiMovementSpeed(0.1f);

    Motion afterTickMotion1 = simulator.simulateAfterTick(
      testUser,
      environment,
      environment.position(),
      Motion.newEmpty()
    );

    environment.setBaseMotion(afterTickMotion1);

    for (int i = 1; i < relativeMotion.length; i++) {
      double lastMotionX = relativeMotion[i - 1][0];
      double lastMotionY = relativeMotion[i - 1][1];
      double lastMotionZ = relativeMotion[i - 1][2];
      Motion lastMotion = new Motion(lastMotionX, lastMotionY, lastMotionZ);

      double motionX = relativeMotion[i][0];
      double motionY = relativeMotion[i][1];
      double motionZ = relativeMotion[i][2];
      Motion motion = new Motion(motionX, motionY, motionZ);

      environment.setMotionX(motionX);
      environment.setMotionY(motionY);
      environment.setMotionZ(motionZ);

      environment.setPositionX(environment.positionX() + motionX);
      environment.setPositionY(environment.positionY() + motionY);
      environment.setPositionZ(environment.positionZ() + motionZ);

      Simulation simulation = simulator.simulateTick(
        testUser,
        environment.mutableBaseMotionCopy(),
        environment.immutableView(),
        configuration
      );

      environment.assumeOccurred(simulation);

      double accuracy = simulation.offsetDifference();
      if (accuracy > 0.001 && environment.positionY() > 3) {
        System.out.println("#" + i + " (" + lastMotion + " -> " + simulation.offsetMotion() + ", but expected " + motion + ")");
        fail("Simulation accuracy deviation: " + accuracy);
      }

      Motion modifiableSimulationMotion = simulation.offsetMotion();
      Motion afterTickMotion = simulator.simulateAfterTick(
        testUser,
        environment,
        environment.position(),
        modifiableSimulationMotion
      );

      environment.setBaseMotion(afterTickMotion);
      environment.copyPositionToVerifiedPosition();
    }
  }

  @After
  public void tearDownMovementTest() {
    UserRepository.unregisterUser(player);
//    testWorld.erase();
  }
}

