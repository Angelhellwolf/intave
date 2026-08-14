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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.PacketFilterManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.access.player.trust.TrustFactor;
import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.block.collision.Collision;
import de.jpx3.intave.check.Check;
import de.jpx3.intave.check.CheckStatistics;
import de.jpx3.intave.cleanup.GarbageCollector;
import de.jpx3.intave.command.CommandStage;
import de.jpx3.intave.command.Optional;
import de.jpx3.intave.command.SubCommand;
import de.jpx3.intave.diagnostic.PacketSynchronizations;
import de.jpx3.intave.diagnostic.timings.Timing;
import de.jpx3.intave.diagnostic.timings.Timings;
import de.jpx3.intave.executor.Synchronizer;
import de.jpx3.intave.math.MathHelper;
import de.jpx3.intave.module.Modules;
import de.jpx3.intave.module.mitigate.AttackNerfStrategy;
import de.jpx3.intave.module.nayoro.Classifier;
import de.jpx3.intave.module.nayoro.Nayoro;
import de.jpx3.intave.module.nayoro.OperationalMode;
import de.jpx3.intave.module.nayoro.event.AttackEvent;
import de.jpx3.intave.module.nayoro.event.BlockPlaceEvent;
import de.jpx3.intave.module.nayoro.event.sink.EventSink;
import de.jpx3.intave.module.test.ChestLootProvider;
import de.jpx3.intave.module.tracker.player.PacketLogging;
import de.jpx3.intave.player.DamageModify;
import de.jpx3.intave.resource.Resource;
import de.jpx3.intave.resource.ResourceRegistry;
import de.jpx3.intave.security.HashAccess;
import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.user.MessageChannel;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.ConnectionMetadata;
import de.jpx3.intave.user.meta.MovementMetadata;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import de.jpx3.intave.user.meta.PunishmentMetadata;
import de.jpx3.intave.user.storage.PlaytimeStorage;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Turtle;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED;

public final class DiagnosticsStage extends CommandStage {
  private static final DateTimeFormatter MESSAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH.mm.ss.SSS");
  private static final DateTimeFormatter FILE_MESSAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");
  private static DiagnosticsStage singletonInstance;
  private final IntavePlugin plugin;

  private final Map<UUID, PacketAdapter> adapterMap = GarbageCollector.watch(new HashMap<>());

  //  private final Set<UUID> damageDebug = GarbageCollector.watch(new HashSet<>());

  private DiagnosticsStage() {
    super(BaseStage.singletonInstance(), "diagnostics");
    plugin = IntavePlugin.singletonInstance();
  }

  private static String threadDumpFileName() {
    return "intave-threaddump-" + LocalDateTime.now().format(FILE_MESSAGE_DATE_FORMATTER).toLowerCase(Locale.ROOT) + ".txt";
  }

  public static DiagnosticsStage singletonInstance() {
    if (singletonInstance == null) {
      singletonInstance = new DiagnosticsStage();
    }
    return singletonInstance;
  }

  @SubCommand(selectors = "branchfreq", usage = "", description = "输出分支频率数据", permission = "intave.command.diagnostics.performance")
  public void branchfreq(User user) {
    Map<String, Long> branchFrequency = user.meta().movement().branchFrequency;
//    sort
    List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(branchFrequency.entrySet());
    sortedEntries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

    Player player = user.player();
    player.sendMessage(ChatColor.GRAY + "分支频率分布:");
    for (Map.Entry<String, Long> entry : sortedEntries) {
      String branchIdentifier = entry.getKey();
      long count = entry.getValue();
      player.sendMessage((count > 0 ? ChatColor.RED + "" + count : ChatColor.GRAY + "0") + ChatColor.GRAY + "x " + ChatColor.WHITE + branchIdentifier);
    }
  }

