# Task 2：复用 L2Hostility 实体能力门槛

## RED 证据

- 先仅修改 `MinecraftTraitSpawnIndexSourceTest`，新增 `shouldIncludeEntity` 的完整布尔真值表，以及生产源码必须包含 `MobTraitCap.HOLDER.isProper(living)` 的断言。
- 控制器执行测试后，`compileTestJava` 失败：4 处均为 `MinecraftTraitSpawnIndexSource.shouldIncludeEntity(boolean, boolean, boolean)` 不存在。
- 失败原因与预期一致：生产代码尚未提供待测的包内纯函数。
- 审查发现玩家实体仍会先求值 `MobTraitCap.HOLDER.isProper(living)` 后才被排除，因此第二轮先仅新增源码接线顺序断言。
- 控制器确认第二轮 RED：`MinecraftTraitSpawnIndexSourceTest` 共 1 个测试失败，位置为第 42 行；缺少玩家守卫位于能力调用之前的短路表达式，失败原因符合预期。

## GREEN 结果

- 在 `captureEntities` 中，临时实体先被限定为 `LivingEntity`，随后先计算 `boolean player = living instanceof Player;`，再通过 `boolean traitCapabilityApplies = !player && MobTraitCap.HOLDER.isProper(living);` 短路计算能力门槛。玩家实体不会求值 `isProper`。
- 新增包内纯函数 `shouldIncludeEntity(boolean living, boolean player, boolean traitCapabilityApplies)`，仅在 `(true, false, true)` 时返回 `true`。
- 源码接线测试同时断言玩家守卫的出现顺序与 `!player && MobTraitCap.HOLDER.isProper(living)` 短路表达式，防止未来回退为先调用能力门槛。
- 指定验证命令通过：`gradlew.bat --no-daemon --offline test --tests com.l2hostility_tweaks.generation.MinecraftTraitSpawnIndexSourceTest --tests com.l2hostility_tweaks.generation.TraitSpawnIndexBuilderTest --tests 'com.l2hostility_tweaks.compat.jei.*'`；控制器报告 `BUILD SUCCESSFUL in 41s`（初始 GREEN）及 `BUILD SUCCESSFUL in 38s`（短路修正 GREEN）。

## 修改文件

- `src/main/java/com/l2hostility_tweaks/generation/MinecraftTraitSpawnIndexSource.java`
- `src/test/java/com/l2hostility_tweaks/generation/MinecraftTraitSpawnIndexSourceTest.java`
- `.superpowers/sdd/refine-task-2-report.md`

## 提交

- `a68537d fix: filter JEI mobs by trait capability`（本轮 amend 前的基线提交；最终提交哈希见提交历史）。

## 风险

- `MobTraitCap.HOLDER.isProper(living)` 的结果取决于 L2Hostility 的运行时标签与配置；这是需要复用的真实门槛，因此白名单允许的非敌对实体会被包含，普通非敌对实体与黑名单敌对实体会被排除。既有的 `NO_TRAIT`、配置及词条过滤、全局开关和动态条件路径未修改。玩家实体现在在调用该运行时门槛前被短路排除。
