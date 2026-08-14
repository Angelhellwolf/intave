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

package de.jpx3.intave.command.stages;

import com.comphenix.protocol.ProtocolLibrary;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.access.player.trust.TrustFactor;
import de.jpx3.intave.command.CommandStage;
import de.jpx3.intave.command.Forward;
import de.jpx3.intave.command.Optional;
import de.jpx3.intave.command.SubCommand;
import de.jpx3.intave.module.Modules;
import de.jpx3.intave.module.actionbar.ActionBarDisplayer;
import de.jpx3.intave.module.actionbar.DisplayType;
import de.jpx3.intave.module.test.PhysicsTestRecorder;
import de.jpx3.intave.module.violation.ViolationVerboseMode;
import de.jpx3.intave.player.ProfileLookup;
import de.jpx3.intave.user.MessageChannel;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import de.jpx3.intave.user.permission.BukkitPermissionCheck;
import de.jpx3.intave.user.storage.LongTermViolationStorage;
import de.jpx3.intave.user.storage.PlaytimeStorage;
import de.jpx3.intave.user.storage.StorageViolationEvent;
import de.jpx3.intave.user.storage.StorageViolationEvents;
import de.jpx3.intave.version.DurationTranslator;
import de.jpx3.intave.version.IntaveVersion;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class BaseStage extends CommandStage {
  private static BaseStage singletonInstance;

  private BaseStage() {
    super(null, "/intave");
  }
/*
  @SubCommand(
    selectors = "violations",
    usage = "<player...>",
    description = "切换简易违规消息",
    permission = "intave.command.verbose"
  )
  public void violationCommand(User user, @Optional Player[] selectedPlayers) {
    Player player = user.player();
    if (user.receives(MessageChannel.VIOLATION_FINE)) {
      user.toggleReceive(MessageChannel.VIOLATION_FINE);
      player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.RED + "不再" + IntavePlugin.defaultColor() + "接收详细违规消息");
    }

    boolean receivesSimple = user.receives(MessageChannel.VIOLATION_SIMPLE);

    if (user.receives(MessageChannel.VIOLATION_SIMPLE)) {
      if (selectedPlayers != null && !user.hasChannelConstraint(MessageChannel.VIOLATION_SIMPLE)) {
        List<UUID> uniqueIds = Arrays.stream(selectedPlayers).map(Entity::getUniqueId).distinct().collect(Collectors.toList());
        user.setChannelConstraint(MessageChannel.VIOLATION_SIMPLE, player1 -> uniqueIds.contains(player1.getUniqueId()));
        String names = ChatColor.RED + describePlayerList(Arrays.stream(selectedPlayers).map(Entity::getName).map(s -> ChatColor.RED + s).collect(Collectors.toList()));
        player.sendMessage(IntavePlugin.prefix() + "已将简易违规消息限定为 " + names);
        return;
      }
    }

    user.toggleReceive(MessageChannel.VIOLATION_SIMPLE);
    user.removeChannelConstraint(MessageChannel.VIOLATION_SIMPLE);

    if (receivesSimple) {
      player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.RED + "不再" + IntavePlugin.defaultColor() + "接收简易违规消息");
    } else {
      if (selectedPlayers == null) {
        String target = ChatColor.RED + "所有人";
        player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.GREEN + "现在" + IntavePlugin.defaultColor() + "接收 " + target + " 的简易违规消息");
      } else {
        List<UUID> uniqueIds = Arrays.stream(selectedPlayers).map(Entity::getUniqueId).distinct().collect(Collectors.toList());
        user.setChannelConstraint(MessageChannel.VIOLATION_SIMPLE, player1 -> uniqueIds.contains(player1.getUniqueId()));
        String names = ChatColor.RED + describePlayerList(Arrays.stream(selectedPlayers).map(Entity::getName).map(s -> ChatColor.RED + s).collect(Collectors.toList()));
        player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.GREEN + "现在" + IntavePlugin.defaultColor() + "接收 " + names + " 的简易违规消息");
      }
    }
  }*/

  @SubCommand(
    selectors = "verbose",
    usage = "<player...>",
    description = "切换详细违规消息",
    permission = "intave.command.verbose"
  )
  public void verboseCommand(User user, @Optional Player[] selectedPlayers) {
    Player player = user.player();
    if (user.receives(MessageChannel.VIOLATION_SIMPLE)) {
      user.toggleReceive(MessageChannel.VIOLATION_SIMPLE);
      player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.RED + "不再" + IntavePlugin.defaultColor() + "接收简易违规消息");
    }

    boolean receivesVerbose = user.receives(MessageChannel.VIOLATION_FINE);

    ViolationVerboseMode mode = Modules.violationProcessor().verboseMode();
    String modeName = mode.name().toLowerCase();

    if (user.receives(MessageChannel.VIOLATION_FINE)) {
      if (selectedPlayers != null && !user.hasChannelConstraint(MessageChannel.VIOLATION_FINE)) {
        List<UUID> uniqueIds = Arrays.stream(selectedPlayers).map(Entity::getUniqueId).distinct().collect(Collectors.toList());
        user.setChannelConstraint(MessageChannel.VIOLATION_FINE, player1 -> uniqueIds.contains(player1.getUniqueId()));
        String names = ChatColor.RED + describePlayerList(Arrays.stream(selectedPlayers).map(Entity::getName).map(s -> ChatColor.RED + s).collect(Collectors.toList()));
        player.sendMessage(IntavePlugin.prefix() + "已将 " + modeName + " 详细违规输出限定为 " + names);
        return;
      }
    } /*else if (selectedPlayers == null && !IntavePlugin.singletonInstance().sibyl().isAuthenticated(player)) {
      player.sendMessage(IntavePlugin.prefix() + "/intave verbose <player...>");
      return;
    }*/

    user.toggleReceive(MessageChannel.VIOLATION_FINE);
    user.removeChannelConstraint(MessageChannel.VIOLATION_FINE);

    if (receivesVerbose) {
      player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.RED + "不再" + IntavePlugin.defaultColor() + "接收详细违规输出");
    } else {
      if (selectedPlayers == null) {
        String target = ChatColor.RED + "所有人";
        player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.GREEN + "现在" + IntavePlugin.defaultColor() + "接收 " + target + " 的详细违规输出");
      } else {
        List<UUID> uniqueIds = Arrays.stream(selectedPlayers).map(Entity::getUniqueId).distinct().collect(Collectors.toList());
        user.setChannelConstraint(MessageChannel.VIOLATION_FINE, player1 -> uniqueIds.contains(player1.getUniqueId()));
        String names = ChatColor.RED + describePlayerList(Arrays.stream(selectedPlayers).map(Entity::getName).map(s -> ChatColor.RED + s).collect(Collectors.toList()));
        player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.GREEN + "现在" + IntavePlugin.defaultColor() + "接收 " + names + " 的详细违规输出");
      }
    }
  }

  @SubCommand(
    selectors = {"cms", "combatmodifiers"},
    usage = "",
    description = "切换战斗修正调试",
    permission = "intave.command.combatmodifiers"
  )
  public void combatModifiersCommand(User user) {
    Player player = user.player();
    boolean receivesCombatModifiers = user.receives(MessageChannel.COMBAT_MODIFIERS);

    user.toggleReceive(MessageChannel.COMBAT_MODIFIERS);
    if (receivesCombatModifiers) {
      player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.RED + "不再" + IntavePlugin.defaultColor() + "接收战斗修正调试信息");
    } else {
      player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.GREEN + "现在" + IntavePlugin.defaultColor() + "接收战斗修正调试信息");
    }
  }

  private static String describePlayerList(List<String> elements) {
    int size = elements.size();
    String defaultColor = IntavePlugin.defaultColor();
    if (size == 0) {
      return defaultColor + "无人";
    } else if (size == 1) {
      return elements.get(0);
    } else {
      return defaultColor + String.join(defaultColor + "、", elements.subList(0, size - 1)) + defaultColor + " 和 " + elements.get(size - 1);
    }
  }

  @SubCommand(
    selectors = "debug",
    usage = "<debug type>",
    description = "切换调试消息",
    permission = "intave.command.verbose"
  )
  public void debug(User user, DebugType type) {
    Player player = user.player();
    boolean receivesDebug = user.receives(type.channel);

    user.toggleReceive(type.channel);
    user.removeChannelConstraint(type.channel);

    String cleanType = type.name().toLowerCase().replace("_", " ");
    if (receivesDebug) {
      player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.RED + "不再" + IntavePlugin.defaultColor() + "接收 " + ChatColor.RED + cleanType + IntavePlugin.defaultColor() + " 调试消息");
    } else {
      player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.GREEN + "现在" + IntavePlugin.defaultColor() + "接收 " + ChatColor.RED + cleanType + IntavePlugin.defaultColor() + " 调试消息");
    }
  }

  public enum DebugType {
    TELEPORT(MessageChannel.DEBUG_TELEPORT),
    MOUNTS(MessageChannel.DEBUG_MOUNTS),
    ITEM_RESETS(MessageChannel.DEBUG_ITEM_RESETS),
    BLOCK_CACHE(MessageChannel.DEBUG_BLOCK_CACHE),
    POSITION(MessageChannel.DEBUG_POSITION),
    PACKET_HOLD(MessageChannel.DEBUG_PACKET_HOLD),
    COLLISIONS(MessageChannel.DEBUG_COLLISIONS),
    NERFS(MessageChannel.DEBUG_NERFS),
    HITBOXES(MessageChannel.DEBUG_HITBOXES),
    HITBOX(MessageChannel.DEBUG_HITBOX),
    HITRAY(MessageChannel.DEBUG_HITRAY),
    MOVEMENT(MessageChannel.DEBUG_MOVEMENT),
    MOTION(MessageChannel.DEBUG_MOTION),
    SENT_INPUT(MessageChannel.DEBUG_SENT_INPUT),
    PLAYER_ACTIONS(MessageChannel.DEBUG_PLAYER_ACTIONS),
    ATTACK_RAYTRACE(MessageChannel.DEBUG_ATTACK_RAYTRACE),

    ;

    private final MessageChannel channel;

    DebugType(MessageChannel channel) {
      this.channel = channel;
    }
  }

  @SubCommand(
    selectors = {"cps", "clicks"},
    permission = "intave.command.cps",
    usage = "[<player...>]",
    description = "显示点击可视化"
  )
  public void cpsCommand(User user, @Optional Player selectedPlayer) {
    Player player = user.player();

    if (selectedPlayer == null) {
      selectedPlayer = player;
    }

    ActionBarDisplayer actionBar = Modules.actionBar();

    if (actionBar.inSubscription(user)) {
//      boolean isSameActionTarget = Objects.equals(user.actionTarget(), selectedPlayer.getUniqueId());
//      if (isSameActionTarget) {
//      }
      actionBar.unsubscribe(user);
      player.sendMessage(IntavePlugin.prefix() + "已取消订阅 " + ChatColor.RED + selectedPlayer.getName() + IntavePlugin.defaultColor() + " 的点击");
      return;
    }

    actionBar.subscribe(user, UserRepository.userOf(selectedPlayer), DisplayType.CLICKS);
    player.sendMessage(IntavePlugin.prefix() + "已订阅 " + ChatColor.RED + selectedPlayer.getName() + IntavePlugin.defaultColor() + " 的点击");
  }

  @SubCommand(
    selectors = {"alert", "alerts"},
    hideInHelp = true,
    description = ""
  )
  public void redirectToVerbose(CommandSender sender) {
    if (!BukkitPermissionCheck.permissionCheck(sender, "intave.command.verbose")) {
      showAllCommands(sender);
    } else {
      sender.sendMessage(IntavePlugin.prefix() + "你是想用 verbose 还是 notify？");
    }
  }

  @SubCommand(
    selectors = {"notify", "notifications"},
    usage = "[<player...>]",
    description = "切换通知",
    permission = "intave.command.notify"
  )
  public void notifyCommand(User user, @Optional Player[] selectedPlayers) {
    Player player = user.player();
    boolean receivesNotify = user.receives(MessageChannel.NOTIFY);

    if (user.receives(MessageChannel.NOTIFY)) {
      if (selectedPlayers != null && !user.hasChannelConstraint(MessageChannel.NOTIFY)) {
        List<UUID> uniqueIds = Arrays.stream(selectedPlayers).map(Entity::getUniqueId).distinct().collect(Collectors.toList());
        user.setChannelConstraint(MessageChannel.NOTIFY, player1 -> uniqueIds.contains(player1.getUniqueId()));
        String names = ChatColor.RED + describePlayerList(Arrays.stream(selectedPlayers).map(Entity::getName).map(s -> ChatColor.RED + s).collect(Collectors.toList()));
        player.sendMessage(IntavePlugin.prefix() + "已将通知限定为 " + names);
        return;
      }
    }

    user.toggleReceive(MessageChannel.NOTIFY);
    user.removeChannelConstraint(MessageChannel.NOTIFY);

    if (receivesNotify) {
      player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.RED + "不再" + IntavePlugin.defaultColor() + "接收通知");
    } else {
      if (selectedPlayers == null) {
        String target = ChatColor.RED + "所有人";
        player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.GREEN + "现在" + IntavePlugin.defaultColor() + "接收 " + target + " 的通知");
      } else {
        List<UUID> uniqueIds = Arrays.stream(selectedPlayers).map(Entity::getUniqueId).distinct().collect(Collectors.toList());
        user.setChannelConstraint(MessageChannel.NOTIFY, player1 -> uniqueIds.contains(player1.getUniqueId()));
        String names = ChatColor.RED + describePlayerList(Arrays.stream(selectedPlayers).map(Entity::getName).map(s -> ChatColor.RED + s).collect(Collectors.toList()));
        player.sendMessage(IntavePlugin.prefix() + "你" + ChatColor.GREEN + "现在" + IntavePlugin.defaultColor() + "接收 " + names + " 的通知");
      }
    }
  }

  @SubCommand(selectors = "dump")
  public void dump(CommandSender sender) {
    Player player = null;
    String playerVersion = "";
    if (sender instanceof Player) {
      player = ((Player) sender);
      User user = UserRepository.userOf(player);
      ProtocolMetadata protocol = user.meta().protocol();
      playerVersion = protocol.versionString() + "@" + protocol.protocolVersion();
      sender.sendMessage(ChatColor.GRAY + "玩家版本为 " + ChatColor.WHITE + playerVersion);
    } else {
      sender.sendMessage(ChatColor.GRAY + "请在游戏内执行此命令以显示客户端版本");
    }
    String intaveVersion = IntavePlugin.fullVersion();
    String serverVersion = Bukkit.getName() + "@" + Bukkit.getVersion();
    String protocolLibVersion = ProtocolLibrary.getPlugin().getDescription().getVersion();
    sender.sendMessage(ChatColor.GRAY + "服务端软件 Spigot 版本为 " + ChatColor.WHITE + serverVersion);
    sender.sendMessage(ChatColor.GRAY + "数据包组件 ProtocolLib 版本为 " + ChatColor.WHITE + protocolLibVersion);
    sender.sendMessage(ChatColor.GRAY + "反作弊插件 Intave 版本为 " + ChatColor.WHITE + intaveVersion);

    TextComponent message = new TextComponent("[Copy report message to chat]");
    message.setColor(net.md_5.bungee.api.ChatColor.GRAY);
    message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "Environment: `" + playerVersion + "`,`" + serverVersion + "`,`" + protocolLibVersion + "`,`" + intaveVersion + "`"));

    if (player != null) {
      // Send the message to the player
      player.spigot().sendMessage(message);
    }
  }

  @SubCommand(selectors = "record")
  public void recordCommand(User user) {
    PhysicsTestRecorder recorder = Modules.physicsTestRecorder();
    boolean recording = recorder.isRecording(user);
    recorder.setRecordingStatus(user, !recording);

    if (recording) {
      user.player().sendMessage(ChatColor.RED + "已停止录制..");

      File file;
      File resourcesFolder = new File(IntavePlugin.singletonInstance().dataFolder(), "../../../../src/test/resources");
      if (resourcesFolder.exists()) {
        file = new File(
          resourcesFolder,
          "/physics_test_runs/pending/" + UUID.randomUUID() + ".ptr"
        );
      } else {
        file = new File(
          IntavePlugin.singletonInstance().dataFolder(),
          "/recordings/" + UUID.randomUUID() + ".ptr"
        );
      }
      file.getParentFile().mkdirs();
      try {
        recorder.saveRecordingDataTo(user, file);
      } catch (IOException e) {
        user.player().sendMessage(ChatColor.RED + "保存录制失败: " + e.getMessage());
        return;
      }
      try {
        user.player().sendMessage(ChatColor.GREEN + "录制已保存至 " + file.getCanonicalPath());
      } catch (IOException e) {
        user.player().sendMessage(ChatColor.GREEN + "录制已保存至 " + file.getAbsolutePath());
      }
    } else {
      user.player().sendMessage(ChatColor.GREEN + "已开始录制..");
    }
  }

  @SubCommand(
    selectors = {"history", "logs"},
    usage = "<player>",
    description = "显示违规历史",
    permission = "intave.command.history"
  )
  public void historyCommand(CommandSender sender, String playerName) {
    Player player = Bukkit.getPlayer(playerName);
    if (isOnline(player)) {
      User targetUser = UserRepository.userOf(player);
      String name = player.getName();
      UUID id = player.getUniqueId();
      LongTermViolationStorage violationStorage = targetUser.storageOf(LongTermViolationStorage.class);
      outputHistory(sender, name, id, violationStorage);
    } else {
      sender.sendMessage(IntavePlugin.prefix() + ChatColor.YELLOW + "正在加载历史..");
      ProfileLookup.lookupIdFromName(playerName, uuid -> {
        if (uuid == null) {
          sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "未找到玩家 \"" + playerName + "\"");
        } else {
          Modules.storage().nullableManualStorageRequest(uuid, playerStorage -> {
            if (playerStorage == null) {
              sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + playerName + " 尚未进过服");
            } else {
              outputHistory(sender, playerName, uuid, playerStorage.storageOf(LongTermViolationStorage.class));
            }
          });
        }
      });
    }
  }

  @SubCommand(
    selectors = "info",
    usage = "<player>",
    description = "显示玩家信息",
    permission = "intave.command.verbose"
  )
  public void infoCommand(CommandSender sender, Player target) {
    User targetUser = UserRepository.userOf(target);
    String versionString = targetUser.meta().protocol().versionString();
    int protocolVersion = targetUser.meta().protocol().protocolVersion();
    int latency = targetUser.latency();
    int latencyJitter = targetUser.latencyJitter();
    TrustFactor trustFactor = targetUser.trustFactor();
    PlaytimeStorage playtimeStorage = targetUser.storageOf(PlaytimeStorage.class);
    long joins = playtimeStorage.totalJoins();
    int activePlaytime = (int) playtimeStorage.minutesPlayed();
    int afkPlaytime = (int) playtimeStorage.minutesAfk();
    boolean mitigated = !targetUser.meta().punishment().activeNerfers().isEmpty();

    String brand = targetUser.meta().protocol().clientBrand();
    String language = targetUser.meta().protocol().locale();

    UUID offlineTest = UUID.nameUUIDFromBytes(("OfflinePlayer:" + target.getName()).getBytes());
    boolean isOffline = target.getUniqueId().equals(offlineTest);

    sender.sendMessage(IntavePlugin.prefix() + "玩家信息");
    // Name, UUID, Brand/Locale, Version, Latency, Trust, Playtime, Joins, Mitigated,
    sender.sendMessage(ChatColor.GRAY + "名称: " + ChatColor.RED + target.getName());
    sender.sendMessage(ChatColor.GRAY + "玩家 UUID: " + ChatColor.RED + target.getUniqueId() + (isOffline ? ChatColor.GRAY + " (离线)" : ""));
    sender.sendMessage(ChatColor.GRAY + "品牌/语言: " + ChatColor.RED + brand + ChatColor.GRAY + "/" + ChatColor.RED + language);
    sender.sendMessage(ChatColor.GRAY + "版本: " + ChatColor.RED + versionString + ChatColor.GRAY + " (" + ChatColor.RED + protocolVersion + ChatColor.GRAY + ")");
    sender.sendMessage(ChatColor.GRAY + "延迟: " + ChatColor.RED + latency + ChatColor.GRAY + "ms (±" + ChatColor.RED + latencyJitter + ChatColor.GRAY + "ms)");
    sender.sendMessage(ChatColor.GRAY + "信任: " + trustFactor.coloredBaseName());

    String activePlaytimeDisplay = DurationTranslator.translateMinutes(activePlaytime * 60 * 1000L);
    String afkPlaytimeDisplay = DurationTranslator.translateMinutes(afkPlaytime * 60 * 1000L);

    sender.sendMessage(ChatColor.GRAY + "游玩时长 (活跃/挂机): " + ChatColor.RED + activePlaytimeDisplay + ChatColor.GRAY + "/" + ChatColor.RED + afkPlaytimeDisplay);
    sender.sendMessage(ChatColor.GRAY + "加入次数: " + ChatColor.RED + joins);
    sender.sendMessage(ChatColor.GRAY + "已削弱: " + ChatColor.RED + mitigated);
  }

  private void outputHistory(CommandSender sender, String name, UUID id, LongTermViolationStorage violationStorage) {
    StorageViolationEvents violations = violationStorage.violations();
    sender.sendMessage(String.format("%s" + ChatColor.RED + "%s%s 的历史:", IntavePlugin.prefix(), name, IntavePlugin.defaultColor()));
    if (violations.isEmpty()) {
      sender.sendMessage(IntavePlugin.prefix() + ChatColor.GREEN + "未发现违规");
      return;
    }
    printHistory(sender, "Reach", violations.fromCheck("attackraytrace"));
    printHistory(sender, "KillAura", violations.fromCheck("heuristics"));
    printHistory(sender, "Fly/Speed", violations.fromCheck("physics"));
    printHistory(sender, "Timer", violations.fromCheck("timer"));
    printHistory(sender, "AutoClicker", violations.fromCheck("clickpatterns"));
    printHistory(sender, "AutoClicker (speed)", violations.fromCheck("clickspeedlimiter"));
    printHistory(sender, "FastBreak", violations.fromCheck("breakspeedlimiter"));
    printHistory(sender, "BadPackets", violations.fromCheck("protocolscanner"));
    printHistory(sender, "Scaffold", violations.fromCheck("placementanalysis"));
    printHistory(sender, "ChestStealer", violations.fromCheck("inventoryanalysis"));
  }

  private void printHistory(CommandSender sender, String cheat, StorageViolationEvents violations) {
    if (violations.isEmpty()) {
      return;
    }
    if (violations.size() == 1) {
      StorageViolationEvent firstViolation = violations.first();
      String baseMessage = MessageFormat.format("{0}- 检测到使用 {1}{2}{0} {3}", IntavePlugin.defaultColor(), ChatColor.RED, cheat, durationToString(firstViolation.timePassedSince()));
      String defaultColor = IntavePlugin.defaultColor();
      TextComponent textComponent = new TextComponent(baseMessage);
      textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{
        new TextComponent(defaultColor + "检测 " + ChatColor.RED + correctlyFormattedCheckName(firstViolation.checkName())),
        new TextComponent(defaultColor + " 达到 " + ChatColor.RED + firstViolation.violationLevel() + defaultColor + "VL"),
        new TextComponent(defaultColor + " 于 " + ChatColor.RED + dateFormat(firstViolation.timestamp())),
      }));
      if (sender instanceof Player) {
        ((Player) sender).spigot().sendMessage(textComponent);
      } else {
        sender.sendMessage(baseMessage);
      }
      return;
    }

    String baseMessage = IntavePlugin.defaultColor() + "- 多次检测到使用 " + ChatColor.RED + cheat + IntavePlugin.defaultColor() + "，最近一次 " + durationToString(violations.newest().timePassedSince());
    String defaultColor = IntavePlugin.defaultColor();
    TextComponent newLine = new TextComponent(ComponentSerializer.parse("{text: \"\n\"}"));
    TextComponent[] textComponents = new TextComponent[violations.size()];
    int i = 0;
    for (StorageViolationEvent violation : violations) {
      TextComponent textComponent = new TextComponent(
        new TextComponent(defaultColor + "检测 " + ChatColor.RED + correctlyFormattedCheckName(violation.checkName())),
        new TextComponent(defaultColor + " 达到 " + ChatColor.RED + violation.violationLevel() + defaultColor + "VL"),
        new TextComponent(defaultColor + " 于 " + ChatColor.RED + dateFormat(violation.timestamp()))
      );
      if (i != violations.size() - 1) {
        textComponent.addExtra(newLine);
      }
      textComponents[i++] = textComponent;
    }

    TextComponent textComponent = new TextComponent(baseMessage);
    textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, textComponents));
    if (sender instanceof Player) {
      ((Player) sender).spigot().sendMessage(textComponent);
    } else {
      sender.sendMessage(baseMessage);
    }
  }

  private String correctlyFormattedCheckName(String checkNameLowercase) {
    IntavePlugin plugin = IntavePlugin.singletonInstance();
    return plugin.checks().searchCheck(checkNameLowercase).name();
  }

  private final DateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yy");

  private String dateFormat(long input) {
    return dateFormat.format(new Date(input));
  }

  // converts milliseconds to a string like "a few days ago"
  private String durationToString(long duration) {
    long seconds = duration / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    long days = hours / 24;
    if (days > 0) {
      return days + " 天前";
    }
    if (hours > 0) {
      return hours + " 小时前";
    }
    if (minutes > 0) {
      return minutes + " 分钟前";
    }
    if (seconds > 0) {
      return seconds + " 秒前";
    }
    return "几秒前";
  }

  private StorageViolationEvents filterByCheck(String check, StorageViolationEvents allViolations) {
    return allViolations.filter(event -> event.checkName().equalsIgnoreCase(check));
  }

  private boolean isOnline(OfflinePlayer player) {
    return player != null && (player.isOnline() || Bukkit.getPlayer(player.getUniqueId()) != null);
  }

  @SubCommand(
    selectors = "version",
    usage = "",
    description = "显示版本信息"
  )
  public void versionCommand(CommandSender commandSender) {
    sendVersionMessage(commandSender);
  }

