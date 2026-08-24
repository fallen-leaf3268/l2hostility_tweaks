# 玩家词条运行状态死亡继承设计

## 背景

玩家的不死触发次数保存在实体持久数据键 `l2fix$undying_count` 中。当前死亡克隆流程会保存词条能力、能力元数据以及封印到期时间，但不会保存该次数。启用 `keepInventory` 时，玩家通过绕过不死的伤害真正死亡后，词条会恢复，而不死次数会重置，从而绕过配置的最大触发次数。

## 目标

使用一个统一的 `CompoundTag` 快照保存无法从词条能力重建的运行状态，使 `keepInventory=true` 的玩家死亡克隆同时保留：

- 所有以 `l2htweaks_seal_expiry_` 开头的封印到期时间；
- `l2fix$undying_count` 不死触发次数。

不复制玩家的其他持久 NBT，也不复制 `sealed_level`。封印等级继续根据恢复后的负词条等级重建，避免恢复陈旧状态。

## 结构

`TraitDisableHelper` 提供两个纯数据方法：

- 从玩家持久数据创建受控运行状态快照；
- 将受控运行状态快照恢复到新玩家持久数据。

方法只识别上述两类键，并按原类型写入：封印到期时间使用长整型，不死次数使用整型。返回的快照是独立数据，不与源标签共享可变子对象。

`L2HostilityFix` 使用 `Map<UUID, CompoundTag>` 保存死亡时的运行状态，替换只保存封印时间的 `deathSealExpiry`。词条能力快照和 `deathMeta` 保持不变。

## 数据流程

1. `LivingDeathEvent` 捕获玩家当前词条能力。
2. 调用 `TraitDisableHelper` 从旧玩家持久数据创建运行状态快照。
3. 快照非空时按玩家 UUID 保存。
4. `PlayerEvent.Clone` 开始时移除并取得能力快照、运行状态快照和能力元数据。
5. `keepInventory=false` 时保持当前行为：清空新玩家词条，不恢复运行状态。
6. `keepInventory=true` 且能力快照有效时，恢复词条、能力元数据和运行状态。
7. 遍历恢复后的词条，根据负等级重新生成 `sealed_level`，随后执行现有初始化和延迟同步。

## 生命周期与异常数据

- 克隆读取使用 `remove`，保证每份快照只消费一次。
- 玩家登出和服务器停止时清理运行状态快照 Map。
- 缺少不死次数时不在目标中创建该键。
- 无关 NBT 永远不进入快照。
- 空快照不写入 Map。
- 保留现有 `keepInventory=false` 语义，不把旧玩家的限制状态带给已被清空词条的新实体。

## 测试

在现有 `TraitDisableHelperTest` 中覆盖：

- 快照并恢复不死触发次数；
- 不死次数不存在时保持不存在；
- 快照并恢复多个封印到期时间；
- 无关持久 NBT 与 `sealed_level` 不被复制；
- 修改源数据不会改变已创建的快照；
- 修改恢复后的目标数据不会反向改变快照。

构建验证使用完整 `clean test build`，并检查输出 JAR 包含更新后的 `TraitDisableHelper` 与 `L2HostilityFix` 类。

## 非目标

- 不改变不死次数的累加、封印或解封规则；
- 不改变 `keepInventory=false` 的词条清除规则；
- 不复制其他模组或 Minecraft 自身的玩家持久数据；
- 不重构现有能力快照和延迟同步流程。
