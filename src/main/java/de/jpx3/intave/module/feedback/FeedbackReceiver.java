package de.jpx3.intave.module.feedback;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.jpx3.intave.IntaveControl;
import de.jpx3.intave.IntaveLogger;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.adapter.ViaVersionAdapter;
import de.jpx3.intave.diagnostic.LatencyStudy;
import de.jpx3.intave.executor.TaskTracker;
import de.jpx3.intave.module.Module;
import de.jpx3.intave.module.Modules;
import de.jpx3.intave.module.linker.packet.ListenerPriority;
import de.jpx3.intave.module.linker.packet.PacketId;
import de.jpx3.intave.module.linker.packet.PacketSubscription;
import de.jpx3.intave.player.FaultKicks;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.ConnectionMetadata;
import de.jpx3.intave.user.meta.MetadataBundle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static de.jpx3.intave.module.feedback.FeedbackSender.*;
import static de.jpx3.intave.module.linker.packet.PacketId.Client.*;

public final class FeedbackReceiver extends Module {
  private static final boolean USE_PING_PACKETS = MinecraftVersions.VER1_17_0.atOrAbove();
  private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(8);
  private static final long TIMEOUT_KICK = TimeUnit.SECONDS.toMillis(40);
  private static final long CHECK_TIMEOUT_KICK = TIMEOUT_KICK / 4;

  public FeedbackReceiver(IntavePlugin plugin) {
    int taskId = plugin.getServer().getScheduler()
      .scheduleAsyncRepeatingTask(plugin, this::checkTransactionTimeout, CHECK_TIMEOUT_KICK, CHECK_TIMEOUT_KICK);
    TaskTracker.begun(taskId);

    int taskId2 = plugin.getServer().getScheduler()
      .scheduleAsyncRepeatingTask(plugin, this::decreaseTAKAVL, 20 * 60, 20 * 60);
    TaskTracker.begun(taskId2);
  }

  private void decreaseTAKAVL() {
    UserRepository.applyOnAll(user -> {
      user.meta().connection().transactionKeepAliveInvalidOrderVL = Math.max(0, user.meta().connection().transactionKeepAliveInvalidOrderVL - 1);
    });
  }

