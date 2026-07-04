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

package de.jpx3.intave.entity.size;

import de.jpx3.intave.reflect.access.ReflectiveHandleAccess;
import de.jpx3.intave.test.IntegrationTests;
import de.jpx3.intave.test.Severity;
import de.jpx3.intave.test.Test;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Sheep;

public final class EntitySizeTests extends IntegrationTests {
  public EntitySizeTests() {
    super("ES");
  }

  @Test(
    severity = Severity.ERROR
  )
  public void testSheep() {
    World world = Bukkit.getWorlds().get(0);
    // spawn sheep
    Sheep sheep = world.spawn(new Location(world, 0,0,0), Sheep.class);
    Object handle = ReflectiveHandleAccess.handleOf(sheep);
    // get size
    Class<?> entityClass = handle.getClass();
    HitboxSize size = HitboxSizeAccess.dimensionsOfNMSEntityClass(entityClass);
    sheep.remove();
    if (size == null || Math.abs(size.width() - 0.9) > 0.01 || Math.abs(size.height() - 1.3) > 0.01) {
      fail("Failed to fetch sheep size, is " + size);
    }
  }
}
