# 不死次数恢复与说明设计

## 目标

修复不死词条的临时封印到期后仍保留旧触发次数的问题，并在不死词条说明中展示当前次数上限与封印时间。

## 配置语义

- `max_resurrections = -1` 表示无限制，不显示次数限制说明。
- `seal_duration = 0` 保持现有“不封印”语义，不启用实际次数限制，也不显示次数限制说明。
- `max_resurrections >= 0` 且 `seal_duration > 0` 时，显示“触发 %s 次后封印词条，封印的词条将在 %s 秒后解封”。
- `max_resurrections >= 0` 且 `seal_duration = -1` 时，显示“触发 %s 次后永久封印词条”。

## 解封行为

不死词条被临时封印时保留当前触发次数。封印自然到期并实际恢复不死词条等级后，删除 `l2fix$undying_count`，使下一轮重新获得完整次数。永久封印不会进入解封流程，因此不会重置次数。其他词条解封时不修改该计数。

计数重置放在 `TraitDisableHelper.setDisabled(..., false, ...)` 的成功恢复分支中，确保只有词条真正解封后才重置，而不是仅凭到期时间提前重置。

## Tooltip

`UndyingTrait` 没有声明自己的 `addDetail`，因此不直接向其继承方法注入。改为在现有 `MobTraitDescMixin` 中注入 `MobTrait.addDetail` 的尾部，并以注册名 `l2hostility:undying` 精确筛选。这样物品 Tooltip、HUD 和词条面板使用同一条说明。

说明组件由 `TraitDisableHelper` 根据 `max_resurrections` 和 `seal_duration` 构造。临时封印和永久封印使用不同语言键，中英文资源同步添加。

## 测试

- 验证不死词条真正解封时清除触发计数。
- 验证其他词条解封不清除不死计数。
- 验证无限次数和 `seal_duration = 0` 不生成说明。
- 验证临时封印说明包含次数与秒数。
- 验证永久封印说明使用永久封印语言键。
- 运行完整测试和 `clean build`，检查最终 JAR 内容与哈希。

## 不在本次范围

- 不改变不死触发次数的现有计数时机。
- 不改变 `seal_duration = 0` 的既有行为。
- 不修改其他词条的封印、解封或 Tooltip。
