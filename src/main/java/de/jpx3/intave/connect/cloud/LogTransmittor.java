package de.jpx3.intave.connect.cloud;

import de.jpx3.intave.IntaveLogger;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.cleanup.ShutdownTasks;
import de.jpx3.intave.cleanup.StartupTasks;
import de.jpx3.intave.diagnostic.ConsoleOutput;
import de.jpx3.intave.executor.TaskTracker;
import de.jpx3.intave.module.Modules;
import de.jpx3.intave.module.linker.bukkit.BukkitEventSubscriber;
import de.jpx3.intave.module.linker.bukkit.BukkitEventSubscription;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.function.Consumer;

public final class LogTransmittor implements BukkitEventSubscriber {
  private final Cloud cloud = IntavePlugin.singletonInstance().cloud();
  private final Map<UUID, LogState> logStates = new HashMap<>();
  private final LogState intaveLogState = new LogState();
  private int scheduledUploadLogTaskId;
  private int logIdRequestFailedUpdateScheduleTaskId;

  public void addPlayerLog(Player player, String line) {
    LogState logState = logStateOf(player);
    // HH:MM:SS
    String dateFormatted = "[" + new Date().toString().split(" ")[3] + "] ";
    logState.addLine(dateFormatted + ChatColor.stripColor(line));
  }

  public void addIntaveLog(String line) {
//    intaveLogState.addLine(line);
  }

  public void init() {
    if (scheduledUploadLogTaskId == 0) {
      scheduledUploadLogTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(IntavePlugin.singletonInstance(), this::uploadAll, 20 * 60 * 5, 20 * 60 * 5).getTaskId();
      TaskTracker.begun(scheduledUploadLogTaskId);
    }
    if (logIdRequestFailedUpdateScheduleTaskId == 0) {
      logIdRequestFailedUpdateScheduleTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(IntavePlugin.singletonInstance(), () -> {
        for (LogIdRequest request : requests) {
          if (request.fulfilled) {
            requests.remove(request);
          } else if (request.timedOut()) {
            request.fulfill("日志 ID 不可用");
            requests.remove(request);
          }
        }
      }, 20 * 5, 20 * 5).getTaskId();
      TaskTracker.begun(logIdRequestFailedUpdateScheduleTaskId);
    }
    StartupTasks.add(() -> Modules.linker().bukkitEvents().registerEventsIn(this));
    ShutdownTasks.add(this::uploadAll);
  }

  private void uploadAll() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      uploadLogOf(player);
    }
  }

  @BukkitEventSubscription
  public void onPlayerQuit(PlayerQuitEvent quit) {
    Player player = quit.getPlayer();
    UUID uuid = player.getUniqueId();
    uploadLogOf(player, logId -> {
      if (ConsoleOutput.CLOUD_LOG_IDS) {
        Player player1 = Bukkit.getPlayer(uuid);
        if (player1 != null &&player1.isOnline()) {
          IntaveLogger.logger().info("已为 " + player1.getName() + " 分配日志 ID：" + logId);
        }
      }
    });
  }

  private LogState logStateOf(Player player) {
    return logStates.computeIfAbsent(player.getUniqueId(), uuid -> new LogState());
  }

  private final List<LogIdRequest> requests = new LinkedList<>();

  public void awaitLogIdOf(Player player, Consumer<? super String> logIdCallback) {
    if (cloud.available()) {
      LogIdRequest request = new LogIdRequest(logIdCallback);
      requests.add(request);
      logIdCallback = request;
      if (logStateOf(player).logId() != null) {
        logIdCallback.accept(logStateOf(player).logId());
        return;
      }
      uploadLogOf(player, logIdCallback);
    } else {
      logIdCallback.accept("无日志 ID");
    }
  }

  private static class LogIdRequest implements Consumer<String> {
    private static int NONCE = 0;
    private final int nonce;
    private final long requestTime = System.currentTimeMillis();
    private boolean fulfilled;
    private final Consumer<? super String> callback;

    public LogIdRequest(Consumer<? super String> callback) {
      this.callback = callback;
      this.nonce = NONCE++;
    }

    public void fulfill(String logId) {
      if (fulfilled) {
        return;
      }
      fulfilled = true;
      callback.accept(logId);
    }

    @Override
    public void accept(String o) {
      if (o != null) {
        fulfill(o);
      } else {
        throw new IllegalArgumentException("Expected String, got " + null);
      }
    }

    public boolean timedOut() {
      return System.currentTimeMillis() - requestTime > 1000 * 10;
    }

    public int nonce() {
      return nonce;
    }
  }

  private void uploadLogOf(Player player) {
    uploadLogOf(player, logId -> {});
  }

  private void uploadLogOf(Player player, Consumer<? super String> logIdCallback) {
    LogState logState = logStateOf(player);
    if (logState.logId() != null) {
      logIdCallback.accept(logState.logId());
    }
    cloud.uploadPlayerLogs(
      player,
      logState.currentNonce(),
      new ArrayList<>(logState.pendingLogs()),
      logId -> {
        if (logState.logId() == null) {
          logIdCallback.accept(logId);
        }
        logState.setLogId(logId);
      }
    );
    logState.nextNonce();
    logState.clearPendingLogs();
  }

  public static class LogState {
    private static int NONCE = 0;
    private final List<String> pendingLogs = new LinkedList<>();
    private String logId;
    private int currentNonce;
    private long lastLogUploadAttempt = System.currentTimeMillis();
    private boolean currentlyUploading;

    public LogState() {
    }

    public boolean isCurrentlyUploading() {
      return currentlyUploading;
    }

    public void setCurrentlyUploading(boolean currentlyUploading) {
      this.currentlyUploading = currentlyUploading;
    }

    public boolean wasUploadedRecently() {
      return System.currentTimeMillis() - lastLogUploadAttempt < 1000 * 60 * 5;
    }

    public void setLastLogUploadAttempt(long lastLogUploadAttempt) {
      this.lastLogUploadAttempt = lastLogUploadAttempt;
    }

    public int currentNonce() {
      return currentNonce;
    }

    public void nextNonce() {
      currentNonce = NONCE++;
    }

    private List<String> pendingLogs() {
      return pendingLogs;
    }

    public void addLine(String line) {
      pendingLogs.add(line);
    }

    public void clearPendingLogs() {
      pendingLogs.clear();
    }

    public void setLogId(String logId) {
      this.logId = logId;
    }

    public String logId() {
      return logId;
    }
  }
}
