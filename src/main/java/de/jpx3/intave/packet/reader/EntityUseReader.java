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

package de.jpx3.intave.packet.reader;

import com.comphenix.protocol.wrappers.EnumWrappers;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.module.linker.packet.PacketId;

public final class EntityUseReader extends EntityReader {

  public boolean isAttackPacket() {
    return useAction() == EnumWrappers.EntityUseAction.ATTACK;
  }

  public boolean isSecondary() {
    return useAction() == EnumWrappers.EntityUseAction.INTERACT_AT;
  }

  public EnumWrappers.EntityUseAction useAction() {
    if (MinecraftVersions.VER26_1_1.atOrAbove()) {
      if (PacketId.Client.ATTACK_ENTITY.lookupName().equalsIgnoreCase(
        packet().getType().name()
      )) {
        return EnumWrappers.EntityUseAction.ATTACK;
      }
      boolean isSecondary = packet().getBooleans().read(0);
      return isSecondary ? EnumWrappers.EntityUseAction.INTERACT_AT : EnumWrappers.EntityUseAction.INTERACT;
    } else {
      EnumWrappers.EntityUseAction action = packet().getEntityUseActions().readSafely(0);
      if (action == null) {
        action = packet().getEnumEntityUseActions().read(0).getAction();
      }
      return action;
    }
  }
}
