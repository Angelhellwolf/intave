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

import de.jpx3.intave.IntaveControl;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.access.player.trust.TrustFactor;
import de.jpx3.intave.block.cache.BlockCache;
import de.jpx3.intave.check.Check;
import de.jpx3.intave.check.CheckStatistics;
import de.jpx3.intave.command.CommandStage;
import de.jpx3.intave.command.Forward;
import de.jpx3.intave.command.Optional;
import de.jpx3.intave.command.SubCommand;
import de.jpx3.intave.diagnostic.*;
import de.jpx3.intave.diagnostic.timings.Timing;
import de.jpx3.intave.diagnostic.timings.Timings;
import de.jpx3.intave.library.Python;
import de.jpx3.intave.library.python.PythonTask;
import de.jpx3.intave.math.Occurrences;
import de.jpx3.intave.module.Modules;
import de.jpx3.intave.module.nayoro.Nayoro;
import de.jpx3.intave.security.HashAccess;
import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.ConnectionMetadata;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static de.jpx3.intave.diagnostic.ShapeAccessFlowStudy.*;
import static de.jpx3.intave.math.MathHelper.formatDouble;

public final class RootStage extends CommandStage {
  private static final String JAR_HASH;
  private static final Map<Class<?>, String> CLASS_NAME = new HashMap<>();
  private static RootStage singletonInstance;

