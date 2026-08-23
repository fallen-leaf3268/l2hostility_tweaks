# KubeJS 传奇词条颜色兜底设计

## 目标

保证通过 KubeJS 创建 `legendary_self_effect` 或 `legendary_effect` 词条时，即使脚本没有调用 `.color(...)`，词条的 Tooltip、名称和符文渲染也能稳定使用传奇金色，不因空颜色供应器抛出异常。

## 已确认问题

上游 `AbstractTraitBuilder` 的 `color` 初始值为 null。`LegendarySelfEffectTraitBuilder` 和 `LegendaryTargetEffectTraitBuilder` 直接把该值传入词条对象，而两个词条的 `getColor()` 都直接调用 `color.getAsInt()`。与之相比，`LegendaryAttributeTraitBuilder` 会在创建对象前补金色，普通 `SelfEffectTraitBuilder` 也会在颜色为空时回退父类颜色。

## 设计

采用创建端和对象端两层保护：

1. 两个受影响的 Builder 在 `createObject()` 中检查 `color`。为空时调用现有的 `color(ChatFormatting.GOLD)`，与 `LegendaryAttributeTraitBuilder` 保持一致。
2. `LegendarySelfEffectTrait` 和 `LegendaryTargetEffectTrait` 在构造时将空颜色供应器规范化为返回 `ChatFormatting.GOLD.getColor()` 的供应器。这样直接构造对象或未来新增 Builder 时也不会留下空状态。

显式调用 `.color(ChatFormatting.X)` 或 `.color(int)` 时继续使用脚本给出的颜色，不被默认值覆盖。默认金色只应用于缺省情况。

## 数据流与错误处理

KubeJS Builder 创建词条时先保证颜色存在，词条构造器再验证一次并保存非空供应器。之后所有 `getColor()` 调用保持原路径，无需捕获异常、记录日志或在每次渲染时重复判断。该修改不影响效果类型、持续时间、倍率、传奇规则或注册 ID。

## 测试

- 直接以空颜色构造两种词条，验证 `getColor()` 返回传奇金色且不抛异常。
- 验证非空自定义颜色供应器仍原样生效。
- 验证两个 Builder 的创建路径包含缺省金色初始化，并通过编译与相关测试。
- 执行完整 `clean build`；生成 JAR，但不部署到 `mods`。
