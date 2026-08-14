package de.jpx3.intave.trustfactor;

import de.jpx3.intave.access.player.trust.TrustFactor;
import de.jpx3.intave.access.player.trust.TrustFactorResolver;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

final class EmptyTrustFactorResolver implements TrustFactorResolver {
  @Override
  public void resolve(Player player, Consumer<TrustFactor> callback) {

  }

  @Override
  public String toString() {
    return "空解析器";
  }
}
