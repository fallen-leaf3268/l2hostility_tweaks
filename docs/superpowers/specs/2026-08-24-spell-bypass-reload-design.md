# KubeJS 伤害绕过状态重载清理设计

## 目标

使 `L2HFix.enableBypass(...)` 建立的伤害绕过状态与当前 KubeJS 脚本一致：脚本重载时移除旧状态，再由新脚本重新声明仍需启用的标记。

## 已确认根因

`SpellDamageFlags` 使用进程级静态集合保存启用的伤害类型标签。当前只有服务器完全停止时才调用 `SpellDamageFlags.clear()`，所以 `/reload` 会重新执行脚本，却不会删除旧脚本曾经启用的标记。

项目内置的 KubeJS `2001.6.5-build.26` 提供 `KubeJSPlugin.clearCaches()` 生命周期。其 `ScriptManager.reload()` 字节码确认执行顺序为：先遍历插件调用 `clearCaches()`，然后卸载旧脚本、读取并执行新脚本。因此该回调可以在新脚本运行前准确清除旧状态。

## 设计

在 `L2HFKJSPlugin` 中覆盖：

```java
public void clearCaches()
```

实现先调用 `super.clearCaches()`，再调用 `SpellDamageFlags.clear()`。当前父类实现为空，但保留父类调用可兼容后续 KubeJS 版本增加的基础清理行为。

重载数据流为：

1. KubeJS 开始脚本重载。
2. `L2HFKJSPlugin.clearCaches()` 清空旧的绕过标记。
3. 旧脚本被卸载，新脚本开始执行。
4. 新脚本中的 `enableBypass(...)` 重新加入当前需要的标记。
5. 已从脚本删除的标记保持关闭。

服务器停止时的现有 `SpellDamageFlags.clear()` 继续保留，作为生命周期结束时的额外清理。

## 测试策略

增加 `L2HFKJSPluginTest`，先启用 `bypass_resistance`，确认状态存在，再调用插件的 `clearCaches()` 并确认状态被删除。测试后始终清空静态集合，避免测试间污染。

由于 KubeJS 在生产配置中是 `compileOnly` 可选依赖，测试需把仓库已有的 KubeJS、Rhino 与 Architectury JAR 仅加入 `testImplementation`。这不会改变模组运行时依赖或打包内容，只允许测试直接实例化插件并验证真实生命周期方法。

完整构建还需确认：

- 修改前测试因继承的空 `clearCaches()` 而断言失败。
- 修改后聚焦测试通过。
- `clean build`、资源校验和 JAR 构建通过。

## 错误处理

清空并发集合是幂等操作；首次加载、重复重载以及服务器停止时重复调用都不会报错。脚本执行失败时，最终状态只包含本轮失败前成功声明的标记，不会继续保留上一轮已经删除的配置。

## 非目标

- 不改变各个绕过类型的映射。
- 不处理 `enableBypass(null)` 或未知类型提示；这些作为独立审查项处理。
- 不增加新的玩家配置或服务端配置。
- 不把 KubeJS 变为模组强制运行时依赖。
- 不将 JAR 部署到 `mods`。
