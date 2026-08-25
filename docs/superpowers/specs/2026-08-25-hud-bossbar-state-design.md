# HUD 与 BossBar 状态统一设计

## 问题

`L2HHealthOverlay.render()` 会在启用 `hideHudWithBossbar` 且存在原版 BossBar 时隐藏自定义血条并保留原版 BossBar；`precomputeHudState()` 却只要找到有效词条目标就设置 `hideBossBars = true`。BossBar 渲染随后被取消，而自定义 HUD 又因检测到 BossBar 而隐藏，导致两个血条同时消失。

## 行为

- 没有有效词条目标时，隐藏自定义血条并保留原版 BossBar。
- 有有效词条目标且没有 BossBar 时，显示自定义血条并隐藏原版 BossBar。
- 有有效词条目标、存在 BossBar 且开启 `hideHudWithBossbar` 时，隐藏自定义血条并保留原版 BossBar。
- 有有效词条目标、存在 BossBar 且关闭 `hideHudWithBossbar` 时，显示自定义血条并隐藏原版 BossBar。

## 实现

在 `L2HHealthOverlay` 中增加一个包内可测试的纯状态判定方法，根据有效目标、BossBar 活跃状态和客户端配置生成自定义 HUD 与原版 BossBar 的两个显示状态。`precomputeHudState()` 和 `render()` 共同应用该结果，避免两条路径再次分歧。

目标实体 ID 只在自定义 HUD 应显示时设置；否则重置为 `-1`。不修改目标检测、显示距离、血条样式、伤害动画、词条图标或 BossBar 活跃状态的采集方式。

## 验证

单元测试覆盖四种行为组合，特别验证存在 BossBar 且配置开启时不会同时隐藏两个界面。随后运行完整 `clean build`，检查测试结果和生成 JAR。
