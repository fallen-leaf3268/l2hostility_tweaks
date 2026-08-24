# 词条 Tick 日志移除设计

## 问题

`MobTraitCapTickMixin` 在免疫系统阻止词条 tick 时执行 INFO 日志。该方法随生物词条每游戏刻调用，一个持续被阻止的词条每小时会产生约 72,000 条日志，造成日志膨胀、磁盘写入和控制台开销。

## 行为

- 保留 `ImmunityHelper.isImmuneToTraitTick(entity, trait)` 判断。
- 免疫成立时继续跳过 `trait.tick(entity, level)`。
- 免疫不成立时继续正常调用词条 tick。
- 免疫成立时不再写入 INFO 或其他级别日志。
- 保留 `ImmunityHelper` 中已有的 DEBUG 诊断信息，不修改其他日志。

## 实现范围

从 `MobTraitCapTickMixin` 删除 Logger 字段、Logger 相关 import 和 `[TraitTick] BLOCKED` 日志调用。除该文件及对应回归测试外，不修改其他生产代码。

## 测试

回归测试检查 `MobTraitCapTickMixin` 不再声明日志器字段，旧实现应先失败，删除日志器后转为通过。随后运行完整测试和 `clean build`，检查最终 JAR 并输出到 `C:\Users\Lenovo\Desktop\Ai_Run\output`。
