package de.jpx3.intave.check.movement.physics.branch;

import de.jpx3.intave.user.meta.InventoryMetadata;
import de.jpx3.intave.user.meta.MovementMetadata;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import org.bukkit.Material;

import java.util.Set;

import static de.jpx3.intave.check.movement.physics.MoveMetric.ENTITY_USE;

final class UseItemBrancher extends MovementSearchBrancher {
  private static final boolean[] OPTIMISTIC = new boolean[]{true, false};
  private static final boolean[] PESSIMISTIC = new boolean[]{false, true};

  @Override
  public Set<MovementSearchConfig> branch(MovementSearchInput input, MovementSearchConfig config) {
    InventoryMetadata inventoryData = input.user().meta().inventory();
    ProtocolMetadata protocol = input.user().meta().protocol();
    UseItemStates useItemStates = useItemStates(input);
    Set<MovementSearchConfig> result = ordered();
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
    return result;
  }

  private UseItemStates useItemStates(MovementSearchInput input) {
    InventoryMetadata inventoryData = input.user().meta().inventory();
    MovementMetadata movementData = input.user().meta().movement();
    ProtocolMetadata protocol = input.user().meta().protocol();
    boolean usableItemInEitherHand = input.user().hasPlayer() && inventoryData.usableItemInEitherHand();
    boolean skipUseItem = (!protocol.sprintWhenHandActive() && movementData.sprinting && !protocol.viaVersionShieldBlockReplacement())
      || !usableItemInEitherHand;
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