  @SubCommand(selectors = "environment", usage = "", description = "将环境信息输出到聊天", permission = "intave.command.diagnostics.performance")
  public void environment(CommandSender sender) {
    Player player = null;
    String playerVersion = "";
    if (sender instanceof Player) {
      player = ((Player) sender);
      User user = UserRepository.userOf(player);
      ProtocolMetadata protocol = user.meta().protocol();
      playerVersion = protocol.versionString() + "@" + protocol.protocolVersion();
      sender.sendMessage(ChatColor.GRAY + "玩家版本 " + ChatColor.WHITE + playerVersion);
    } else {
      sender.sendMessage(ChatColor.GRAY + "请在游戏内执行此命令以显示客户端版本");
    }
    String intaveVersion = IntavePlugin.fullVersion();
    String serverVersion = Bukkit.getName() + "@" + Bukkit.getVersion();
    String protocolLibVersion = ProtocolLibrary.getPlugin().getDescription().getVersion();
    sender.sendMessage(ChatColor.GRAY + "服务端软件 Spigot 版本 " + ChatColor.WHITE + serverVersion);
    sender.sendMessage(ChatColor.GRAY + "数据包组件 ProtocolLib 版本 " + ChatColor.WHITE + protocolLibVersion);
    sender.sendMessage(ChatColor.GRAY + "反作弊插件 Intave 版本 " + ChatColor.WHITE + intaveVersion);

    TextComponent message = new TextComponent("[点击复制报告到聊天栏]");
    message.setColor(net.md_5.bungee.api.ChatColor.GRAY);
    message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "Environment: `" + playerVersion + "`,`" + serverVersion + "`,`" + protocolLibVersion + "`,`" + intaveVersion + "`"));

    if (player != null) {
      // Send the message to the player
      player.spigot().sendMessage(message);
    }
  }

  @SubCommand(
    selectors = "nerfers",
    usage = "",
    description = "输出活动削弱",
    permission = "intave.command.diagnostics.performance"
  )
  public void nerfers(User user) {
    List<PunishmentMetadata.AttackNerfer> attackNerfers = user.meta().punishment().activeNerfers();
    if (attackNerfers.isEmpty()) {
      user.player().sendMessage(IntavePlugin.prefix() + ChatColor.RED + "无活动削弱");
    } else {
      user.player().sendMessage(IntavePlugin.prefix() + "活动削弱: " + attackNerfers.stream().map(nerfer -> nerfer.strategy().typeName()).collect(Collectors.joining(", ")));
    }
  }

  private enum Mode {
    PHYSICS_EVAL("pxeval");

    private String name;

    private Mode(String name) {
      this.name = name;
    }

    public String typeName() {
      return this.name;
    }
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
    selectors = "nerf",
    usage = "",
    description = "输出活动削弱",
    permission = "intave.command.diagnostics.performance"
  )
  public void nerf(User user, String type) {
    try {
      AttackNerfStrategy strategy = AttackNerfStrategy.byName(type);
      if (strategy == null) {
        user.player().sendMessage(IntavePlugin.prefix() + ChatColor.RED + "无效削弱类型");
        return;
      }
      user.nerfPermanently(strategy, "command");
      user.player().sendMessage(IntavePlugin.prefix() + "已应用削弱 " + strategy.typeName());
    } catch (Exception exception) {
      user.player().sendMessage(IntavePlugin.prefix() + ChatColor.RED + "无效削弱类型");
    }
  }

  @SubCommand(
    selectors = "entities",
    usage = "",
    description = "输出实体数据",
    permission = "intave.command.diagnostics.performance"
  )
  public void entityCommand(User user) {
    Player player = user.player();

    ConnectionMetadata connection = user.meta().connection();
    int totalEntities = connection.entities().size();
    //    int tickedEntities = connection.tickedEntities().size();
    int tracedEntities = connection.tracedEntities().size();
    player.sendMessage(IntavePlugin.prefix() + "正在监控 " + ChatColor.RED + totalEntities + IntavePlugin.defaultColor() + " 个实体，追踪 " + ChatColor.RED + tracedEntities + IntavePlugin.defaultColor() + " 个实体");
    player.sendMessage(IntavePlugin.prefix() + connection.tracedEntities().stream().map(entity -> entity.entityName() + "/" + entity.entityId()).collect(Collectors.toList()));
  }

  @SubCommand(
    selectors = "turtle",
    usage = "",
    description = "生成海龟",
    permission = "intave.command.diagnostics.performance"
  )
  public void turtleCommand(User user) {
    Player player = user.player();
    if (MinecraftVersions.VER1_20.atOrAbove()) {
      Bukkit.getScheduler().runTask(IntavePlugin.singletonInstance(), () -> {
        Turtle turtle = player.getWorld().spawn(player.getLocation(), Turtle.class);
        turtle.setPassenger(player);
      });
      player.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "已生成海龟");
    } else {
      player.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "失败");
    }
  }

  @SubCommand(selectors = "ntrace", usage = "", description = "采样点击/攻击追踪", permission = "intave.command.diagnostics.performance")
  public void ntraceCommand(User user) {
    Player player = user.player();
    Nayoro nayoro = Modules.nayoro();
    nayoro.pushSink(user, new EventSink() {
      @Override
      public void visit(de.jpx3.intave.module.nayoro.event.ClickEvent event) {
        player.sendMessage("点击事件");
      }

      @Override
      public void visit(AttackEvent event) {
        player.sendMessage("攻击事件");
      }

      @Override
      public void visit(BlockPlaceEvent event) {
        Synchronizer.synchronize(() -> {
          player.sendMessage("方块放置事件{");
          player.sendMessage("  " + event.placedBlock());
          player.sendMessage("  " + event.againstBlock());
          player.sendMessage("  " + event.direction());
          player.sendMessage("  " + event.rotation());
          player.sendMessage("  " + event.eyePosition());
          player.sendMessage("  " + event.endOfRaytrace());
          //        player.sendMessage("  " + event.facingX());
          //        player.sendMessage("  " + event.facingY());
          //        player.sendMessage("  " + event.facingZ());
          player.sendMessage("  " + event.hand());
          player.sendMessage("  " + event.typeName());
          player.sendMessage("  " + event.amountInHand());
          player.sendMessage("}");
        });
      }

      @Override
      public String name() {
        return "ntrace/anonymous";
      }
    });
    player.sendMessage(ChatColor.RED + "已添加实体追踪");
  }

  @SubCommand(
    selectors = "storagetrace",
    usage = "",
    description = "",
    permission = "intave.command.diagnostics.performance"
  )
  public void storageTrace(User user) {
    Player player = user.player();
    PlaytimeStorage storage = user.storageOf(PlaytimeStorage.class);
    if (storage.readTag() != 0) {
      player.sendMessage("正在移除 storage-tag");
      storage.removeDebugTag();
      return;
    }
    player.sendMessage("你已进入存储追踪模式");
    storage.setDebugTag();
    Synchronizer.synchronize(() -> {
      player.sendMessage("你的 storage-tag 为 " + storage.readTag());
    });
  }

  @SubCommand(
    selectors = "playtime",
    usage = "[<target>]",
    description = "",
    permission = "sibyl"
  )
  public void playtimeOf(User user, @Optional Player target) {
    Player player = user.player();
    Player targetPlayer = target == null ? player : target;
    User targetUser = UserRepository.userOf(targetPlayer);
    PlaytimeStorage storage = targetUser.storageOf(PlaytimeStorage.class);
    long minutesPlayed = storage.minutesPlayed();
    long minutesAfk = storage.minutesAfk();
    Synchronizer.synchronize(() -> {
      player.sendMessage("玩家 " + targetPlayer.getName() + " 已游玩 " + minutesPlayed + " 分钟，挂机 " + minutesAfk + " 分钟");
    });
  }

  @SubCommand(
    selectors = "etxtrace",
    usage = "",
    description = "采样点击/攻击追踪",
    permission = "intave.command.diagnostics.performance"
  )
  public void etxTraceCommand(User user) {
    Player player = user.player();
    player.sendMessage(ChatColor.DARK_PURPLE + "已切换实体追踪调试");
    user.meta().connection().debugEntityTracing = !user.meta().connection().debugEntityTracing;
  }

  @SubCommand(
    selectors = {"platrace"},
    usage = "",
    description = "采样攻击 ProtocolLib 追踪",
    permission = "intave.command.diagnostics.performance"
  )
  public void attackTraceCommand(User user) {
    try {
      Player player = user.player();
      ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
      PacketFilterManager packetFilterManager = (PacketFilterManager) protocolManager;
      Field inboundListeners = null;
      try {
        inboundListeners = packetFilterManager.getClass().getDeclaredField("inboundListeners");
        inboundListeners.setAccessible(true);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
//      SortedPacketListenerList sortedPacketListenerList;
//      try {
//        sortedPacketListenerList = (SortedPacketListenerList) inboundListeners.get(packetFilterManager);
//      } catch (IllegalAccessException e) {
//        throw new RuntimeException(e);
//      }
//
//      PacketContainer packet = new PacketContainer(PacketType.Play.Client.USE_ENTITY);
//      packet.getIntegers().write(0, 0);
//      packet.getEntityUseActions().write(0, EnumWrappers.EntityUseAction.ATTACK);
//
//      PacketEvent event = PacketEvent.fromClient(packet.getHandle(), packet, player);
//      Collection<PrioritizedListener<PacketListener>> listeners = sortedPacketListenerList.getListener(PacketType.Play.Client.USE_ENTITY);
//      if (listeners != null) {
//        for (PrioritizedListener<PacketListener> listener : listeners) {
//          listener.getListener().onPacketReceiving(event);
//          player.sendMessage("Listener " + listener.getListener().getClass() + " " + listener.getPriority() + ", cancelled: " + event.isCancelled());
//        }
//      }
    } catch (Exception exception) {
      exception.printStackTrace();
      user.player().sendMessage("数据包组件 ProtocolLib 版本可能无效，错误: " + exception.getMessage());
    }
  }

  @SubCommand(selectors = "damage", usage = "", description = "将攻击伤害输出到聊天", permission = "intave.command.diagnostics.performance")
  public void damageCommand(User user) {
    Player player = user.player();
    Bukkit.getPluginManager().registerEvents(new Listener() {
      @org.bukkit.event.EventHandler
      public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().equals(player)) {
          double baseDamage = event.getDamage(EntityDamageEvent.DamageModifier.BASE);
          double predictedDamage = DamageModify.attackDamageOf(player) + DamageModify.sharpnessDamageOf(player.getInventory().getItemInMainHand());
          boolean probablyCritical = Math.abs(baseDamage * 1.5 - predictedDamage) < 0.01;
          player.sendMessage("造成 " + event.getFinalDamage() + " 伤害" + (probablyCritical ? "（暴击）" : ""));
        }
      }
    }, plugin);
  }

  @SubCommand(selectors = "timings", usage = "", description = "输出计时数据", permission = "intave.command.diagnostics.performance")
  public void timingsCommand(User user, @Optional String[] specifier) {
    Player player = user.player();
    String fullSpecifier = specifier != null ? Arrays.stream(specifier).map(s -> s + " ").collect(Collectors.joining()).trim().toLowerCase(Locale.ROOT) : "";

    player.sendMessage(ChatColor.RED + "正在加载计时...");
    List<Timing> timings = new ArrayList<>(Timings.timingPool());
    timings.sort(Timing::compareTo);

    timings.forEach(timing -> {
      if (timing.isPacketEventTiming() || timing.isBukkitEventTiming()) {
        return;
      }
      boolean suspicious = timing.averageCallDurationInMillis() > 0.5d;
      boolean dumping = timing.averageCallDurationInMillis() > 1.5d;
      String message = String.format("%s: %s::%sms (%s&f ms/c)", timing.coloredName(), timing.recordedCalls(), MathHelper.formatDouble(timing.totalDurationMillis(), 4), (suspicious ? (dumping ? ChatColor.RED : ChatColor.YELLOW) : ChatColor.GREEN) + "" + MathHelper.formatDouble(timing.averageCallDurationInMillis(), 8));
      if (!fullSpecifier.isEmpty() && !timing.name().toLowerCase(Locale.ROOT).contains(fullSpecifier)) {
        message = IntavePlugin.defaultColor() + ChatColor.stripColor(message);
      }
      player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    });
  }

  @SubCommand(
    selectors = "performance",
    usage = "",
    description = "输出性能数据",
    permission = "intave.command.diagnostics.performance"
  )
  public void timingsCommand(CommandSender sender) {
    sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "当前不可用");

    //    sender.sendMessage(IntavePlugin.prefix() + "Service status");
    //    List<Timing> timings = new ArrayList<>(Timings.timingPool());
    //    timings.sort(Timing::compareTo);
    //
    //    timings.forEach(timing -> {
    //      boolean suspicious = timing.getAverageCallDurationInMillis() > 0.5d;
    //      boolean dumping = timing.getAverageCallDurationInMillis() > 1.5d;
    //      String type;
    //      if (suspicious) {
    //        type = ChatColor.GOLD + "SUSPICIOUS";
    //      } else if (dumping) {
    //        type = ChatColor.RED + "CRITICAL";
    //      } else {
    //        type = ChatColor.GREEN + "HEALTHY";
    //      }
    //      String message = type + " " + ChatColor.GRAY + timing.getTimingName();
    //      sender.sendMessage(message);
    //    });
  }

  @SubCommand(
    selectors = "fireball",
    usage = "",
    description = "火球弹射",
    permission = "intave.command.diagnostics.performance"
  )
  public void fireballCommand(User user) {
    Player player = user.player();

//    val gamer = Gamer.getGamer(player);

//    if (gamer == null || gamer.isSpectator()) {
//      return;
//    }

//    val damagerPlayer = game.getMetadata().<Player>get(damager, "owner");
//    if (!game.getSetting(GameSetting.FIREBALL_FRIENDLY_FIRE, true)) {
//      val team = game.getTeamManager().getTeam(gamer);
//
//      if (team != null) {
//        val damagerGamer = Gamer.getGamer(damagerPlayer);
//
//        if (damagerGamer != null && !damagerGamer.equals(gamer) && team.contains(damagerGamer)) {
//          return;
//        }
//      }
//    }

    player.damage(0);

    double multiply = 1.5;
    Vector vector = getPosition(player, player.getLocation(), player.getLocation().clone().add(0, 0.5, 0), 1.2);

    vector.setX(vector.getX() * multiply);
//    if (game.getSetting(GameSetting.MULTIPLY_Y_VELOCITY, false)) {
    vector.setY(vector.getY() * multiply);
//    }
    vector.setZ(vector.getZ() * multiply);

    player.setVelocity(vector);
  }

  private static Vector getPosition(Entity entity, Location location1, Location location2, double defaultY) {
    double distance = location1.distance(location2);

    double x = location1.getX() - location2.getX();
    double y = (defaultY / distance);
    double z = location1.getZ() - location2.getZ();

    double distanceForce = Math.min(1.2, 0.9 / distance);

    double finalX = x * distanceForce;
    double finalY = entity.isOnGround() ? (distance <= 1.5 ? defaultY : (y * 1.2)) : defaultY;
    double finalZ = z * distanceForce;

    return new Vector(finalX, finalY, finalZ);
  }

  @SubCommand(
    selectors = "resistance",
    usage = "",
    description = "给全服玩家抗性",
    permission = "intave.command.diagnostics.performance"
  )
  public void resistanceCommand(User user) {
    Bukkit.getOnlinePlayers().forEach(player -> {
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 999999, 100));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 999999, 100));
      }
    );
  }


  @SubCommand(
    selectors = "teleportspam",
    usage = "",
    description = "连续传送自己",
    permission = "intave.command.diagnostics.performance"
  )
  public void teleportSpam(User user) {
    Player player = user.player();
    MovementMetadata movement = user.meta().movement();
    player.sendMessage(ChatColor.RED + "退出登录以停止");

    int[] id = {0};
    id[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      if (!player.isOnline()) {
        Bukkit.getScheduler().cancelTask(id[0]);
        return;
      }
      int attempts = 100;
      BoundingBox playerBox = BoundingBox.fromPosition(
        user, movement, player.getLocation()
      );
      double moveX;
      do {
        moveX = ThreadLocalRandom.current().nextGaussian();
      } while (Collision.present(user, movement, playerBox.move(moveX, 0, 0)) && attempts-- > 0);
      if (attempts <= 0) moveX = 0;
      attempts = 100;
      double moveY;
      do {
        moveY = ThreadLocalRandom.current().nextGaussian();
      } while (Collision.present(user, movement, playerBox.move(0, moveY, 0)) && attempts-- > 0);
      double moveZ;
      do {
        moveZ = ThreadLocalRandom.current().nextGaussian();
      } while (Collision.present(user, movement, playerBox.move(0, 0, moveZ)) && attempts-- > 0);
      if (attempts <= 0) moveZ = 0;
      if (attempts <= 0) moveY = 0;
      player.teleport(player.getLocation().clone().add(moveX, moveY, moveZ));

      if (user.receives(MessageChannel.DEBUG_TELEPORT)) {
        player.sendMessage(IntavePlugin.prefix() + "传送至 " + player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ() + " " + " 原因: " + ChatColor.RED + " 命令请求");
      }
    }, 20, 3);
  }

  @SubCommand(
    selectors = "velocityspam",
    usage = "",
    description = ""
  )
  public void velocitySpam(User user) {
    Player player = user.player();
    player.sendMessage(ChatColor.RED + "退出登录以停止");

    int[] id = {0};
    id[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      if (!player.isOnline()) {
        Bukkit.getScheduler().cancelTask(id[0]);
        return;
      }
      player.setVelocity(new Vector(
        ThreadLocalRandom.current().nextGaussian() * 0.2,
        0.3,
        ThreadLocalRandom.current().nextGaussian() * 0.2
      ));
    }, 20, 20 * 2);
  }

  @SubCommand(
    selectors = "flyingswitch",
    usage = "",
    description = "连续传送自己",
    permission = "intave.command.diagnostics.performance"
  )
  public void flyingSwitch(User user) {
    Player player = user.player();
    player.sendMessage(ChatColor.RED + "退出登录以停止");

    int[] id = {0};
    id[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      if (!player.isOnline()) {
        Bukkit.getScheduler().cancelTask(id[0]);
        return;
      }
      boolean canFly = player.getAllowFlight();
      Synchronizer.synchronizeDelayed(() -> player.setAllowFlight(!canFly), 40);
      player.sendMessage(IntavePlugin.prefix() + "飞行将在 2 秒后" + ChatColor.RED + (!canFly ? "启用" : "关闭"));
    }, 20, 20 * 10);
  }

  @SubCommand(
    selectors = "walkspeed",
    usage = "",
    description = "设置行走速度",
    permission = "intave.command.diagnostics.performance"
  )
  public void walkSpeed(User user, @Optional Double speed, @Optional WalkSpeedMethod method) {
    if (speed == null) {
      speed = 0.1d;
    }
    if (method == null) {
      method = WalkSpeedMethod.ATTRIBUTE;
    }
    Player player = user.player();

    if (method == WalkSpeedMethod.ATTRIBUTE) {
      player.getAttribute(GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
    } else {
      player.setWalkSpeed(speed.floatValue());
    }
  }

  public static enum WalkSpeedMethod {
    DIRECT,
    ATTRIBUTE
  }

  @SubCommand(
    selectors = "vehicleboost",
    usage = "",
    description = "加速你的载具",
    permission = "intave.command.diagnostics.performance"
  )
  public void boostVehicle(User user) {
    Player player = user.player();
    LivingEntity strider = (LivingEntity) player.getVehicle();

    int duration = 100;
    if (strider.hasPotionEffect(PotionEffectType.SPEED)) {
      duration += strider.getPotionEffect(PotionEffectType.SPEED).getDuration();
    }
    strider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2, false, false));
  }

  @SubCommand(
    selectors = "simnofeedback",
    usage = "",
    description = "临时忽略反馈包",
    permission = "intave.command.diagnostics.performance"
  )
  public void simulateNoFeedback(User user) {
    Player player = user.player();
    UUID userId = player.getUniqueId();

    player.sendMessage(ChatColor.RED + "需等待一分钟后才能再次获取反馈。");
    PacketAdapter adapter = new PacketAdapter(IntavePlugin.singletonInstance(), PacketType.Play.Server.TRANSACTION, PacketType.Play.Server.PING) {
      final long timeout = System.currentTimeMillis() + 60000;

      @Override
      public void onPacketSending(PacketEvent event) {
        if (System.currentTimeMillis() > timeout) {
          ProtocolLibrary.getProtocolManager().removePacketListener(this);
          adapterMap.remove(userId);

          Player blayer = Bukkit.getPlayer(userId);
          if (blayer.isOnline()) {
            blayer.sendMessage(IntavePlugin.prefix() + ChatColor.GREEN + "现在可以再次获取反馈。");
          }
          return;
        }
        event.setCancelled(true);
      }

      @Override
      public void onPacketReceiving(PacketEvent event) {
      }
    };
    adapterMap.put(userId, adapter);
    ProtocolLibrary.getProtocolManager().addPacketListener(adapter);
  }

  @SubCommand(
    selectors = "resync",
    usage = "",
    permission = "intave.command.diagnostics.statistics",
    description = "输出数据包重同步"
  )
  public void checkPacketResync(CommandSender sender) {
    sender.sendMessage(IntavePlugin.prefix() + "正在加载数据..");
    Map<String, Long> packets = PacketSynchronizations.output();
    if (packets.isEmpty()) {
      sender.sendMessage(ChatColor.GREEN + "暂无硬重同步记录");
    } else {
      packets = sortHashMapByValues(packets);
      packets.forEach((name, hardsResyncs) -> sender.sendMessage(ChatColor.RED + name.toLowerCase(Locale.ROOT) + IntavePlugin.defaultColor() + " 包共触发 " + ChatColor.RED + hardsResyncs + IntavePlugin.defaultColor() + " 次硬重同步"));
    }
  }

  public <K extends Comparable<? super K>, V extends Comparable<? super V>> Map<K, V> sortHashMapByValues(Map<K, V> passedMap) {
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

  @SubCommand(
    selectors = "resources",
    usage = "",
    permission = "intave.command.diagnostics.performance",
    description = ""
  )
  public void resourceStatus(CommandSender sender) {
    sender.sendMessage(IntavePlugin.prefix() + "资源");
    ResourceRegistry.registeredResources().forEach((identifier, resource) ->
      sender.sendMessage(IntavePlugin.prefix() + " " + identifier.substring(0, 2) + " -> " + HashAccess.readHashFromStream(resource.read()))
    );
  }

  @SubCommand(
    selectors = "invalidatecaches",
    usage = "",
    permission = "intave.command.diagnostics.performance",
    description = ""
  )
  public void cacheInvalidate(CommandSender sender) {
    sender.sendMessage(IntavePlugin.prefix() + "正在使缓存失效..");
    for (Resource value : ResourceRegistry.registeredResources().values()) {
      value.delete();
    }
    sender.sendMessage(IntavePlugin.prefix() + "完成，请重启 Intave");
  }

  @SubCommand(
    selectors = "threaddump",
    usage = "",
    permission = "intave.command.diagnostics.statistics",
    description = "创建并保存线程转储"
  )
  public void createThreadDump(CommandSender sender) {
    File dumpsFolder = new File(plugin.dataFolder(), "dumps");
    File threadDumpFile = new File(dumpsFolder, threadDumpFileName());

    try {
      dumpsFolder.mkdir();
      threadDumpFile.createNewFile();
    } catch (IOException exception) {
      exception.printStackTrace();
      return;
    }

    try {
      FileOutputStream stream = new FileOutputStream(threadDumpFile);
      PrintStream printStream = new PrintStream(stream);
      printStream.println("Static environment");
      printStream.println(" Time: " + LocalDateTime.now().format(MESSAGE_DATE_FORMATTER));
      printStream.println(" Intave: " + IntavePlugin.fullVersion());
      printStream.println(" ProtocolLib: " + Bukkit.getPluginManager().getPlugin("ProtocolLib").getDescription().getVersion());
      if (Bukkit.getPluginManager().getPlugin("ViaVersion") != null) {
        printStream.println(" ViaVersion: " + Bukkit.getPluginManager().getPlugin("ViaVersion").getDescription().getVersion());
      } else {
        printStream.println(" ViaVersion not present");
      }
      printStream.println(" Server: "/* + Bukkit.getServerName() + "/"*/ + Bukkit.getVersion() + "/" + Bukkit.getBukkitVersion());
      printStream.println(" Minecraft: " + MinecraftVersion.current().toString());
      printStream.println("Players");
      printStream.println(" Thread dump creator: " + sender.getName());
      printStream.println(" Players online: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
      printStream.println(" ");
      Thread.getAllStackTraces().forEach((thread, stackTraceElements) -> {
        String threadName = thread.getName();
        if (threadName.contains("Netty") || threadName.contains("Intave") || threadName.contains("Server thread")) {
          printStream.println("Thread " + threadName);
          Exception exception = new Exception();
          exception.setStackTrace(stackTraceElements);
          exception.printStackTrace(printStream);
        }
      });
      printStream.flush();
      printStream.close();
    } catch (FileNotFoundException exception) {
      exception.printStackTrace();
    }
    sender.sendMessage(IntavePlugin.prefix() + ChatColor.GREEN + "线程转储已创建");
    sender.sendMessage(IntavePlugin.prefix() + "文件位置: " + threadDumpFile.getAbsolutePath());
  }

  @SubCommand(selectors = {"packetlog", "pl"}, usage = "[<target>]", permission = "intave.command.diagnostics.statistics", description = "创建并保存数据包日志")
  public void startPacketLog(CommandSender sender, Player target) {
    Synchronizer.synchronize(() -> {
      Modules.find(PacketLogging.class).togglePacketLogging(sender, target);
    });
  }

//  @SubCommand(selectors = "packetlogupload", usage = "", permission = "intave.command.diagnostics.statistics", description = "Upload packet logs")
//  public void uploadPacketLog(CommandSender sender) {
//    sender.sendMessage(IntavePlugin.prefix() + "Uploading packet logs...");
//    File logsFolder = new File(plugin.dataFolder(), "packetlogs");
//    File[] files = logsFolder.listFiles();
//    if (files == null) {
//      sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "No packet logs found");
//      return;
//    }
//    // get newest file
//    Arrays.sort(files, Comparator.comparingLong(File::lastModified));
//    File packetLogFile = files[files.length - 1];
//    BackgroundExecutors.executeWhenever(() -> {
//      upload(packetLogFile, sender);
//    });
//  }

  private void upload(File file, CommandSender sender) {
    try {
      // upload to anonfile
      URL url = new URL("https://api.anonfiles.com/upload");

      String boundary = Long.toHexString(System.currentTimeMillis());
      URLConnection connection = url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      try (
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))
      ) {
        writer.println("--" + boundary);
        writer.println("Content-Disposition: form-data; name=file; filename=\"" + file.getName() + "\"");
        writer.println("Content-Type: text/plain; charset=UTF-8");
        writer.println();
        try (
          BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))
        ) {
          for (String line; (line = reader.readLine()) != null; ) {
            writer.println(line);
          }
        }
        writer.println("--" + boundary + "--");
      }

      connection.connect();

      HttpsURLConnection httpsURLConnection = (HttpsURLConnection) connection;
      int responseCode = httpsURLConnection.getResponseCode();

      if (responseCode != 200) {
        sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "上传失败");
        return;
      }

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()))) {
        String str = reader.lines().collect(Collectors.joining("\n"));
        try {
          JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
          String url1 = jsonObject.getAsJsonObject("data").getAsJsonObject("file").getAsJsonObject("url").get("short").getAsString();
          sender.sendMessage(IntavePlugin.prefix() + ChatColor.GREEN + "已上传至 " + url1);
        } catch (Exception exception) {
          exception.printStackTrace();
          sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "上传失败");
          System.out.println(str);
        }
        //        System.out.println(str);
      }

    } catch (IOException exception) {
      exception.printStackTrace();
      sender.sendMessage(IntavePlugin.prefix() + ChatColor.RED + "上传失败");
    }
  }

  @SubCommand(selectors = "statistics", usage = "", permission = "intave.command.diagnostics.statistics", description = "输出检测统计")
  public void checkStatisticsCommand(CommandSender sender) {
    sender.sendMessage(IntavePlugin.prefix() + "正在加载检测统计...");
    List<Check> checks = new ArrayList<>(plugin.checks().checks());
    checks.sort(Comparator.comparing(check -> check.baseStatistics().totalFails()));
    boolean output = false;
    for (Check check : checks) {
      CheckStatistics statistics = check.baseStatistics();
      long processed = statistics.totalProcessed();
      long violations = statistics.totalViolations();
      if (processed == 0 || !check.enabled()) {
        continue;
      }
      String violatedRate = MathHelper.formatDouble((((double) violations / (double) processed)) * 100d, 5);
      String checkFormat = ChatColor.RED + check.name();
      String message = checkFormat + IntavePlugin.defaultColor() + ": " + processed + " 次处理中检出 " + violations + " 次（" + violatedRate + "%）";
      sender.sendMessage(message);
      output = true;
    }
    if (!output) {
      sender.sendMessage(IntavePlugin.prefix() + "暂无检测统计");
    }
  }

  @SubCommand(
    selectors = "lootchest",
    usage = "",
    description = "打开战利品箱",
    permission = "intave.command.diagnostics.performance"
  )
  public void onLootChest(User user) {
    ChestLootProvider provider = Modules.find(ChestLootProvider.class);
    if (!user.player().isOp()) {
      user.player().sendMessage(IntavePlugin.prefix() + ChatColor.RED + "需要 OP 权限才能使用此命令");
      return;
    }
    provider.openLootChestCommand(user.player());
  }

  @SubCommand(
    selectors = "samplerecord",
    usage = "",
    description = "",
    permission = "intave.command.diagnostics.performance"
  )
  public void sampleRecord(User user, @Optional Classifier classifier) {
    Nayoro nayoro = Modules.nayoro();

    if (nayoro.recordingActiveFor(user)) {
      nayoro.disableRecordingFor(user);
      user.player().sendMessage(IntavePlugin.prefix() + ChatColor.RED + "已停止录制");
    } else {
      if (classifier == null) {
        user.player().sendMessage(IntavePlugin.prefix() + ChatColor.RED + "请指定分类标签");
        return;
      }
      nayoro.enableRecordingFor(user, Classifier.UNKNOWN, OperationalMode.LOCAL_STORAGE);
      user.player().sendMessage(IntavePlugin.prefix() + ChatColor.GREEN + "已开始录制");
    }
  }
}
