package de.jpx3.intave.module.player;

import de.jpx3.intave.IntaveControl;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.adapter.ViaVersionAdapter;
import de.jpx3.intave.cleanup.GarbageCollector;
import de.jpx3.intave.executor.Synchronizer;
import de.jpx3.intave.klass.trace.Caller;
import de.jpx3.intave.klass.trace.PluginInvocation;
import de.jpx3.intave.module.Module;
import de.jpx3.intave.module.linker.bukkit.BukkitEventSubscription;
import de.jpx3.intave.player.ItemProperties;
import de.jpx3.intave.user.MessageChannel;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.InventoryMetadata;
import de.jpx3.intave.user.permission.BukkitPermissionCheck;
import de.jpx3.intave.version.DurationTranslator;
import de.jpx3.intave.version.IntaveVersion;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;

import static de.jpx3.intave.IntaveControl.DISALLOW_ALL_BLOCK_PLACEMENTS_WITH_EVENT;
import static org.bukkit.event.EventPriority.MONITOR;

public final class MiscBukkitEvents extends Module {
  @BukkitEventSubscription
  public void on(PlayerJoinEvent join) {
    Player player = join.getPlayer();
    boolean hasNotificationPermission = BukkitPermissionCheck.permissionCheck(player, "intave.command") && !BukkitPermissionCheck.permissionCheck(player, "intave.command.noupdate");
    if (!hasNotificationPermission) {
      return;
    }
    String currentVersion = IntavePlugin.fullVersion();
    IntaveVersion version = plugin.versions().versionInformation(currentVersion);
    if (version == null) {
      sendPrefixedMessage(ChatColor.YELLOW + "此服务器正在运行未收录的 Intave 版本（" + currentVersion + "）", player);
      sendPrefixedMessage(ChatColor.YELLOW + "此版本可能存在未知问题", player);
    } else {
      if (version.typeClassifier() == IntaveVersion.Status.OUTDATED) {
        long duration = System.currentTimeMillis() - version.release();
        String durationAsString = DurationTranslator.translateHours(duration);

        sendPrefixedMessage(ChatColor.RED + "此服务器正在运行过旧的 Intave 版本（已发布 " + durationAsString + "）", player);
        if (!Bukkit.getPluginManager().isPluginEnabled("IntaveBootstrap")) {
          sendPrefixedMessage(ChatColor.RED + "可安装 IntaveBootstrap 自动保持最新版本", player);
        }
        sendPrefixedMessage(ChatColor.RED + "反作弊属于安全软件，请及时更新。", player);
      }
    }
  }

  @BukkitEventSubscription
  public void on(PlayerTeleportEvent teleport) {
    if (IntaveControl.DEBUG_TELEPORT_CAUSE_AND_CAUSER) {
      PluginInvocation pluginInvocation = Caller.pluginInfo(false);
      String pluginClass = pluginInvocation == null ? "无其他插件" : pluginInvocation.className();
      teleport.getPlayer().sendMessage("传送 " + teleport.getCause() + " " + teleport.getTo() + " 由 " + pluginClass);
    }
  }

  @BukkitEventSubscription
  public void on(BlockPlaceEvent place) {
    if (DISALLOW_ALL_BLOCK_PLACEMENTS_WITH_EVENT) {
      place.setCancelled(true);
    }
  }

  @BukkitEventSubscription
  public void on(WorldUnloadEvent unloadEvent) {
    World world = unloadEvent.getWorld();
    GarbageCollector.clear(world);
//    GarbageCollector.clear(world.getUID());
    GarbageCollector.clearIf(o -> o instanceof Location && ((Location) o).getWorld().equals(world));
  }

  @BukkitEventSubscription(priority = MONITOR)
  public void on(PlayerQuitEvent quit) {
    Player player = quit.getPlayer();
    GarbageCollector.clear(player);
    GarbageCollector.clear(player.getUniqueId());
  }

  /*
   * fixes a bug where players drop their sword whilst blocking, tricking the server into letting them constantly block - even whilst attacking
   */
  @BukkitEventSubscription(ignoreCancelled = true)
  public void on(PlayerDropItemEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    ItemStack item = player.getItemOnCursor();
    Material type = item.getType();
    boolean problematic = false;
    if (ItemProperties.isSwordItem(item) && !ViaVersionAdapter.ignoreBlocking(user.player())) {
      problematic = true;
    } else if (ItemProperties.isBow(type) || ItemProperties.foodConsumable(player, type)) {
      problematic = true;
    }
    if (problematic) {
      user.meta().inventory().releaseItemNextTick();
    }
  }

  @BukkitEventSubscription
  public void on(EntityShootBowEvent event) {
    if (!(event.getEntity() instanceof Player)) {
      return;
    }
    User user = UserRepository.userOf((Player) event.getEntity());
    InventoryMetadata inventory = user.meta().inventory();
    if (inventory.blockNextArrow) {
      boolean applyArrowBlock = System.currentTimeMillis() - inventory.lastBlockArrowRequest < 800L;
      if (applyArrowBlock) {
        event.setCancelled(true);
      }
      if (user.receives(MessageChannel.DEBUG_ITEM_RESETS)) {
        user.player().sendMessage(IntavePlugin.prefix() + " 已取消本次射箭以与服务器状态同步");
      }
      inventory.blockNextArrow = false;
    }
  }

//  @BukkitEventSubscription
//  public void on(EntityDamageByEntityEvent event) {
//    if (!(event.getDamager() instanceof Player)) {
//      return;
//    }
//    double predAttackDamage = DamageModify.attackDamageOf((Player) event.getDamager());
//    ItemStack heldItem = UserRepository.userOf((Player) event.getDamager()).meta().inventory().heldItem();
//    predAttackDamage += DamageModify.sharpnessDamageOf(heldItem);
//    double actualAttackDamage = event.getDamage(EntityDamageEvent.DamageModifier.BASE);
//    System.out.println("ATTACK " + event.getDamager() + " -> " + event.getEntity() + " " + predAttackDamage +"/"+actualAttackDamage);
//  }

//  @BukkitEventSubscription
//  public void on(PlayerAttackEntityCooldownResetEvent event) {
//    System.out.println("RESET " + event.getPlayer() + " " + event.getCooledAttackStrength());
////    Thread.dumpStack();
//  }

  private void sendPrefixedMessage(String message, CommandSender target) {
    if (!Bukkit.isPrimaryThread()) {
      Synchronizer.synchronize(() -> sendPrefixedMessage(message, target));
      return;
    }
    target.sendMessage(IntavePlugin.prefix() + message);
  }
}