  private void checkTransactionTimeout() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      checkTransactionTimeoutFor(player);
    }
  }

  private void checkTransactionTimeoutFor(Player player) {
    User user = userOf(player);
    ConnectionMetadata connection = user.meta().connection();
    if (oldestPendingTransaction(user) > TIMEOUT_KICK &&
      connection.eligibleForTransactionTimeout &&
      FaultKicks.IGNORING_FEEDBACK
    ) {
      IntaveLogger.logger().error(player.getName() + " 未响应任何反馈数据包");
      user.kick("未响应反馈数据包");
      if (IntaveControl.NETTY_DUMP_ON_TIMEOUT) {
        dumpNettyThreads();
      }
    }
  }

  private void dumpNettyThreads() {
    Thread.getAllStackTraces().forEach((thread, stackTraceElements) -> {
      if (thread.getName().contains("Netty")) {
        boolean containsIntave = false;
        for (StackTraceElement stackTraceElement : stackTraceElements) {
          if (stackTraceElement.getClassName().toLowerCase(Locale.ROOT).contains("intave")) {
            containsIntave = true;
            break;
          }
        }
        if (containsIntave) {
          System.out.println("线程：" + thread.getName());
          Exception exception = new Exception();
          exception.setStackTrace(stackTraceElements);
          exception.printStackTrace();
        }
      }
    });
  }

  @PacketSubscription(
    packetsIn = WINDOW_CLICK
  )
  public void receiveInventoryClick(PacketEvent event) {
    Player player = event.getPlayer();
    User user = userOf(player);
    PacketContainer packet = event.getPacket();
    Short clientTransactionId = packet.getShorts().readSafely(0);
    if (clientTransactionId == null) {
      return;
    }
//    ConnectionMetadata connection = user.meta().connection();
//    connection.windowClickId++;
//    connection.windowClickId %= 250;
//    int start = Short.MAX_VALUE - 250;
//    packet.getShorts().writeSafely(0, (short) (connection.windowClickId + start));
  }

  @PacketSubscription(
    packetsIn = {
      KEEP_ALIVE
    }
  )
  public void onKeepAlive(PacketEvent event) {
    if (!IntaveControl.CLIENT_KEEP_ALIVE_NETTY_CHECK) {
      return;
    }
    Player player = event.getPlayer();
    User user = userOf(player);
    FeedbackQueue feedbackQueue = user.meta().connection().feedbackQueue();
    PacketContainer packet = event.getPacket();
    short possibleUserKey = 0;
    if (MinecraftVersions.VER1_12_0.atOrAbove()) {
      possibleUserKey = packet.getLongs().readSafely(0).shortValue();
    } else {
      possibleUserKey = packet.getIntegers().readSafely(0).shortValue();
    }
    FeedbackRequest<?> peek = feedbackQueue.peek(possibleUserKey);
    if (peek != null) {
      peek.verifyPreThreadInjection();
//      System.out.println("Verified " + possibleUserKey + " for " + player.getName());
    }
  }

  @PacketSubscription(
    packetsOut = {
      PacketId.Server.TRANSACTION, PacketId.Server.PING
    }
  )
  public void outgoingTransaction(PacketEvent event) {
    Player player = event.getPlayer();
    PacketContainer packet = event.getPacket();
    User user = UserRepository.userOf(player);
    boolean noPingMask = user.meta().protocol().noPingMask();
    if (!hasValidUserKey(packet, noPingMask) && activeGenerator != IdGeneratorMode.highestCompatibility()) {
      short userKey = userKeyFrom(packet, noPingMask);
      boolean couldBeWindowClick = userKey >= Short.MAX_VALUE - 250;
      if (couldBeWindowClick) {
        return;
      }
      activeGenerator = IdGeneratorMode.highestCompatibility();
      IntaveLogger.logger().info("检测到玩家 " + player.getName() + " 使用外部事务 ID " + userKey);
      IntaveLogger.logger().info("正在切换到最高兼容性的事务 ID 选择模式");
    }
  }

  @PacketSubscription(
    priority = ListenerPriority.LOWEST,
    packetsIn = {
      TRANSACTION, PONG
    }
  )
  public void receiveAcknowledgementPacket(PacketEvent event) {
    Player player = event.getPlayer();

    // viaversion packet limit workaround
    ViaVersionAdapter.decrementReceivedPackets(player, 2);

    User user = UserRepository.userOf(player);
    if (!user.hasPlayer()) {
      return;
    }
    MetadataBundle meta = user.meta();
    ConnectionMetadata connection = meta.connection();
    FeedbackQueue feedbackQueue = connection.feedbackQueue();
    PacketContainer packet = event.getPacket();
    boolean noPingMask = user.meta().protocol().noPingMask();

    if (!hasValidUserKey(packet, noPingMask)) {
      if (IntaveControl.DEBUG_FEEDBACK_PACKETS) {
        System.out.println("从 " + player.getName() + " 收到 " + packet.getIntegers().readSafely(0) + "，但找不到用户密钥");
      }
      return;
    }

    short userKey = userKeyFrom(packet, noPingMask);
    FeedbackRequest<?> response = feedbackQueue.peek(userKey);
    if (response == null) {
      if (IntaveControl.DEBUG_FEEDBACK_PACKETS) {
        System.out.println("从 " + player.getName() + " 收到 " + userKey + "/" + packet.getIntegers().readSafely(0) + "，但找不到对应请求");
      }
      return;
    }

    long expected = connection.lastReceivedTransactionNum + 1;
    long received = response.num();

    if (IntaveControl.DEBUG_FEEDBACK_PACKETS) {
      System.out.println("预期值：" + expected + "，实际值：" + received);
    }

    if (IntaveControl.DEBUG_FEEDBACK_PACKETS) {
      System.out.println("从 " + player.getName() + " 收到 " + userKey + "/" + response.num());
//      Synchronizer.synchronize(() -> {
//        player.sendMessage("Received " + userKey + "/" +response.num());
//      });
    }

    response.markAcknowledgedByClient();
    for (FeedbackRequest<?> acknowledged : feedbackQueue.pollAcknowledged()) {
      receiveAcknowledgedRequest(user, acknowledged);
    }

    event.setCancelled(true);
  }

  private void receiveAcknowledgedRequest(User user, FeedbackRequest<?> response) {
    ConnectionMetadata connection = user.meta().connection();
    if (IntaveControl.CLIENT_KEEP_ALIVE_NETTY_CHECK) {
      if (!response.preThreadInjectionPassed() && !MinecraftVersions.VER1_12_0.atOrAbove() && !user.meta().protocol().affectedByLevitation()) {
        if (connection.transactionKeepAliveInvalidOrderVL++ > 10) {
//          Violation violation = Violation.builderFor(ProtocolScanner.class)
//            .forPlayer(user.player())
//            .withMessage("invalid transaction/keepalive order")
//            .withDetails("player version: " + user.meta().protocol().versionString())
//            .withVL(1)
//            .build();
//          Modules.violationProcessor().processViolation(violation);
          if (connection.transactionKeepAliveInvalidOrderVL > 20) {
            connection.transactionKeepAliveInvalidOrderVL = 10;
          }
        }
      }
    }

    receiveRequest(user, response);
    long passedTime = response.passedTime();
    connection.receivedTransactionAfter(passedTime);
    Modules.feedbackAnalysis().receivedTransaction(user, response);

//    Balance.BalanceMeta balanceMeta = (Balance.BalanceMeta) user.checkMetadata(Balance.BalanceMeta.class);
//
//    if (balanceMeta.confirmedBalance != Integer.MAX_VALUE) {
//      balanceMeta.timerBalance = Math.max(balanceMeta.timerBalance, balanceMeta.confirmedBalance);
//      balanceMeta.confirmedBalance = Integer.MAX_VALUE;
//    }

//    long nextConfirmedBalance = -passedTimeNs;
//    connection.nextFeedbackSubscribers.add(() -> {
//      balanceMeta.nextConfirmedBalance = nextConfirmedBalance - TimeUnit.MILLISECONDS.toNanos(1);
//    });

    LatencyStudy.receivedTransactionAfter(passedTime);
  }

  private short userKeyFrom(PacketContainer packet, boolean noPingMask) {
    if (USE_PING_PACKETS) {
      int inputInteger = packet.getIntegers().readSafely(0);
      return (short) (inputInteger & 0xffff);
    } else {
      return packet.getShorts().readSafely(0);
    }
  }

  private boolean hasValidUserKey(PacketContainer packet, boolean noPingMask) {
    short shortInput;
    if (USE_PING_PACKETS) {
      int inputInteger = packet.getIntegers().readSafely(0);
      if ((inputInteger & 0xffff0000) != PING_MASK && !noPingMask) {
        return false;
      }
      shortInput = (short) (inputInteger & 0xffff);
    } else {
      shortInput = packet.getShorts().readSafely(0);
    }
    return shortInput <= MAX_USER_KEY && shortInput >= MIN_USER_KEY;
  }

  private void receiveRequest(User user, FeedbackRequest<?> feedbackRequest) {
    Player player = user.player();
    ConnectionMetadata connection = user.meta().connection();
    for (Runnable nextFeedbackSubscriber : connection.nextFeedbackSubscribers) {
      nextFeedbackSubscriber.run();
    }
    connection.nextFeedbackSubscribers.clear();
    connection.lastSynchronization = feedbackRequest.requestedAsNanos();
    connection.lastReceivedTransactionNum = feedbackRequest.num();
    Map<Long, Queue<FeedbackRequest<?>>> appendMap = connection.transactionAppendMap();
    Queue<FeedbackRequest<?>> appendedRequests = appendMap.get(feedbackRequest.num());
    if (appendedRequests != null && !appendedRequests.isEmpty()) {
      for (FeedbackRequest<?> appendedRequest : appendedRequests) {
        acknowledge(player, appendedRequest);
      }
      appendMap.remove(feedbackRequest.num());
    }
    acknowledge(player, feedbackRequest);
  }

  private void acknowledge(Player player, FeedbackRequest<?> feedbackRequest) {
    try {
      feedbackRequest.acknowledge(player);
    } catch (Exception e) {
      if (IntaveControl.DEBUG) {
        IntaveLogger.logger().error("确认目标 " + feedbackRequest.target() + " 的回调 " + feedbackRequest.callback() + " 时出错");
        e.printStackTrace();
      }
    }
  }

  @PacketSubscription(
    priority = ListenerPriority.LOWEST,
    packetsIn = {
      ATTACK_ENTITY, USE_ENTITY
    }
  )
  public void cancelAttacksIfTransactionMissing(PacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    ConnectionMetadata connection = user.meta().connection();
    connection.eligibleForTransactionTimeout = true;
    if (oldestPendingTransaction(user) > TIMEOUT ||
      // Logically, this is part of the PacketDelayer,
      // but I've put this stuff in this if-clause to have a common place for
      // any transaction-related attack cancels
      !connection.enqueuedPackets().isEmpty() ||
      System.currentTimeMillis() - connection.timestampRequiredForAttack < 0
    ) {
      event.setCancelled(true);
    }
  }

  @PacketSubscription(
    priority = ListenerPriority.HIGHEST,
    packetsIn = {
      BLOCK_DIG, BLOCK_PLACE, USE_ITEM
    }
  )
  public void cancelInteractionsOnTimeout(PacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    user.meta().connection().eligibleForTransactionTimeout = true;
    if (oldestPendingTransaction(user) > TIMEOUT * 2) {
      event.setCancelled(true);
    }
  }

  public long oldestPendingTransaction(User user) {
    ConnectionMetadata connection = user.meta().connection();
    FeedbackRequest<?> peek = connection.feedbackQueue().peek();
    return peek == null ? 0 : peek.passedTime();
  }

  public User userOf(Player player) {
    return UserRepository.userOf(player);
  }
}
