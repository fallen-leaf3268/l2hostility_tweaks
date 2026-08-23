# 封印词条 hasTrait 修复实施计划

**目标：** 让封印词条保留在原始映射中，但不参与任何依赖 `hasTrait()` 或 `getTraitLevel()` 的生效逻辑。

**实现方式：** 在 `GetTraitLevelMixin` 中建立可测试的统一激活判定 `rawLevel > 0`，并让两个注入点共同使用该判定。

### 任务 1：建立失败测试

- [x] 新增 `GetTraitLevelMixinTest`。
- [x] 覆盖正等级、零等级和负等级。
- [x] 运行定向测试并确认因缺少判定方法而失败。

### 任务 2：实现修复

- [x] 新增统一激活判定方法。
- [x] `getTraitLevel()` 对非正等级返回 0。
- [x] `hasTrait()` 对非正原始等级返回 `false`。

### 任务 3：验证与提交

- [x] 定向测试通过。
- [x] 完整 `clean build` 通过。
- [x] 检查差异并提交独立 commit。
