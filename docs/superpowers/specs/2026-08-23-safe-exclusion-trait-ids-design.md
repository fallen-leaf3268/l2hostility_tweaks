# 互斥词条 ID 安全处理设计

## 目标

防止 `trait_exclusion.groups` 中格式非法的词条 ID 在客户端生成词条符文 Tooltip 时触发 `ResourceLocation` 异常，同时保持合法配置、合法但未注册的附属模组词条 ID 和现有互斥行为不变。

## 根因

Forge 配置目前只检查互斥组字符串是否包含逗号。`L2HConfig.getExclusionGroups()` 会保留所有非空 ID，而 `TraitSymbolMixin.getTraitName()` 直接用这些字符串构造 `ResourceLocation`。例如 `first,l2hostility:gravity,Invalid Trait ID` 会被配置层接受，但悬停“重力”词条符文时，非法 ID 会在 Tooltip 路径中抛出异常。

## 设计

采用两层保护：

1. `L2HConfig` 在解析互斥组时，仅过滤无法通过 `ResourceLocation.tryParse` 的词条 ID。合法但当前未注册的 ID 继续保留，以支持可选附属模组和加载顺序。
2. `TraitSymbolMixin.getTraitName()` 使用安全解析。若调用方仍传入非法 ID，则直接返回包含原始字符串的文本组件，不查询注册表，也不抛异常。

规则名和其余配置格式保持不变。过滤后只剩一个合法 ID 的互斥组可以保留；它没有实际冲突对象，但不会改变该词条的行为。完全没有合法 ID 的条目不加入解析结果。

## 错误处理

格式非法的 ID 被静默忽略，不刷日志。合法但未注册的 ID 在 Tooltip 中继续显示原始 ID。该修复不自动改写 TOML 文件，便于用户自行发现和修正原配置。

## 测试

- 配置解析测试：合法 ID 被保留，包含空格或非法字符的 ID 被过滤，合法但未注册的 ID 被保留。
- Tooltip 名称解析测试：非法 ID 返回原始文本且不抛异常。
- 运行相关定向测试，再执行完整 `clean build`。
- 不把 JAR 部署到 `mods`。
