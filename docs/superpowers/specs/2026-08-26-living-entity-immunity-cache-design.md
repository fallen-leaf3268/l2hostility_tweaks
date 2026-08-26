# LivingEntity 实例级免疫缓存设计

## 问题

`ImmunityHelper` 当前为力免疫和重力免疫分别使用实体弱引用、tick 和 boolean 三个独立静态字段。客户端与集成服务端共享这些字段；两个线程交错更新时，实体引用、tick 和结果可能来自不同实体，使后续查询命中错误结果。

每种免疫还只能缓存最近查询的一个实体。多个实体连续发生碰撞或重力判定时会相互替换缓存，频繁重新扫描 Curios 和词条，缓存收益有限。

## 目标

- 每个 `LivingEntity` 实例独立缓存力免疫和重力免疫结果。
- 同一实体、同一 tick、同一标签代数下，每种免疫最多执行一次完整扫描。
- 客户端、服务端、不同实体及重建后的实体对象完全隔离。
- 标签或数据包重载后，即使仍在同一 tick，旧结果也立即失效。
- 删除静态实体弱引用及其拆分状态，避免并发错配。
- 保持 Curios、词条标签、调试日志和免疫判断语义不变。

## 设计

新增 `EntityImmunityCache` 接口，分别接受当前缓存戳并查询力免疫或重力免疫。新增以 `LivingEntity` 为目标的 `LivingEntityImmunityCacheMixin`，为每个实体实例注入：

- 一个力免疫 `long` 缓存戳和 boolean 结果；
- 一个重力免疫 `long` 缓存戳和 boolean 结果。

每个缓存戳按 `((long) generation << 32) | (tickCount & 0xffffffffL)` 生成，使一个 int 代数和一个 int tick 完整占据 long 的高、低 32 位。`ImmunityHelper` 持有静态代数整数；`invalidateTagCaches()` 清空词条标签结果表后递增代数。实体无需集中登记或遍历清理，下一次查询会因代数不同而自动失效。

`ImmunityHelper.isImmuneToForce` 和 `isImmuneToGravity` 生成当前缓存戳，将实体转换为 `EntityImmunityCache`，再调用对应实例查询。实例查询仅在缓存戳不同时执行实际扫描，并按“先结果、后缓存戳”的顺序发布。

实际扫描继续调用 `ImmunityHelper` 中现有的力免疫或重力免疫计算逻辑，顺序保持：

1. 检查对应 Curios 物品标签；
2. 检查对应词条标签；
3. 命中时保留现有 debug 日志；
4. 均未命中时返回 false。

为避免复制扫描逻辑，现有计算方法调整为供 Mixin 调用的静态方法。新 Mixin 不注入、覆盖或重定向 `LivingEntity` 原方法，只添加接口、字段和缓存查询方法。

## 删除的共享状态

从 `ImmunityHelper` 删除：

- `cachedEntityForceRef`
- `cacheTickForce`
- `cachedImmuneToForce`
- `cachedEntityGravityRef`
- `cacheTickGravity`
- `cachedImmuneToGravity`
- `WeakReference` 导入及重载时对上述字段的复位逻辑

词条标签到 boolean 的 `ConcurrentHashMap` 保留。它缓存的是注册表标签关系，不包含实体状态，并继续在标签重载时清空。

## 运行语义

- 同一实体同一 tick 的重复力免疫查询只扫描一次；重力免疫使用独立缓存。
- 不同实体、客户端副本与服务端副本拥有不同实例字段，不会互相覆盖。
- tick 变化时重新扫描。
- 标签缓存代数变化时，即使 tick 不变也重新扫描。
- 同一 tick 内普通装备或词条状态变化最迟下一 tick 生效，保持原缓存语义。

## 明确不做

- 不改变力免疫与重力免疫的功能边界。
- 不改变 `PushCancelMixin` 或 `GravityTraitMixin` 的取消条件。
- 不改变免疫物品标签和词条标签内容。
- 不与玩家眼镜缓存或反建造缓存合并。
- 不新增配置项、网络包或提示文本。

## 验证

- 先增加失败测试，再编写生产实现。
- 使用可计数扫描实现验证同戳仅扫描一次、tick 变化重新扫描、力与重力独立、不同实例隔离。
- 验证标签代数递增后，同一 tick 生成不同缓存戳并触发重扫。
- 验证结果先于缓存戳写入。
- 验证旧静态弱引用和拆分字段全部移除，新 Mixin 已在 common 配置中注册。
- 更新现有 `ImmunityHelperCacheTest`，使其验证代数失效而非旧字段反射复位。
- 运行完整单元测试、资源校验和构建。
- 检查生成 JAR 包含 `EntityImmunityCache`、`LivingEntityImmunityCacheMixin` 和更新后的 Mixin 配置。
- 将最终 JAR 输出到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`。
