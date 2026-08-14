# 分支命名规范

我们有三个主分支：**master**、**staging** 与 **dev**。<br>

**master** 是主分支，所有变更最终汇入此处。<br>

## 功能分支

开发新功能时，应从 **dev** 创建新分支。<br>
分支命名应为：

feature/`<feature-name>` 新功能，合并到 master<br>
refactor/`<refactor-class>` 重构，合并到 master<br>
hotfix/`<hotfix-name>` 热修复，合并到 master<br>

琐碎改动可直接提交到 dev。

## Pull Request

功能完成后，应从功能分支向 **dev** 发起 pull request。<br>
合并前至少应有一人审查。<br>

## 合并

合并 pull request 时，应始终使用 **Squash and merge**。<br>
