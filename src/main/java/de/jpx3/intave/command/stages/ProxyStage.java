package de.jpx3.intave.command.stages;

import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.command.CommandStage;
import de.jpx3.intave.command.Optional;
import de.jpx3.intave.command.SubCommand;
import de.jpx3.intave.connect.proxy.protocol.IntavePacket;
import de.jpx3.intave.connect.proxy.protocol.packets.IntavePacketOutExecuteCommand;
import de.jpx3.intave.connect.proxy.protocol.packets.IntavePacketOutPunishment;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class ProxyStage extends CommandStage {
  private static ProxyStage singletonInstance;
  private final IntavePlugin plugin;

  private ProxyStage() {
    super(BaseStage.singletonInstance(), "proxy");
    plugin = IntavePlugin.singletonInstance();
  }

  @SubCommand(
    selectors = {"command", "proxcommand"},
    usage = "<player> <command...>",
    permission = "intave.command.proxy",
    description = "在代理上远程执行命令"
  )
  public void proxyCommand(CommandSender sender, Player uplink, String[] commandParts) {
    if (!plugin.proxy().isChannelOpen()) {
      sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "未连接到代理");
      return;
    }
    String command = Arrays.stream(commandParts).map(commandPart -> commandPart + " ").collect(Collectors.joining()).trim();
    IntavePacket packet = new IntavePacketOutExecuteCommand(uplink.getUniqueId(), command);
    plugin.proxy().sendPacket(uplink, packet);
    sender.sendMessage(IntavePlugin.prefix() + "已下发远程命令执行 \"/" + command + "\"");
  }

  @SubCommand(
    selectors = {"kick", "proxkick"},
    usage = "<player> [<message...>]",
    permission = "intave.command.proxy",
    description = "从代理远程踢出目标玩家"
  )
  public void proxyKick(CommandSender sender, Player target, @Optional String[] message) {
    if (!plugin.proxy().isChannelOpen()) {
      sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "未连接到代理");
      return;
    }
    String reason = message == null ? "未提供" : Arrays.stream(message).map(commandPart -> commandPart + " ").collect(Collectors.joining()).trim();
    performPunishment(target, IntavePacketOutPunishment.PunishmentType.KICK, reason);
    sender.sendMessage(IntavePlugin.prefix() + "已下发远程踢出");
  }

  @SubCommand(
    selectors = {"tempban", "proxtempban"},
    usage = "<player> [<message...>]",
    permission = "intave.command.proxy",
    description = "从代理远程临时封禁目标玩家"
  )
  public void proxyTempBan(CommandSender sender, Player target, @Optional String[] reasonParts) {
    if (!plugin.proxy().isChannelOpen()) {
      sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "未连接到代理");
      return;
    }
    String reason = reasonParts == null ? "未提供" : Arrays.stream(reasonParts).map(commandPart -> commandPart + " ").collect(Collectors.joining()).trim();
    performPunishment(target, IntavePacketOutPunishment.PunishmentType.TEMP_BAN, reason);
    sender.sendMessage(IntavePlugin.prefix() + "已下发远程临时封禁");
  }

  @SubCommand(
    selectors = {"ban", "proxban"},
    usage = "<player> [<message...>]",
    permission = "intave.command.proxy",
    description = "从代理远程封禁目标玩家"
  )
  public void proxyBan(CommandSender sender, Player target, @Optional String[] reasonParts) {
    if (!plugin.proxy().isChannelOpen()) {
      sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "未连接到代理");
      return;
    }
    String reason = reasonParts == null ? "未提供" : Arrays.stream(reasonParts).map(commandPart -> commandPart + " ").collect(Collectors.joining()).trim();
    performPunishment(target, IntavePacketOutPunishment.PunishmentType.BAN, reason);
    sender.sendMessage(IntavePlugin.prefix() + "已下发远程封禁");
  }

  private void performPunishment(
    Player target,
    IntavePacketOutPunishment.PunishmentType type,
    String reason
  ) {
    long tempbanEndTimestamp = System.currentTimeMillis() + 1000 * 60 * 60;
    IntavePacket packet = new IntavePacketOutPunishment(target.getUniqueId(), type, reason.trim(), tempbanEndTimestamp);
    plugin.proxy().sendPacket(target, packet);
  }

  public static ProxyStage singletonInstance() {
    if (singletonInstance == null) {
      singletonInstance = new ProxyStage();
    }
    return singletonInstance;
  }
}
