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

package de.jpx3.intave.check.movement.physics.branch;

import de.jpx3.intave.user.meta.InventoryMetadata;
import de.jpx3.intave.user.meta.MovementMetadata;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import org.bukkit.Material;

import java.util.List;

import static de.jpx3.intave.check.movement.physics.MoveMetric.ENTITY_USE;

final class UseItemBrancher extends MovementSearchBrancher {
  private static final boolean[] OPTIMISTIC = new boolean[]{true, false};
  private static final boolean[] PESSIMISTIC = new boolean[]{false, true};

  @Override
  public void branch(MovementSearchInput input, MovementSearchConfig config, List<MovementSearchConfig> result) {
    InventoryMetadata inventoryData = input.user().meta().inventory();
    ProtocolMetadata protocol = input.user().meta().protocol();
    UseItemStates useItemStates = useItemStates(input);
    for (boolean useItemState : inventoryData.handActive() ? OPTIMISTIC : PESSIMISTIC) {
      if (useItemStates.skip && useItemState) {
        continue;
      }
      if (useItemStates.require && !useItemState) {
        continue;
      }
      if (config.moveConfig().isSprinting() && useItemState && !protocol.combatUpdate()) {
        continue;
      }
      result.add(config.withHandActive(useItemState));
    }
  }

  private UseItemStates useItemStates(MovementSearchInput input) {
    InventoryMetadata inventoryData = input.user().meta().inventory();
    MovementMetadata movementData = input.user().meta().movement();
    ProtocolMetadata protocol = input.user().meta().protocol();
    boolean hasUsableItem = inventoryData.usableItemInEitherHandOrHotbar();
    if (!hasUsableItem) {
      return new UseItemStates(true, false);
    }
    boolean skipUseItem = !protocol.sprintWhenHandActive() && movementData.sprinting && !protocol.viaVersionShieldBlockReplacement();
    boolean requireUseItem = !protocol.combatUpdate()
      && inventoryData.handActive()
      && inventoryData.pastHotBarSlotChange > 20
      && (inventoryData.heldItem() == null || inventoryData.heldItem().getType() != Material.BOW);

    if (requireUseItem && movementData.ticksPast(ENTITY_USE) <= inventoryData.handActiveTicks) {
      requireUseItem = false;
    }

    if (requireUseItem || input.user().sizeOf(movementData.pose()).height() <= 1) {
      skipUseItem = false;
    }

    if ((requireUseItem || skipUseItem) && input.user().hasPlayer() && inventoryData.couldChargeCrossbow()) {
      requireUseItem = false;
      skipUseItem = false;
    }

    if (!input.detectNoSlowdown()) {
      skipUseItem = false;
      requireUseItem = false;
    }
    return new UseItemStates(skipUseItem, requireUseItem);
  }

  private static final class UseItemStates {
    private final boolean skip;
    private final boolean require;

    private UseItemStates(boolean skip, boolean require) {
      this.skip = skip;
      this.require = require;
    }
  }
}
