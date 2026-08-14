
![Intave](docs/assets/hero_banner.png "Intave")


Intave 是一款面向 Minecraft 服务器的企业级反作弊插件，自 2016 年起持续开发。
在世界顶级服务器上使用近十年、并于 2025 年中停止运营后，
我们决定回馈社区，将 Intave 以源码可用（source-available）的形式向所有人开放。

## 下载
- [自动加载器](https://github.com/intave/loader/releases/download/v1.0.0/IntaveLoader.jar)（推荐）
- [每日构建](https://github.com/intave/intave/releases/download/nightly/Intave.jar)
- [Modrinth](https://modrinth.com/plugin/intave)

## 概述

与传统模块化反作弊不同，Intave 精确模拟玩家移动、客户端实体与方块数据，
从而发现极细微的操控。基于该思路，Intave 能有效阻止各类战斗、
移动与交互类作弊，例如加速/飞行，或超出 3.0 格攻击距离等。

此外，Intave 还提供启发式检测，用于对抗自瞄、连点、加速（Timer）、搭路、
挖方块、背包操作，以及许多仅靠模拟客户端逻辑无法覆盖的作弊。

更多说明见 Intave 检测文档：
[此处](https://docs.intave.ac/mechanics/checks-01-overview.html)。

## 开发

### 环境搭建

1. 克隆项目：`git clone https://github.com/intave/intave.git`。
2. 以 Gradle 项目打开；等待数分钟，让 IntelliJ 完成索引与构建。

### 测试

选择与目标 Minecraft 服务端版本对应的 `intave/run_X.X.X` Gradle 任务。
Intave 会自动安装到该服务器。若 Intave 未能下载 ProtocolLib，
请手动将 ProtocolLib 放入 `plugins` 目录。

这样即可在 IDE 中直接运行插件，并支持断点与热替换。
我们使用 [此 IntelliJ 插件](https://plugins.jetbrains.com/plugin/14832-single-hotswap) 做高效热替换，
可替换不含 indy lambda 或匿名类的方法体。

## 贡献

欢迎贡献，提交前请先阅读[贡献指南](docs/CONTRIBUTING.md)。
项目结构高层概览见[本文档](docs/STRUCTURE.md)。
快速上手速查表见[此处](docs/CHEATSHEET.md)，也欢迎补充！
方块系统简述见[本文档](docs/BLOCK_SYSTEM.md)。
有问题可在 [Discord](https://intave.ac/go/discord) 联系我们。

## 许可证
我们希望 Intave 永久免费开放，供所有人使用。
但我们不希望任何人拿走这份成果、改名包装后当作自己的产品出售。
其它反作弊项目上已多次发生此类情况，我们明确禁止此类行为。
同时，我们允许 Minecraft 服务器将 Intave 用于商业运营，
并允许按需修改与适配，前提是不得将其作为产品出售或公开发布。
因此我们采用 [Polyform Perimeter License 1.0.0](LICENSE.md)，
禁止任何形式的竞争性使用。
我们也鼓励大家把改进回馈主项目，而不是各自维护分支，
以免社区与开发力量被分散。
从技术上说，Intave 并非严格意义上的「开源」，而是「源码可用」：
可使用、可修改，但不得出售、不得冒充原创，也不得并入你自己的产品或项目，
不论其它许可证如何约定。
若发生盗源或商业再分发，我们将发起 DMCA 下架；情节严重者将追究法律责任，
并非戏言。
请注意：Intave 使用的第三方库各自遵循其许可证，
可能不受 Polyform Perimeter License 约束。