//  @SubCommand(
//    selectors = "ui",
//    usage = "",
//    permission = "intave.command",
//    description = "Open the Intave UI"
//  )
//  public void openUICommand(CommandSender commandSender) {
//
//  }

//  @SubCommand(
//    selectors = "proxy",
//    usage = "",
//    description = "Access proxy related features",
//    permission = "intave.command.proxy"
//  )
//  @Forward(
//    target = ProxyStage.class
//  )
//  public void proxyCommand(CommandSender sender) {
//  }

  @SubCommand(
    selectors = "cloud",
    usage = "",
    description = "云端相关功能",
    permission = "intave.command.cloud"
  )
  @Forward(
    target = CloudStage.class
  )
  public void cloudCommand(CommandSender sender) {
  }

  @SubCommand(
    selectors = "root",
    usage = "",
    description = "",
    permission = "sibyl",
    hideInHelp = true
  )
  @Forward(
    target = RootStage.class
  )
  public void rootCommand(User user) {
  }

  @SubCommand(
    selectors = "diagnostics",
    usage = "",
    description = "运行时信息与诊断工具",
    permission = "intave.command.diagnostics.*"
  )
  @Forward(
    target = DiagnosticsStage.class
  )
  public void diagnosticsCommand(CommandSender commandSender) {
  }

  @SubCommand(
    selectors = "sample",
    usage = "",
    permission = "sibyl",
    hideInHelp = true
  )
  @Forward(
    target = SampleStage.class
  )
  public void sampleCommand(User user) {
  }

  @SubCommand(
    selectors = {"performance", "timings"},
    usage = "",
    description = "性能数据输出",
    permission = "intave.command.diagnostics.*"
  )
  @Forward(
    target = PerformanceStage.class
  )
  public void performanceTools(CommandSender commandSender) {

  }

  @SubCommand(
    selectors = "internals",
    usage = "",
    description = "控制台专用命令",
    permission = "intave.command.internals.*"
  )
  @Forward(
    target = InternalsStage.class
  )
  public void internalCommand(User user) {
  }

  @Override
  protected void showAllCommands(CommandSender sender) {
    boolean hasIntavePermission = BukkitPermissionCheck.permissionCheck(sender, "intave.command");
    if (hasIntavePermission) {
      super.showAllCommands(sender);
    } else {
      sendVersionMessage(sender);
    }
  }

  private void sendVersionMessage(CommandSender player) {
    boolean hasVersionViewPermission = BukkitPermissionCheck.permissionCheck(player, "intave.command");

    IntaveVersion versionInformation = IntavePlugin.singletonInstance().versions().versionInformation(IntavePlugin.versionTag());
    String version;
    if (!hasVersionViewPermission) {
      version = "（版本已隐藏）";
    } else if (versionInformation != null) {
      boolean outdated = versionInformation.outdated();
      version = IntavePlugin.fullVersion() + "（" + (outdated ? "已过时，" : "") + DurationTranslator.translateHours(System.currentTimeMillis() - versionInformation.release()) + " 前）";
    } else {
      version = IntavePlugin.fullVersion() + "（未知版本）";
    }

    String prefix = IntavePlugin.prefix();
    player.sendMessage(new String[]{
      prefix + "正在运行 Intave " + version,
      prefix + "用作自动化反作弊与防御工具",
      prefix + "访问 " + ChatColor.UNDERLINE + "intave.de" + IntavePlugin.defaultColor() + " 了解更多",
    });
  }

  public static BaseStage singletonInstance() {
    if (singletonInstance == null) {
      singletonInstance = new BaseStage();
    }
    return singletonInstance;
  }
}
