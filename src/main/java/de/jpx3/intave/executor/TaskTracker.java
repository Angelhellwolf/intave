package de.jpx3.intave.executor;

import de.jpx3.intave.IntaveLogger;
import de.jpx3.intave.cleanup.ShutdownTasks;
import io.netty.util.internal.ConcurrentSet;
import org.bukkit.Bukkit;

import java.util.Set;

public final class TaskTracker {
  private static final Set<Integer> runningTasks = new ConcurrentSet<>();

  public static void setup() {
    ShutdownTasks.add(TaskTracker::stopAll);
  }

  public static void begun(int taskId) {
    runningTasks.add(taskId);
    if (runningTasks.size() > 64) {
      IntaveLogger.logger().error("Intave 创建的任务过多，正在关闭最后一个任务以将任务数保持在 64 以下");
      Thread.dumpStack();
      Bukkit.getScheduler().cancelTask(taskId);
    }
  }

  public static void stopped(int taskId) {
    runningTasks.remove(taskId);
  }

  public static void stopAll() {
    for (Integer runningTask : runningTasks) {
      Bukkit.getScheduler().cancelTask(runningTask);
    }
    runningTasks.clear();
  }
}
