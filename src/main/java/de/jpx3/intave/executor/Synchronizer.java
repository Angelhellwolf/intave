package de.jpx3.intave.executor;

import de.jpx3.intave.IntaveLogger;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.access.UnsupportedFallbackOperationException;
import de.jpx3.intave.diagnostic.timings.Timings;
import de.jpx3.intave.klass.Lookup;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Queue;
import java.util.concurrent.Executor;

public final class Synchronizer {
  private static final BukkitScheduler scheduler = Bukkit.getScheduler();
  private static Executor synchronizationExecutor;

  public static void setup() {
    try {
      Class<?> minecraftServerClass = Lookup.serverClass("MinecraftServer");
      Object minecraftServer = minecraftServerClass.getMethod("getServer").invoke(null);
      //noinspection unchecked
      Queue<Runnable> cachedProcessQueue = (Queue<Runnable>) minecraftServerClass.getField("processQueue").get(minecraftServer);
      synchronizationExecutor = cachedProcessQueue::add;
    } catch (NoSuchFieldException exception) {
      IntavePlugin.singletonInstance().logger().error("当前 Spigot 版本已移除任务排队支持，将切换到 Bukkit 调度服务");
      synchronizationExecutor = command -> scheduler.runTask(IntavePlugin.singletonInstance(), command);
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  public static void synchronize(Runnable runnable) {
    synchronizationExecutor.execute(wrapped(runnable));
  }

  public static void synchronizeDelayed(Runnable runnable, int ticks) {
    runnable = wrapped(runnable);
    scheduler.runTaskLater(IntavePlugin.singletonInstance(), runnable, ticks);
  }

  private static Runnable wrapped(Runnable runnable) {
    return () -> {
      try {
        Timings.EXE_SERVER.start();
        runnable.run();
      } catch (UnsupportedFallbackOperationException fallbackOp) {
        IntaveLogger.logger().info("任务 " + runnable + " 执行失败，因为关联玩家已经离线");
      } catch (Exception | Error throwable) {
        IntaveLogger.logger().error("服务器任务执行失败：" + runnable);
        throwable.printStackTrace();
      } finally {
        Timings.EXE_SERVER.stop();
      }
    };
  }
}
