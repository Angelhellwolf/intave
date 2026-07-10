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

package de.jpx3.intave.player.collider.complex;

import de.jpx3.intave.block.collision.Collision;
import de.jpx3.intave.block.shape.BlockShape;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.share.Direction;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.user.User;

final class v7Collider implements Collider {
  @Override
  public SimulationResult collide(User user, SimulationEnvironment environment, Motion offsetMotion, double positionX, double positionY, double positionZ, boolean inWeb) {
//    MovementMetadata movement = user.meta().movement();

    // ?
    // this.ySize *= 0.4F;

    double offsetMotionX = offsetMotion.motionX;
    double offsetMotionY = offsetMotion.motionY;
    double offsetMotionZ = offsetMotion.motionZ;

    double var13 = offsetMotion.motionX;
    double var15 = offsetMotion.motionY;
    double var17 = offsetMotion.motionZ;

    BoundingBox entityBoundingBox = environment.boundingBox();
    BoundingBox var19 = environment.boundingBox().copy();
    boolean sneakingOnGround = environment.onGround() && environment.isSneaking();

    if (inWeb) {
      offsetMotion.motionX *= 0.25D;
      offsetMotion.motionY *= 0.05f;
      offsetMotion.motionZ *= 0.25D;
    }
    Motion actualMotion = inWeb ? Motion.newEmpty() : offsetMotion.copy();

    BlockShape var37 = Collision.shape(user, environment, entityBoundingBox.expand(offsetMotionX, offsetMotionY, offsetMotionZ));
    offsetMotionY = var37.allowedOffset(Direction.Axis.Y_AXIS, entityBoundingBox, offsetMotionY);
    entityBoundingBox = entityBoundingBox.offset(0, offsetMotionY, 0);

    boolean var36 = environment.onGround() || var15 != offsetMotionY && var15 < 0.0D;
    offsetMotionX = var37.allowedOffset(Direction.Axis.X_AXIS, entityBoundingBox, offsetMotionX);
    entityBoundingBox.offset(offsetMotionX, 0, 0);

    offsetMotionZ = var37.allowedOffset(Direction.Axis.Z_AXIS, entityBoundingBox, offsetMotionZ);
    entityBoundingBox.offset(0.0D, 0.0D, offsetMotionZ);

    double var25;
    double var27;
    double var38;

    boolean step = false;
    double stepHeight = 0.0D;
    if (var36 && (sneakingOnGround /*|| this.ySize < 0.05F*/) && (var13 != offsetMotionX || var17 != offsetMotionZ)) {
      var38 = offsetMotionX;
      var25 = offsetMotionY;
      var27 = offsetMotionZ;
      offsetMotionX = var13;
      offsetMotionY = environment.stepHeight();
      offsetMotionZ = var17;

      BoundingBox var29 = entityBoundingBox.copy();
      entityBoundingBox = var19;

      var37 = Collision.shape(user, environment, entityBoundingBox.expand(var13, offsetMotionY, var17));
      offsetMotionY = var37.allowedOffset(Direction.Axis.Y_AXIS, entityBoundingBox, offsetMotionY);
      entityBoundingBox.offset(0, offsetMotionY, 0);

      offsetMotionX = var37.allowedOffset(Direction.Axis.X_AXIS, entityBoundingBox, offsetMotionX);
      entityBoundingBox.offset(offsetMotionX, 0, 0);

      offsetMotionZ = var37.allowedOffset(Direction.Axis.Z_AXIS, entityBoundingBox, offsetMotionZ);
      entityBoundingBox.offset(0, 0, offsetMotionZ);

      // where is the sneak limiter?

      if (var38 * var38 + var27 * var27 >= offsetMotionX * offsetMotionX + offsetMotionZ * offsetMotionZ) {
        offsetMotionX = var38;
        offsetMotionY = var25;
        offsetMotionZ = var27;
        entityBoundingBox = var29;
      } else {
        step = true;
        stepHeight = offsetMotionY;
      }
    }

    boolean collidedVertically = offsetMotionY != offsetMotion.motionY;
    boolean collidedHorizontally = offsetMotionX != offsetMotion.motionX || offsetMotionZ != offsetMotion.motionZ;
    boolean onGround = offsetMotionY != offsetMotion.motionY && offsetMotionY < 0.0;
    boolean moveResetX = offsetMotionX != offsetMotion.motionX;
    boolean moveResetZ = offsetMotionZ != offsetMotion.motionZ;
    double newPositionX = (entityBoundingBox.minX + entityBoundingBox.maxX) / 2.0D;
    double newPositionY = entityBoundingBox.minY;
    double newPositionZ = (entityBoundingBox.minZ + entityBoundingBox.maxZ) / 2.0D;
    offsetMotion.motionX = newPositionX - positionX;
    offsetMotion.motionY = newPositionY - positionY;
    offsetMotion.motionZ = newPositionZ - positionZ;
    return new SimulationResult(
      Motion.copyFrom(offsetMotion), // hm..
      Motion.copyFrom(offsetMotion),
      null,
      onGround,
      collidedHorizontally, collidedVertically, moveResetX, moveResetZ,
      step, false, stepHeight
    );
  }
}