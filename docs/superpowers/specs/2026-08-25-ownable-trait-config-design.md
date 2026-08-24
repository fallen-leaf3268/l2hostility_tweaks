# 驯服生物词条配置恢复设计

## 背景

上游 `MobTraitCap.tick` 会在以下条件同时成立时清空实体词条：

- `allowTraitOnOwnable=false`；
- 实体实现 `OwnableEntity`；
- 实体主人是玩家；
- 实体当前拥有词条。

本模组的 `MobTraitCapTickMixin.l2fix$skipPetClear` 使用空 `@Redirect` 无条件吞掉这一处 `LinkedHashMap.clear()`，使上游配置无法生效。玩家本身不实现 `OwnableEntity`，因此该重定向与玩家词条能力支持无关。

## 目标

删除宠物词条清除重定向，恢复上游 `allowTraitOnOwnable` 的原始语义：

- 配置为 `false` 时，玩家驯服生物的词条按上游逻辑清除；
- 配置为 `true` 时，上游不会进入清除分支，宠物继续保留词条。

## 实现

在 `MobTraitCapTickMixin` 中删除：

- `l2fix$skipPetClear(LinkedHashMap<?, ?>)` 方法；
- 不再使用的 `java.util.LinkedHashMap` 导入。

保留以下现有行为：

- 玩家能力处于 `PRE_INIT` 时切换至 `POST_INIT`，避免玩家自动生成生物词条；
- 词条 tick 经过 `ImmunityHelper.isImmuneToTraitTick` 判断；
- `GeneralCapabilityHolderMixin` 继续允许玩家拥有词条能力。

不新增本模组配置，不复制或重写上游判断。

## 测试

在现有 `LegendaryAllowMixinTest` 中增加反射回归测试，断言 `MobTraitCapTickMixin` 不再声明 `l2fix$skipPetClear`。测试必须先在现有实现上失败，再删除重定向并转为通过。

随后执行完整 `clean test build`，并检查 JAR 中包含更新后的 `MobTraitCapTickMixin.class` 和 Mixin 配置。

## 非目标

- 不改变上游 `allowTraitOnOwnable` 的默认值；
- 不新增强制保留宠物词条的独立配置；
- 不改变玩家词条、词条封印、词条免疫或词条 tick 行为；
- 不部署 JAR 到 `mods` 目录。
