package de.jpx3.intave.command.stages;

import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.command.CommandStage;
import de.jpx3.intave.command.SubCommand;
import de.jpx3.intave.connect.cloud.Cloud;
import de.jpx3.intave.connect.cloud.protocol.Shard;
import de.jpx3.intave.module.Modules;
import de.jpx3.intave.module.nayoro.Nayoro;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class CloudStage extends CommandStage {
  private static CloudStage singletonInstance;

  private CloudStage() {
    super(BaseStage.singletonInstance(), "cloud");
  }

  @SubCommand(
    selectors = "status",
    usage = "",
    description = "显示版本信息"
  )
  public void statusCommand(CommandSender commandSender) {
    Cloud cloud = IntavePlugin.singletonInstance().cloud();
    boolean enabled = cloud.isEnabled();

    if (!enabled) {
      commandSender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "云端连接未启用");
      return;
    }

//    commandSender.sendMessage(IntavePlugin.prefix() + ChatColor.GRAY + "Status");
    commandSender.sendMessage(IntavePlugin.prefix() + ChatColor.GRAY + "连接状态");

    Map<Shard, Boolean> shardConnected = cloud.shardConnections();
    Map<Shard, Long> receivedBytes = cloud.receivedBytesPerShard();
    Map<Shard, Long> sentBytes = cloud.sentBytesPerShard();

    // connected to at least one
    boolean connectedToAtLeastOne = shardConnected.values().stream().anyMatch(b -> b);
    commandSender.sendMessage(ChatColor.GRAY + " 云端" + (connectedToAtLeastOne ? ChatColor.GREEN + "已连接" : ChatColor.RED + "未连接"));

    for (Map.Entry<Shard, Boolean> entry : shardConnected.entrySet()) {
      Shard shard = entry.getKey();
      boolean connected = entry.getValue();
      commandSender.sendMessage(ChatColor.GRAY + " 分片 " + ChatColor.GREEN + shard.name() + ChatColor.GRAY + " " + (connected ? ChatColor.GREEN + "已连接" : ChatColor.RED + "未连接") + ChatColor.GRAY + "（" + ChatColor.GREEN + formatBytes(receivedBytes.get(shard)) + ChatColor.GRAY + " 接收，" + ChatColor.GREEN + formatBytes(sentBytes.get(shard)) + ChatColor.GRAY + " 发送）");
    }

    if (connectedToAtLeastOne) {
//      commandSender.sendMessage(" ");
      cloud.generalStatusInquiry(stringStringMap -> {
        if (stringStringMap == null || stringStringMap.isEmpty()) {
          commandSender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "常规状态查询失败");
          return;
        }
        commandSender.sendMessage(IntavePlugin.prefix() + ChatColor.GRAY + "远程状态（来自云端）");
        // sorted by key (alphabetical)
        stringStringMap.forEach((key, value) -> commandSender.sendMessage(ChatColor.GRAY +" " + key + ": " + ChatColor.RED +  ChatColor.translateAlternateColorCodes('&', value)));
      });
    }

  }

  @SubCommand(
    selectors = "transmission",
    description = "显示玩家传输状态"
  )
  public void transmissionCommand(CommandSender commandSender) {
    Cloud cloud = IntavePlugin.singletonInstance().cloud();
    boolean enabled = cloud.isEnabled();

    if (!enabled) {
      commandSender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "云端连接未启用");
      return;
    }

    Nayoro nayoro = Modules.nayoro();
    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      String mainBase = IntavePlugin.prefix() + ChatColor.GRAY + "玩家 " + ChatColor.RED + onlinePlayer.getName() + ChatColor.GRAY;
      User user = UserRepository.userOf(onlinePlayer);
      if (nayoro.recordingActiveFor(user)) {
        mainBase += " " + ChatColor.GREEN + "正在传输";
      } else {
        mainBase += " " + ChatColor.RED + "未传输";
      }

      if (nayoro.hasRecordSink(user)) {
        mainBase += ChatColor.GRAY + " 且 " + ChatColor.GREEN + "正在录制";
      } else {
        mainBase += ChatColor.GRAY + " 且 " + ChatColor.RED + "未录制";
      }

      commandSender.sendMessage(mainBase);
    }
  }

  private String formatBytes(long bytes) {
    if (bytes < 1024) {
      return bytes + "B";
    } else if (bytes < 1024 * 1024) {
      return bytes / 1024 + "KB";
    } else if (bytes < 1024 * 1024 * 1024) {
      return bytes / (1024 * 1024) + "MB";
    } else {
      return bytes / (1024 * 1024 * 1024) + "GB";
    }
  }

  public static CloudStage singletonInstance() {
    if (singletonInstance == null) {
      singletonInstance = new CloudStage();
    }
    return singletonInstance;
  }
}
