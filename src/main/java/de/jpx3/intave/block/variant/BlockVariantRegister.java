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

package de.jpx3.intave.block.variant;

import de.jpx3.intave.IntaveControl;
import de.jpx3.intave.IntaveLogger;
import de.jpx3.intave.block.variant.index.VariantIndex;
import de.jpx3.intave.cleanup.ReferenceMap;
import de.jpx3.intave.cleanup.StartupTasks;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BlockVariantRegister {
  private static final Map<Material, Map<Object, Integer>> blockDataIndex = new EnumMap<>(Material.class);
  private static final Map<Material, Map<Integer, Object>> blockDataRegister = new EnumMap<>(Material.class);
  private static final Map<Material, Map<Integer, BlockVariant>> blockVariants = ReferenceMap.soft(new EnumMap<>(Material.class));

  public static void index() {
    for (Material type : Material.values()) {
      if (type.isBlock()) {
        VariantIndex.indexApplyWithReverse(type, blockDataIndex::put, blockDataRegister::put);
      }
    }
    int count = 0;
    int blockCount = 0;
    for (Map<Object, Integer> index : blockDataIndex.values()) {
      count += index.size();
      blockCount++;
    }
    if (IntaveControl.DEBUG_VARIANT_COMPILATION) {
      System.out.println("[variant/debug] 已索引 " + blockCount + " 种方块的 " + count + " 个变体");
    }
    // After initialization, we usually don't need most of the cache anymore
    // So we can clear it after startup to lower memory usage
    StartupTasks.add(BlockVariantRegister::invalidateShadowedVariantCache);
  }

  static final BlockVariant EMPTY_ERROR = new EmptyBlockVariant();

  public static boolean isIndexed(Material type) {
    return blockDataRegister.containsKey(type);
  }

  // Note: Caching all materials will become quite memory-intensive.
  //       Only pass in materials that are actually used, always filter random materials!
  public static BlockVariant variantOf(Material type, int variantIndex) {
    Map<Integer, BlockVariant> variantMap = blockVariants.computeIfAbsent(type, BlockVariantRegister::translateFromServer);
    BlockVariant variant = variantMap.get(variantIndex);
    if (variant == null) {
      IntaveLogger.logger().error("找不到方块类型 " + type + " 的变体索引 " + variantIndex + "（映射大小：" + variantMap.size() + "）");
      return EMPTY_ERROR;
    }
    return variant;
  }

  public static BlockVariant uncachedVariantOf(Material type, int variantIndex) {
    Map<Integer, BlockVariant> variantMap = translateFromServer(type);
    BlockVariant variant = variantMap.get(variantIndex);
    if (variant == null) {
      IntaveLogger.logger().error("找不到方块类型 " + type + " 的变体索引 " + variantIndex + "（映射大小：" + variantMap.size() + "）");
      return EMPTY_ERROR;
    }
    return variant;
  }

  private static Map<Integer, BlockVariant> translateFromServer(Material material) {
    Map<Integer, BlockVariant> map = BlockVariantConverter.translateVariants(material, blockDataRegister.get(material));
    if (IntaveControl.DEBUG_VARIANT_COMPILATION) {
      System.out.println("[variant/debug] 已为 " + material + " 编译 " + map.size() + " 个变体");
      System.out.println("[variant/debug] 零值变体：");
      map.get(0).dumpStates();
    }
    return map;
  }

  public static int variantIndexOf(Material type, Object rawBlockData) {
    Map<Object, Integer> indexMap = blockDataIndex.get(type);
    Integer integer = indexMap.get(rawBlockData);
    return integer == null ? -1 : integer;
  }

  public static Object rawVariantOf(Material type, int variantIndex) {
    try {
      return blockDataRegister.get(type).get(variantIndex);
    } catch (Exception exception) {
      IntaveLogger.logger().printLine("[Intave] 无法正确模拟方块类型 " + type + " 的数据结构（请求的变体：" + variantIndex + "）");
      exception.printStackTrace();
      return blockDataRegister.get(type).get(0);
    }
  }

  public static Set<Integer> variantIdsOf(Material type) {
    return new HashSet<>(blockDataRegister.get(type).keySet());
  }

  static void invalidateShadowedVariantCache() {
    blockVariants.clear();
  }
}
