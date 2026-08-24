# Trait Tick 高频日志移除实施计划

> **For Codex:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task.

**Goal:** 移除免疫词条 tick 被拦截时每刻输出的 INFO 日志，同时保持免疫拦截和正常词条 tick 行为不变。

**Architecture:** 保留 `MobTraitCapTickMixin` 现有 `@Redirect` 注入点，仅把分支简化为“非免疫时调用原始 `trait.tick`”。使用反射回归测试约束该 Mixin 不再持有专用于此日志的 `LOG` 字段。

**Tech Stack:** Java 17、ForgeGradle、Mixin、JUnit 5、Gradle

---

### Task 1: 添加失败回归测试并实施最小修复

**Files:**
- Modify: `src/test/java/com/l2hostility_tweaks/mixin/LegendaryAllowMixinTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/MobTraitCapTickMixin.java`

**Step 1: 写入失败测试**

在现有测试类中加入：

```java
@Test
void blockedTraitTickDoesNotRetainAnInfoLogger() {
    assertFalse(Arrays.stream(MobTraitCapTickMixin.class.getDeclaredFields())
            .anyMatch(field -> field.getName().equals("LOG")));
}
```

并导入 `java.util.Arrays`。

**Step 2: 运行测试并确认 RED**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.mixin.LegendaryAllowMixinTest`

Expected: 新测试失败，因为 `MobTraitCapTickMixin` 仍声明 `LOG` 字段。

**Step 3: 写入最小实现**

删除 `LogUtils`、`Logger` 导入与 `LOG` 字段，将重定向方法改为：

```java
if (!ImmunityHelper.isImmuneToTraitTick(entity, trait)) {
    trait.tick(entity, level);
}
```

**Step 4: 运行测试并确认 GREEN**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.mixin.LegendaryAllowMixinTest`

Expected: 该测试类全部通过。

**Step 5: 提交代码**

```bash
git add src/main/java/com/l2hostility_tweaks/mixin/MobTraitCapTickMixin.java src/test/java/com/l2hostility_tweaks/mixin/LegendaryAllowMixinTest.java
git commit -m "fix: remove blocked trait tick log spam"
```

### Task 2: 完整验证并输出 JAR

**Files:**
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`

**Step 1: 运行完整测试与构建**

Run: `./gradlew.bat clean test build`

Expected: BUILD SUCCESSFUL，全部测试零失败、零错误。

**Step 2: 检查构建产物内容**

确认 JAR 包含 `MobTraitCapTickMixin.class` 和 Mixin 配置文件。

**Step 3: 复制并校验输出 JAR**

将构建产物复制到指定输出目录，不部署到 `mods`；记录文件大小与 SHA-256。

**Step 4: 检查工作树**

Run: `git status --short --branch`

Expected: 分支无未提交修改。
