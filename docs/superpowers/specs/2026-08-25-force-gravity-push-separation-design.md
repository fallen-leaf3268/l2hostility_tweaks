# 力量与重力免疫职责分离设计

## 问题

项目分别公开 `immune_to_force` 和 `immune_to_gravity` 两类物品及词条标签，但 `PushCancelMixin` 将两者都解释为取消 `Entity.push(DDD)`。因此仅声明重力免疫的扩展内容也会获得攻击击退、爆炸推动和实体碰撞推动免疫，两个标签的职责被意外合并。

## 设计

普通推动只由力量免疫控制：

- `PushCancelMixin` 仅在 `ImmunityHelper.isImmuneToForce(entity)` 为真时取消推动；
- `GravityTraitMixin` 继续通过 `isImmuneToGravity(entity)` 阻止重力词条生效；
- 不修改标签 JSON，不新增配置，也不改变恒静腰带和阿布拉哈达布拉的默认行为，因为两件物品当前同时存在于两个免疫标签中；
- 数据包只把物品或词条加入 `immune_to_gravity` 时，该内容免疫重力词条但仍会受到普通击退。

将推动取消条件提取为包内可测试的纯函数，输入力量免疫和重力免疫两个布尔值，输出是否取消推动。生产路径调用该函数，明确记录重力参数不会决定普通推动。

## 验证

新增回归测试覆盖：

1. 只有力量免疫时取消推动；
2. 只有重力免疫时不取消推动；
3. 两者都没有时不取消推动；
4. 两者都有时仍因力量免疫取消推动。

最后运行完整 `clean build`，将 JAR 输出到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`。
