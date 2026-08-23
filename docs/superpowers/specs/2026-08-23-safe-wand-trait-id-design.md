# 工具物品词条 ID 安全解析设计

## 问题

`TraitWandHelper.getTrait()` 直接用物品 NBT 字符串构造 `ResourceLocation`。非法字符会抛出异常，而该方法同时用于 Tooltip、物品交互和网络切换路径，因此异常 NBT 可造成客户端崩溃或服务端交互失败。

## 行为

- 合法资源 ID 按现有流程查询词条注册表。
- 非法资源 ID 不抛异常，进入现有默认 `l2hostility:tank` 回退。
- 合法但未注册的 ID 同样保持当前默认回退行为。
- 不在读取或 Tooltip 阶段修改物品 NBT。

## 实现边界

在 `TraitWandHelper` 中增加可测试的安全解析方法，内部使用 `ResourceLocation.tryParse()`。`getTrait()` 仅在解析结果非空时查询注册表，其余选择、循环和回退逻辑不变。

## 验证

单元测试覆盖合法 ID 正常解析和包含非法字符的 ID 返回 `null`，随后运行完整 `clean build`。
