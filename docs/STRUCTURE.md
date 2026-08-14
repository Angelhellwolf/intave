# 代码结构总览

## 检测（Checks）
检测为检测算法与检测簇提供骨架结构。<br>
它们位于 `intave/check` 包，且必须继承 Check 类。<br>
真正的检测逻辑只应实现于此。<br>
每个检测有一个类，以及同名子包存放检测部件或所需组件。

例如：`intave/check/Physics` 检测对应 `intave/check/physics/` 子包。<br>

## 动态活动模块（Dynamic active modules）
DA 模块按特定顺序加载，位于 `intave/modules/` 包，
且必须继承 `/intave/modules/Module` 类。<br>

它们按用途分组。<br>
例如 `intave/module/dispatch/` 为分发模块，`intave/module/tracker/` 为追踪模块。<br>

## 静态被动模块（Static passive modules）
静态模块拥有保留包名，且始终加载。<br>
可将其视为始终可用的库式服务。<br>

静态模块示例：
- `intave/block/*/`
- `intave/klass/*/`
- `intave/resource/`
- `intave/packet/*/`

通常有一个入口类用于访问该模块。<br>
例如通过 `intave/resource/Resources` 访问全部资源。<br>

## 高阶服务（Services of high order）
高阶服务是尚未迁移到新模块系统的旧模块。<br>
它们标注 `@HighOrderService`，并由主类 `IntavePlugin` 直接加载。<br>
