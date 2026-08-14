package de.jpx3.intave.command.stages;

import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.command.CommandStage;
import de.jpx3.intave.command.Optional;
import de.jpx3.intave.command.SubCommand;
import de.jpx3.intave.diagnostic.message.*;
import de.jpx3.intave.user.User;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class InternalDebugStage extends CommandStage {
  private static InternalDebugStage singletonInstance;

  private InternalDebugStage() {
    super(RootStage.singletonInstance(), "debug");
  }

  @SubCommand(
    selectors = "enable",
    description = "启用调试模式",
    permission = "sibyl"
  )
  public void enableAll(User user, @Optional MessageCategory category) {
    OutputConfiguration outputConfiguration = DebugBroadcast.configurationOf(user.id());

    if (category != null) {
      outputConfiguration.activateCategory(category);
      user.player().sendMessage(ChatColor.GREEN + "已启用 " + category.description().toLowerCase(Locale.ROOT) + " 调试模式");
    } else {
      outputConfiguration.activateAllCategories();
      user.player().sendMessage(ChatColor.GREEN + "已启用全部调试模式。");
    }
  }

  @SubCommand(
    selectors = "disable",
    description = "关闭调试模式",
    permission = "sibyl"
  )
  public void disableAll(User user, @Optional MessageCategory category) {
    OutputConfiguration outputConfiguration = DebugBroadcast.configurationOf(user.id());

    if (category != null) {
      outputConfiguration.deactivateCategory(category);
      user.player().sendMessage(ChatColor.GREEN + "已关闭 " + category.description().toLowerCase(Locale.ROOT) + " 调试模式");
    } else {
      outputConfiguration.deactivateAllCategories();
      user.player().sendMessage(ChatColor.GREEN + "已关闭全部调试模式。");
    }
  }

  @SubCommand(
    selectors = "color",
    description = "设置颜色",
    permission = "sibyl"
  )
  public void setColor(User user, MessageCategory category, ChatColor color) {
    OutputConfiguration outputConfiguration = DebugBroadcast.configurationOf(user.id());
    outputConfiguration.setColor(category, color);
    user.player().sendMessage(ChatColor.GREEN + category.description().toLowerCase(Locale.ROOT) + " 颜色已设为 " + color.name());
  }

  @SubCommand(
    selectors = "severity",
    description = "设置最低严重级别",
    permission = "sibyl"
  )
  public void setMinimumSeverity(User user, MessageSeverity severity) {
    OutputConfiguration outputConfiguration = DebugBroadcast.configurationOf(user.id());
    outputConfiguration.setMinimumSeverity(severity);
    user.player().sendMessage(ChatColor.GREEN + "最低严重级别已设为 " + severity.name());
  }

  @SubCommand(
    selectors = "detail",
    description = "设置消息详情",
    permission = "sibyl"
  )
  public void setOutputDetail(User user, MessageDetail detail, @Optional MessageCategory category) {
    OutputConfiguration outputConfiguration = DebugBroadcast.configurationOf(user.id());
    if (category != null) {
      outputConfiguration.setMessageDetail(category, detail);
      user.player().sendMessage(ChatColor.GREEN + category.description().toLowerCase(Locale.ROOT) + " 详情已设为 " + detail.name());
    } else {
      outputConfiguration.setDefaultMessageDetail(detail);
      user.player().sendMessage(ChatColor.GREEN + "详情已设为 " + detail.name());
    }
  }

  @SubCommand(
    selectors = "prefix",
    description = "设置前缀格式",
    permission = "sibyl"
  )
  public void setFormatter(User user, PrefixDetail detail) {
    OutputConfiguration outputConfiguration = DebugBroadcast.configurationOf(user.id());
    outputConfiguration.setDefaultPrefixDetail(detail);
    user.player().sendMessage(ChatColor.GREEN + "前缀已设为 " + detail.name());
  }

  @SubCommand(
    selectors = "settarget",
    description = "设置目标",
    permission = "sibyl"
  )
  public void setTarget(User user, MessageCategory cat, @Optional Player[] targets) {
    OutputConfiguration outputConfiguration = DebugBroadcast.configurationOf(user.id());
    outputConfiguration.addConstraint(cat, player -> {
      if (targets == null) {
        return true;
      }
      for (Player target : targets) {
        if (player.equals(target)) {
          return true;
        }
      }
      return false;
    });
    user.player().sendMessage(ChatColor.GREEN + "已为 " + cat.description().toLowerCase(Locale.ROOT) + " 设置目标");
  }

  @SubCommand(
    selectors = "selftarget",
    description = "设置自身目标",
    permission = "sibyl"
  )
  public void setSelfTarget(User user, MessageCategory cat) {
    OutputConfiguration outputConfiguration = DebugBroadcast.configurationOf(user.id());
    outputConfiguration.addConstraint(cat, player -> player.equals(user.player()));
    user.player().sendMessage(ChatColor.GREEN + "已为 " + cat.description().toLowerCase(Locale.ROOT) + " 设置自身目标");
  }

  @SubCommand(
    selectors = "remtarget",
    description = "移除目标",
    permission = "sibyl"
  )
  public void removeTarget(User user, MessageCategory cat) {
    OutputConfiguration outputConfiguration = DebugBroadcast.configurationOf(user.id());
    outputConfiguration.removeConstraint(cat);
    user.player().sendMessage(ChatColor.GREEN + "已移除 " + cat.description().toLowerCase(Locale.ROOT) + " 的目标");
  }

  @SubCommand(
    selectors = "status",
    description = "移除自身目标",
    permission = "sibyl"
  )
  public void status(User user) {
    OutputConfiguration outputConfiguration = DebugBroadcast.configurationOf(user.id());
    Player player = user.player();
    player.sendMessage(IntavePlugin.prefix() + "调试模式状态");
    String prefixSelectorName = outputConfiguration.prefixSelector().name().toLowerCase(Locale.ROOT).replace("_", " ");
    player.sendMessage(IntavePlugin.prefix() + ChatColor.GRAY + "前缀为 " + prefixSelectorName + "。示例: " + outputConfiguration.prefixSelector().formatPrefix(MessageSeverity.MEDIUM, "NAME") + "");

    for (MessageCategory category : MessageCategory.values()) {
      ChatColor color = outputConfiguration.colorOf(category);
      String active = outputConfiguration.isActive(category) ? ChatColor.GREEN + "已启用" + ChatColor.GRAY : ChatColor.RED + "已关闭" + ChatColor.GRAY;
      String format = outputConfiguration.detailOf(category).name().toLowerCase(Locale.ROOT);
      String description = category.description().toLowerCase(Locale.ROOT);
      player.sendMessage(color + " " + category.name() + ChatColor.GRAY + "（" + color + description + ChatColor.GRAY + "）" + " " + active + "，格式 " + format);
    }
  }

  @SubCommand(
    selectors = "jump",
    description = "触发物理误判以产生跳跃",
    permission = "sibyl"
  )
  public void falseFlag(User user) {
    user.meta().movement().baseMotionY = 2;
  }

  public static InternalDebugStage singletonInstance() {
    if (singletonInstance == null) {
      singletonInstance = new InternalDebugStage();
    }
    return singletonInstance;
  }
}
