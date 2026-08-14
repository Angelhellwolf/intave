# 速查表

### 方块访问（BLOCK ACCESS）

- 需要 user
- 极快且线程安全
- 支持幽灵方块、自定义方块形状、自定义类型翻译
- 访问远离玩家的方块时可能有问题（!）
```
VolatileBlockAccess.typeAccess
VolatileBlockAccess.variantIndexAccess
VolatileBlockAccess.fluidAccess
VolatileBlockAccess.collisionShapeAccess
Fluids.fluidAt()
Fluids.fluitPresentAt()
```


### 事务（TRANSACTIONS）

- 需要 user
- 应仅在主线程执行

```
user.tickFeedback(() -> <callback>)
```

// 欢迎自行在此补充更多章节！
