# Intave 方块系统

## 概述

Intave 方块系统高层概览：省略部分细节，但足以理解整体工作方式。
<br>
<br>
<br>

<img src="assets/blocksystem-transparent.png" alt="图片缺失! :(">

## 类型（Types）
Intave 使用 Bukkit 的 Material 枚举表示类型。

### 翻译（Translation）

Intave 支持类型翻译，可将方块类型映射转换。
实际中该系统暂未使用 :(

## 变体与变体索引（Variants and Variant Indexes）
服务器中的全部 IBlockData 会映射到一个**随机**且唯一的变体索引。
除处理 1.8 相关特殊情况外，强烈不建议直接使用该索引。
变体索引 0 始终为默认变体（因此不是随机的）。
```java
Material type = ...;
int variantIndex = ...;

// 索引 -> MC IBlockData
Object iBlockData = BlockVariantRegister.rawVariantOf(type, variantIndex);
  
// MC IBlockData -> 索引
int variantIndex = BlockVariantRegister.variantIndexOf(type, iBlockData);

// 另外：

// 索引 -> Intave 着色后的 BlockVariant
BlockVariant variant = BlockVariantRegister.variantOf(type, variantIndex);


```


## 形状（Shapes）
方块形状可以是：
- 长方体 ```CubeShape```
- ```BoundingBox```，或
- 其它方块形状的集合（```ArrayBlockShape```、```MergeBlockShape```）

### 原点形状（Origin Shapes）
若形状为原点形状，表示其位于 x=0 y=0 z=0。
```java
BoundingBox.isOriginShape() // 检查包围盒是否为原点盒
BlockShape.contextualized() // 将形状移动到给定位置
BlockShape.normalized()     // 将形状移回原点
```

### 不同用途
1. Collision Shape：与玩家移动发生碰撞
2. Outline Shape：用于射线检测
3. Visual Shape：用于渲染 [不在 Intave 中]（Minecraft 源码中称为 "Shape"）

### 补丁（Patches）如何工作
- 每次请求形状时都会调用（请尽量保证性能可接受）
- 在流水线中，补丁位于形状缓存之后，因此结果不会被缓存

```java
  @Override             // v---- 仅修补 outline 形状
  protected BlockShape outlinePatch(World world, Player player, int posX, int posY, int posZ, Material type, int variantIndex, BlockShape shape) {
    if (shape.isEmpty()) {                                                                           // ^---------^---------- 始终与延迟同步
      return shape; // <--- 无需修复时不做修复
    }
    BlockVariant variant = BlockVariantRegister.variantOf(type, variantIndex); // <--- 通过 BlockVariantRegister 访问方块属性
    boolean hanging = variant.propertyOf("hanging");
    int age = hanging ? variant.propertyOf("age") : 4;
    long randomCoordinate = coordinateRandom(posX, 0, posZ);
    int xOffsetKey = (int) (randomCoordinate & 15L);
    int zOffsetKey = (int) (randomCoordinate >> 8 & 15L);
    BlockShape box = CACHE[age][xOffsetKey][zOffsetKey]; // <--- 本地缓存加速处理
    if (box == null) {
      double allowedOffset = 0.25D;
      double offsetX = MathHelper.minmax(-allowedOffset,((double) ((float) xOffsetKey / 15.0F) - 0.5D) * 0.5D, allowedOffset);
      double offsetZ = MathHelper.minmax(-allowedOffset,((double) ((float) zOffsetKey / 15.0F) - 0.5D) * 0.5D, allowedOffset);
      double offsetY = 0.0;
      box = CACHE[age][xOffsetKey][zOffsetKey] = SHAPE_PER_AGE[age].originOffset(offsetX, offsetY, offsetZ);
    }                                                               // ^---- 补丁理想情况下应返回原点形状
    return box;
  }
```
重要：通过 VolatileBlockAccess（或其它访问方块缓存的方式）获取方块类型时，
也可能触发形状补丁流程，并再次访问该形状缓存。或许以后应该修一下……
