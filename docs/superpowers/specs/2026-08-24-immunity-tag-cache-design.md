# Immunity Tag Cache Reload Design

## 目标

修复词条免疫标签的首次查询失效和数据包重载后缓存过期问题，同时保留现有的标签查询缓存，不改变饰品免疫、词条标签内容或词条执行规则。

## 根因

`ImmunityHelper` 在调用 `hasTraitInTag` 前读取静态标签字段。首次调用时参数已经求值为 `null`，即使方法内部随后发现注册表并初始化字段，本次查询仍会因为收到的参数为 `null` 而返回 `false`。

词条与标签的匹配结果还会永久保存在 `traitTagCache` 中。Forge 数据包标签通过 `/reload` 更新时，项目没有监听 `TagsUpdatedEvent`，因此缓存中的旧结果会一直生效，直到缓存意外达到容量上限或游戏重启。

## 设计

### 标签初始化

所有依赖词条标签字段的公开判断入口先调用 `discoverTraitRegistry()`，再读取 `forceImmuneTraitTag`、`gravityImmuneTraitTag` 或 `playerSelfBlacklistTag`。这样传入查询方法的永远是初始化后的字段值，首次查询不再丢失。

### 缓存失效

`ImmunityHelper` 新增一个公开、无参数的 `invalidateTagCaches()` 方法。该方法清空：

- 词条标签匹配缓存 `traitTagCache`；
- 实体本 tick 的力量免疫结果；
- 实体本 tick 的重力免疫结果。

实体缓存也必须同时失效，否则标签缓存虽然清空，同一 tick 内仍可能读取标签更新前的实体免疫布尔值。

`L2HostilityFix` 在 Forge 主事件总线上监听 `TagsUpdatedEvent`，每次标签绑定完成时调用 `ImmunityHelper.invalidateTagCaches()`。事件同时覆盖服务端数据包重载和客户端标签同步。

标签键对象只描述注册表键与资源位置，不保存标签成员，因此重载时不需要重新创建；注册表发现逻辑保持不变。

## 测试

新增针对纯缓存生命周期的回归测试：

1. 向词条标签缓存写入旧结果后调用 `invalidateTagCaches()`，确认旧结果被移除。
2. 写入实体免疫缓存后调用失效方法，确认实体缓存的 tick 和引用被重置。
3. 用一个纯选择方法验证：发现标签后，本次查询使用初始化后的标签而不是调用前捕获的 `null`。
4. 完整执行 `clean build`，覆盖单元测试、Mixin 注解处理、资源校验、重混淆和 JAR 生成。

## 非目标

- 不取消词条标签缓存。
- 不修改 `immune_to_force`、`immune_to_gravity` 或 `player_self_blacklist` 的 JSON 内容。
- 不改变 Curios 饰品免疫的检测方式。
- 不改变免疫生效范围、词条 tick 顺序或日志级别。
- 不把 JAR 部署到 `mods` 目录。
