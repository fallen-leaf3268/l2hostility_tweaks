# 玩家实例级反建造绕过缓存设计

## 问题

`AntibuildBypassHelper` 当前以玩家 UUID 为键，将世界 `gameTime` 和绕过结果压缩后存入静态 `ConcurrentHashMap`。单机环境中的客户端玩家与服务端玩家可能具有相同 UUID 和相同世界时间，但装备、Curios 或词条同步状态并不一定一致，因此一侧可能复用另一侧的结果。

`AntibuildBlockImmuneMixin` 还会先调用 `hasArenaTrait`，在结果为 false 后再调用内部重复检查竞技场词条的 `hasBypass`，导致缓存失效路径重复遍历玩家词条。

## 目标

- 同一玩家实例在同一游戏 tick 最多执行一次完整绕过检查。
- 客户端、服务端及重生后的玩家实例完全隔离。
- 删除 UUID 缓存、并发 Map、容量清理和重复竞技场词条扫描。
- 保持竞技场词条、装备标签、Curios 标签和异常回退语义不变。
- 保持同 tick 内状态变化最迟在下一 tick 生效的现有语义。

## 设计

新增 `AntibuildBypassCache` 接口，仅暴露无参数的 `l2fix$hasAntibuildBypass()`。新增以 `Player` 为目标的 `PlayerAntibuildBypassCacheMixin`，为每个玩家实例注入一个上次扫描的 `long gameTime` 和一个缓存的 boolean 结果。

公开查询方法从玩家自身的 `level().getGameTime()` 读取时间，避免调用方传入其他世界的时间。若时间与缓存相同则直接返回结果；不同则执行一次完整扫描，先写入结果，再写入时间。

完整扫描保持以下短路顺序：

1. 检查玩家是否具有正等级的 `l2hostility:arena` 词条；
2. 检查所有原版装备槽是否命中 `l2hostility_tweaks:antibuild_bypass` 物品标签；
3. 检查所有 Curios 槽是否命中同一标签。

Curios 能力缺失或查询抛出异常时仍返回 false。注册表 ID 和标签可以静态共享，但不得静态保存任何玩家状态。

`AntibuildBlockImmuneMixin` 和 `AntibuildPlaceBypassMixin` 均将玩家转换为 `AntibuildBypassCache` 并调用同一接口。前者删除单独的 `hasArenaTrait` 分支，因此每个入口都只触发一次完整缓存查询。

旧 `AntibuildBypassHelper` 不再有调用者，其 UUID Map、清理和扫描职责全部被实例缓存取代，因此删除该文件。

反建造缓存使用独立的 Player Mixin，不并入探测眼镜缓存 Mixin，使两个功能可单独测试和修改。新 Mixin 只添加字段和接口方法，不注入、覆盖或重定向 `Player` 原有方法。

## 运行语义

- 客户端与服务端的玩家对象分别保存结果，UUID 和世界时间相同也不会串用。
- 玩家死亡重生后，新对象从无效缓存状态开始；旧缓存随旧对象回收。
- 首次查询执行词条、装备和 Curios 检查，同 tick 后续查询只执行实例方法、long 比较和 boolean 读取。
- 时间变化时重新扫描；同 tick 内刚获得或移除绕过来源，结果最迟下一 tick 更新。

## 明确不做

- 不改变反建造效果何时阻止交互或放置。
- 不改变竞技场词条、装备标签或 Curios 标签的判定范围。
- 不与探测眼镜缓存合并。
- 不新增配置项或客户端提示。

## 验证

- 先增加失败测试，再编写生产实现。
- 使用可计数扫描实现验证同 tick 多次查询只扫描一次、下一 tick 重新扫描、不同缓存实例互不影响。
- 验证两个调用入口均使用 `AntibuildBypassCache`，且旧 helper、UUID、Map、外层竞技场预检查均被移除。
- 验证新 Player Mixin 已在 common Mixin 配置中注册。
- 运行完整单元测试、资源校验和构建。
- 检查生成 JAR 包含接口、新 Mixin 和更新后的配置，且不再包含 `AntibuildBypassHelper.class`。
- 将最终 JAR 输出到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`。
