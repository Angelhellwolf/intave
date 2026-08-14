package de.jpx3.intave.adapter;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import de.jpx3.intave.IntaveLogger;
import de.jpx3.intave.access.InvalidDependencyException;
import org.bukkit.Bukkit;

import java.util.Arrays;

public final class ProtocolLibraryAdapter {
  private static final String PROTOCOLLIB_OUTDATED = "ProtocolLib 版本过旧";

  @Deprecated
  public static MinecraftVersion serverVersion() {
    return MinecraftVersion.current();
  }

  public static boolean protocolLibAvailable() {
    return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
  }

  public static void checkIfOutdated() {
    boolean temporaryPlayer = Arrays.stream(PacketEvent.class.getMethods()).anyMatch(method -> method.getName().equalsIgnoreCase("isPlayerTemporary"));
    boolean specifiedEnumModifier = Arrays.stream(EnumWrappers.class.getMethods()).anyMatch(method -> method.getName().equalsIgnoreCase("getGenericConverter") && method.getParameterCount() == 2);
    boolean byteBuddyExists = classExists("com.comphenix.net.bytebuddy.ByteBuddy");

    if (!specifiedEnumModifier) {
      throw new InvalidDependencyException(PROTOCOLLIB_OUTDATED + "（缺少通用枚举转换）");
    }

    try {
      Class<?> minecraftKeyClass = MinecraftReflection.getMinecraftKeyClass();
    } catch (Throwable throwable) {
      throw new InvalidDependencyException(PROTOCOLLIB_OUTDATED + "（缺少 MinecraftKey 类引用）");
    }

    if (!methodExists(MinecraftVersion.class.getName(), "atOrAbove")) {
      throw new InvalidDependencyException(PROTOCOLLIB_OUTDATED + "（缺少 atOrAbove 检查）");
    }

    if (!methodExistsInClassHierarchy(PacketContainer.class.getName(), "getMinecraftKeys")) {
      throw new InvalidDependencyException(PROTOCOLLIB_OUTDATED + "（缺少 MinecraftKey 访问能力）");
    }

    if (MinecraftVersions.VER1_14_0.atOrAbove()) {
      if (!methodExistsInClassHierarchy("com.comphenix.protocol.events.PacketContainer", "getMovingBlockPositions")) {
        throw new InvalidDependencyException(PROTOCOLLIB_OUTDATED + "（缺少移动对象位置数据包访问能力）");
      }
    }

    if (MinecraftVersions.VER1_17_0.atOrAbove()) {
      if (!methodExistsInClassHierarchy(PacketContainer.class.getName(), "getEnumEntityUseActions")) {
        throw new InvalidDependencyException(PROTOCOLLIB_OUTDATED + "（缺少实体交互动作枚举访问能力）");
      }

      if (MinecraftVersions.VER1_17_1.atOrAbove()) {
        if (!methodExistsInClassHierarchy(PacketContainer.class.getName(), "getIntLists")) {
          throw new InvalidDependencyException(PROTOCOLLIB_OUTDATED + "（缺少整数列表访问能力）");
        }
      }
    }

    if (!temporaryPlayer || !byteBuddyExists) {
      IntaveLogger.logger().info("建议更新 ProtocolLib");
    }
  }

  private static boolean classExists(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException exception) {
      return false;
    }
  }

  private static boolean methodExistsInClassHierarchy(String className, String methodName) {
    try {
      Class<?> rootClass = Class.forName(className);
      do {
        if (methodExists(rootClass.getName(), methodName)) {
          return true;
        }
      } while ((rootClass = rootClass.getSuperclass()) != Object.class);
      return false;
    } catch (ClassNotFoundException exception) {
      return false;
    }
  }

  private static boolean methodExists(String className, String methodName) {
    try {
      Class.forName(className).getDeclaredMethod(methodName);
      return true;
    } catch (Exception exception) {
      return false;
    }
  }
}