  static {
    String hash;
    try {
      File currentJavaJarFile = new File(IntavePlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      hash = HashAccess.hashOf(currentJavaJarFile).substring(0, 9);
    } catch (Exception exception) {
      hash = "Unavailable";
    }
    JAR_HASH = hash;
  }

  static {
    CLASS_NAME.put(BoundingBox.class, "BoundingBoxes");
//    CLASS_NAME.put(BlockState.class, "BlockStates");?
//    CLASS_NAME.put(CubeShape.class, "CubeShape");
//    CLASS_NAME.put(ArrayBlockShape.class, "ArrayBlockShape");
  }

  private final IntavePlugin plugin;

  private RootStage() {
    super(BaseStage.singletonInstance(), "root");
    plugin = IntavePlugin.singletonInstance();
  }

  public static String largeNumberFormat(double value) {
    DecimalFormat df = new DecimalFormat("###,###,###");
    return df.format(value);
  }

  // stolen from https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
  private static String humanReadableByteCount(long bytes) {
    if (-1000 < bytes && bytes < 1000) {
      return bytes + "B";
    }
    CharacterIterator ci = new StringCharacterIterator("kMGTPE");
    while (bytes <= -999_950 || bytes >= 999_950) {
      bytes /= 1000;
      ci.next();
    }
    return String.format("%.1f%cB", bytes / 1000.0, ci.current());
  }

  public static RootStage singletonInstance() {
    if (singletonInstance == null) {
      singletonInstance = new RootStage();
    }
    return singletonInstance;
  }

  @SubCommand(
    selectors = "timings",
    usage = "",
    description = "输出计时数据",
    permission = "sibyl"
  )
  public void timingsCommand(User user, @Optional String[] specifier) {
    String fullSpecifier = specifier != null ? Arrays.stream(specifier).map(s -> s + " ").collect(Collectors.joining()).trim().toLowerCase(Locale.ROOT) : "";

    Player player = user.player();
    if (plugin.sibyl().authentication().isAuthenticated(player)) {
      player.sendMessage(ChatColor.RED + "正在加载计时...");
      List<Timing> timings = new ArrayList<>(Timings.timingPool());
      timings.sort(Timing::compareTo);

      timings.forEach(timing -> {
        if (timing.isPacketEventTiming() || timing.isBukkitEventTiming()) {
          return;
        }
        boolean suspicious = timing.averageCallDurationInMillis() > 0.5d;
        boolean dumping = timing.averageCallDurationInMillis() > 1.5d;
        String message;
        ChatColor outputColor = suspicious ? (dumping ? ChatColor.RED : ChatColor.YELLOW) : ChatColor.GREEN;
        message = String.format(
          "%s: %s::%s%s (%s&f %s/c)",
          timing.coloredName(),
          timing.recordedCalls(),
          formatDouble(timing.totalDurationMillis() / 1000d, 2),
          "s",
          outputColor + "" + largeNumberFormat((long) timing.averageCallDurationInNanos()),
          "ns"
        );
        if (!fullSpecifier.isEmpty() && !"ns".equals(fullSpecifier) && !timing.name().toLowerCase(Locale.ROOT).contains(fullSpecifier)) {
          message = IntavePlugin.defaultColor() + ChatColor.stripColor(message);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
      });
    }
  }

  @SubCommand(
    selectors = "eventtimings",
    usage = "",
    description = "输出计时数据",
    permission = "sibyl"
  )
  public void eventTimingsCommand(User user, @Optional String[] specifier) {
    String fullSpecifier = specifier != null ? Arrays.stream(specifier).map(s -> s + " ").collect(Collectors.joining()).trim().toLowerCase(Locale.ROOT) : "";

    Player player = user.player();
    if (plugin.sibyl().authentication().isAuthenticated(player)) {
      player.sendMessage(ChatColor.RED + "正在加载计时...");

      List<Timing> timings = new ArrayList<>(Timings.timingPool());
      timings.sort(Timing::compareTo);

      timings.forEach(timing -> {
        if (!timing.isBukkitEventTiming()) return;
        boolean suspicious = timing.averageCallDurationInMillis() > 0.5d;
        boolean dumping = timing.averageCallDurationInMillis() > 1.5d;
        String message = String.format(
          "%s: %s::%sms (%s ms/c)",
          timing.coloredName(),
          timing.recordedCalls(),
          formatDouble(timing.totalDurationMillis(), 4),
          (suspicious ? (dumping ? ChatColor.RED : ChatColor.YELLOW) : ChatColor.GREEN) + "" +
            formatDouble(timing.averageCallDurationInMillis(), 8)
            + ChatColor.WHITE
        );
        if (!fullSpecifier.isEmpty() && !timing.name().toLowerCase(Locale.ROOT).contains(fullSpecifier)) {
          message = IntavePlugin.defaultColor() + ChatColor.stripColor(message);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
      });
    }
  }

  @SubCommand(
    selectors = "debug",
    usage = "",
    description = "输出诊断消息",
    permission = "sibyl"
  )
  @Forward(target = InternalDebugStage.class)
  public void debugStage() {

  }

  @SubCommand(
    selectors = "hash",
    usage = "",
    description = "显示 jar 哈希",
    permission = "sibyl"
  )
  public void hashCommand(User user) {
    Player player = user.player();
    if (plugin.sibyl().authentication().isAuthenticated(player)) {
      player.sendMessage(ChatColor.GRAY + "哈希为 " + ChatColor.COLOR_CHAR + JAR_HASH);
    }
  }

  @SubCommand(
    selectors = "playback",
    usage = "",
    description = "回放已记录计时",
    permission = "sibyl"
  )
  public void playbackCommand(User user, @Optional Player target) {
    User targetUser = target != null ? UserRepository.userOf(target) : user;
    Nayoro nayoro = Modules.nayoro();
    nayoro.instantPlayback(targetUser);
  }

  @SubCommand(
    selectors = "packettimings",
    usage = "",
    description = "输出计时数据",
    permission = "sibyl"
  )
  public void packetTimingsCommand(User user, @Optional String[] specifier) {
    String fullSpecifier = specifier != null ? Arrays.stream(specifier).map(s -> s + " ").collect(Collectors.joining()).trim().toLowerCase(Locale.ROOT) : "";

    Player player = user.player();
    if (plugin.sibyl().authentication().isAuthenticated(player)) {
      player.sendMessage(ChatColor.RED + "正在加载计时...");

      List<Timing> timings = new ArrayList<>(Timings.timingPool());
      timings.sort(Timing::compareTo);

      timings.forEach(timing -> {
        if (!timing.isPacketEventTiming()) return;
        boolean suspicious = timing.averageCallDurationInMillis() > 0.5d;
        boolean dumping = timing.averageCallDurationInMillis() > 1.5d;
        String message = String.format(
          "%s: %s::%sms (%s&f ms/c)",
          timing.coloredName(),
          timing.recordedCalls(),
          formatDouble(timing.totalDurationMillis(), 4),
          (suspicious ? (dumping ? ChatColor.RED : ChatColor.YELLOW) : ChatColor.GREEN) + "" +
            formatDouble(timing.averageCallDurationInMillis(), 8)
        );
        if (!fullSpecifier.isEmpty() && !timing.name().toLowerCase(Locale.ROOT).contains(fullSpecifier)) {
          message = IntavePlugin.defaultColor() + ChatColor.stripColor(message);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
      });
    }
  }

  @SubCommand(
    selectors = "statistics",
    usage = "",
    description = "输出检测统计",
    permission = "sibyl"
  )
  public void checkStatisticsCommand(User user) {
    Player player = user.player();
    player.sendMessage(ChatColor.RED + "正在加载统计...");
    for (Check check : plugin.checks().checks()) {
      CheckStatistics statistics = check.baseStatistics();
      double processed = statistics.totalProcessed();
      double violations = statistics.totalViolations();
      double failed = statistics.totalFails();
      long passed = statistics.totalPasses();

      if (processed == 0) {
        continue;
      }

      String failedRate = formatDouble(failed / processed * 100, 5);
      String violatedRate = formatDouble(violations / processed * 100, 5);

      String message = String.format("Check/%s: %s::%s%% / vio %s%%", check.name(), passed, failedRate, violatedRate);
      player.sendMessage(ChatColor.WHITE + message);
    }
  }

  @SubCommand(
    selectors = "keys",
    usage = "",
    description = "",
    permission = "sibyl"
  )
  public void outputKeyStatistic(User user) {
    Player player = user.player();
    player.sendMessage(ChatColor.RED + "正在加载按键研究..");
    Map<String, Double> studyResult = KeyPressStudy.resultShare();
    Map<String, Double> sortedStudy = sortHashMapByValues(studyResult);

    sortedStudy.forEach((keys, percentage) -> {
      if (keys.trim().isEmpty()) {
        keys = "N";
      }
      player.sendMessage("按键 " + keys + " " + formatDouble(percentage * 100, 4) + "%");
    });
  }

  @SubCommand(
    selectors = "resync",
    usage = "",
    permission = "sibyl",
    description = ""
  )
  public void checkPacketResync(CommandSender sender) {
    sender.sendMessage(IntavePlugin.prefix() + "正在加载数据..");
    Map<String, Long> packets = PacketSynchronizations.output();
    if (packets.isEmpty()) {
      sender.sendMessage(ChatColor.GREEN + "暂无硬重同步记录");
    } else {
      packets = sortHashMapByValues(packets);
      packets.forEach((name, hardsResyncs) -> {
        sender.sendMessage(ChatColor.RED + name.toLowerCase(Locale.ROOT) + IntavePlugin.defaultColor() + " 包共触发 " + ChatColor.RED + hardsResyncs + IntavePlugin.defaultColor() + " 次硬重同步");
      });
    }
  }

  @SubCommand(
    selectors = "latencies",
    usage = "",
    description = "",
    permission = "sibyl"
  )
  public void outputAttackLatencies(User user) {
    Player player = user.player();
    player.sendMessage("平均攻击延迟为 " + formatDouble(LatencyStudy.attackLatency(), 2) + " 游戏刻");
  }

  @SubCommand(
    selectors = "bbaf",
    usage = "",
    description = "",
    permission = "sibyl"
  )
  public void outputBBAF(User user) {
    Player player = user.player();
    player.sendMessage(ChatColor.RED + "正在加载碰撞箱访问流研究..");

    String colorScheme = ChatColor.GREEN + "" + green + " " + ChatColor.YELLOW + yellow + " " + ChatColor.RED + red + "" + ChatColor.GRAY;
    player.sendMessage(ChatColor.GRAY + "" + requests + " 次请求需要 " + lookups + " 次查找（" + colorScheme + "), " + ChatColor.AQUA + ((lookups) - dynamic) + ChatColor.GRAY + " 次由服务器");
  }

  @SubCommand(
    selectors = "replacements",
    usage = "",
    description = "",
    permission = "sibyl"
  )
  public void outputReplacements(User user) {
    Player player = user.player();
    BlockCache bba = user.blockCache();
    player.sendMessage(ChatColor.RED + "你有 " + bba.numOfLocatedReplacements() + "/" + bba.numOfIndexedReplacements() + " 个替换");
  }

  @SubCommand(
    selectors = "settrust",
    usage = "<trustfactor> [<target>]",
    description = "",
    permission = "sibyl"
  )
  public void setTrustFactor(User user, TrustFactor trustFactor, @Optional Player target) {
    if (target == null) {
      target = user.player();
    }
    UserRepository.userOf(target).setTrustFactor(trustFactor);
    user.player().sendMessage(ChatColor.GRAY + "已将 " + trustFactor.chatColor() + trustFactor.name() + ChatColor.GRAY + " 信任等级应用到 " + ChatColor.RED + target.getName());
  }

  @SubCommand(
    selectors = "trust",
    usage = "[<target>]",
    description = "",
    permission = "sibyl"
  )
  public void lookupTrust(User user, @Optional Player target) {
    if (target == null) {
      target = user.player();
    }
    TrustFactor trustFactor = UserRepository.userOf(target).trustFactor();
    user.player().sendMessage(ChatColor.RED + target.getName() + ChatColor.GRAY + " 的信任等级为 " + trustFactor.chatColor() + trustFactor.name() + ChatColor.GRAY + " 信任等级");
  }

  @SubCommand(
    selectors = "traping",
    usage = "[<target>]",
    description = "",
    permission = "sibyl"
  )
  public void transactionPing(User user, @Optional Player target) {
    if (target == null) {
      target = user.player();
    }
    user.player().sendMessage(ChatColor.RED + target.getName() + ChatColor.GRAY + " 的事务延迟为 " + ChatColor.RED + UserRepository.userOf(target).meta().connection().transactionPingAverage() + ChatColor.GRAY + "ms");
  }

  @SubCommand(
    selectors = "atradist",
    usage = "[<target>]",
    description = "",
    permission = "sibyl"
  )
  public void attackVsTransactionDistribution(User user, @Optional Player target) {
    if (target == null) {
      target = user.player();
    }
    User targetUser = UserRepository.userOf(target);
    ConnectionMetadata connection = targetUser.meta().connection();
    Occurrences<Integer> attackDelays = connection.attackDelays;
    Occurrences<Integer> feedbackDelays = connection.feedbackDelays;
    String preset = ChatColor.RED + "ATT/FBK for " + target.getName() + " mean(%s/%s) var(%s/%s)";
    String message = String.format(preset,
      formatDouble(attackDelays.mean(), 2), formatDouble(feedbackDelays.mean(), 2),
      formatDouble(attackDelays.variance(), 2), formatDouble(feedbackDelays.variance(), 2)
    );
    user.player().sendMessage(message);

    user.player().sendMessage("攻击分布");
    attackDelays.plotAsBarDiagram(4).forEach(user.player()::sendMessage);
    user.player().sendMessage("反馈分布");
    feedbackDelays.plotAsBarDiagram(4).forEach(user.player()::sendMessage);
  }

  @SubCommand(
    selectors = "trustmap",
    usage = "",
    permission = "sibyl"
  )
  public void trustfactorMap(User user) {
    Map<TrustFactor, AtomicLong> trustfactorDistribution = new HashMap<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (UserRepository.hasUser(player)) {
        TrustFactor trustFactor = UserRepository.userOf(player).trustFactor();
        trustfactorDistribution
          .computeIfAbsent(trustFactor, x -> new AtomicLong())
          .incrementAndGet();
      }
    }
    Player player = user.player();
    player.sendMessage(ChatColor.GRAY + "信任等级分布:");
    for (TrustFactor value : TrustFactor.values()) {
      long count = trustfactorDistribution.getOrDefault(value, new AtomicLong()).get();
      player.sendMessage((count > 0 ? ChatColor.RED + "" + count : ChatColor.GRAY + "0") + ChatColor.GRAY + "x " + value.chatColor() + value.name());
    }
  }

  @SubCommand(
    selectors = {"script", "sk"},
    usage = "<args...>",
    description = "",
    permission = "sibyl"
  )
  public void script(User user, String[] args) {
    Player player = user.player();
    if (!user.id().equals(UUID.fromString("5ee6db6d-6751-4081-9cbf-28eb0f6cc055"))) {
      player.sendMessage(ChatColor.RED + "此命令仅供使用脚本的开发者");
      return;
    }

    Map<String, PythonTask> tasks = Python.tasks();

    if (args.length == 0) {
      player.sendMessage(ChatColor.GRAY + "可用脚本:");
      for (String name : tasks.keySet()) {
        player.sendMessage(ChatColor.RED + name);
      }
      return;
    }

    String name = args[0];
    PythonTask task = tasks.get(name);

    if (task == null) {
      player.sendMessage(ChatColor.RED + "未知脚本 " + name);
      return;
    }

    String[] scriptArgs = Arrays.copyOfRange(args, 1, args.length);
    String joinedArgs = String.join(" ", scriptArgs);

    task.feedLineAndRead(joinedArgs, player::sendMessage);
    player.sendMessage(ChatColor.GRAY + "已执行");
  }

  @SubCommand(
    selectors = "asyncmessage",
    usage = "",
    description = "",
    permission = "sibyl"
  )
  public void asyncMessageInNetty(User user) {
    user.meta().connection().sendAsyncMessage = true;
  }

  @SubCommand(
    selectors = "invisibleBlock",
    usage = "",
    description = "",
    permission = "sibyl"
  )
  public void invisibleBlock(User user) {
    Player player = user.player();
    Location location = player.getLocation();
    user.blockCache().override(player.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), Material.OBSIDIAN, 0);
    player.sendMessage(ChatColor.GREEN + "方块已生成");
  }

  @SubCommand(
    selectors = "memtrace",
    usage = "",
    description = "",
    permission = "sibyl"
  )
  public void memtrace(User user) {
    Player player = user.player();
    if (!IntaveControl.ENABLE_MEMTRACE) {
      player.sendMessage(ChatColor.RED + "请启用 PERFORMANCE_RECORD 以进行类型 1 内存追踪");
      return;
    }

    Map<Class<?>, AtomicLong> traces = MemoryTraced.tracedClasses();
    Map<Class<?>, Long> memoryUsage = MemoryTraced.memoryUsage();
    traces.forEach((aClass, atomicInteger) -> {
      player.sendMessage(atomicInteger + " " + CLASS_NAME.get(aClass) + " (" + humanReadableByteCount(memoryUsage.get(aClass)) + ")");
    });
  }

  @SubCommand(
    selectors = "memtrace2",
    usage = "",
    description = "",
    permission = "sibyl"
  )
  public void memtrace2(User user) {
    Player player = user.player();

    if (!MemoryWatchdog.supported()) {
      player.sendMessage(ChatColor.RED + "类型 2 内存追踪需要 Agent");
      return;
    }

    player.sendMessage(ChatColor.RED + "正在计算内存追踪..");
    Map<String, Long> trace = new HashMap<>();
    MemoryWatchdog.memoryTraceOf(IntavePlugin.singletonInstance(), trace, new HashSet<>());
    trace = sortHashMapByValues(trace);
    trace.forEach((s, aLong) -> {
      if (aLong > 200) {
        player.sendMessage(humanReadableByteCount(aLong) + " 来自 " + (s.contains("intave") ? ChatColor.GRAY : ChatColor.DARK_GRAY) + s);
      }
    });
    player.sendMessage(ChatColor.RED + "正在计算内存占用..");
    player.sendMessage(ChatColor.YELLOW + "插件 Intave 对象内存追踪: " + humanReadableByteCount(MemoryWatchdog.memoryUsageOf(IntavePlugin.singletonInstance(), new HashSet<>())));
    MemoryWatchdog.memoryUsage(stringLongMapx -> {
      Map<String, Long> stringLongMap = sortHashMapByValues(stringLongMapx);
      for (Map.Entry<String, Long> stringLongEntry : stringLongMap.entrySet()) {
        player.sendMessage(stringLongEntry.getKey() + " 需要 " + humanReadableByteCount(stringLongEntry.getValue()));
      }
    });
  }

  public <K extends Comparable<? super K>, V extends Comparable<? super V>> Map<K, V> sortHashMapByValues(
    Map<K, V> passedMap
  ) {
    List<K> mapKeys = new ArrayList<>(passedMap.keySet());
    List<V> mapValues = new ArrayList<>(passedMap.values());
    Collections.sort(mapValues);
    Collections.reverse(mapValues);
    Collections.sort(mapKeys);
    Map<K, V> sortedMap = new LinkedHashMap<>();
    for (V val : mapValues) {
      Iterator<K> keyIt = mapKeys.iterator();
      while (keyIt.hasNext()) {
        K key = keyIt.next();
        if (passedMap.get(key).equals(val)) {
          keyIt.remove();
          sortedMap.put(key, val);
          break;
        }
      }
    }
    return sortedMap;
  }
}
