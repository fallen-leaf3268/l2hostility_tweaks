# 玩家自身词条指数消耗说明修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让玩家自身词条指数消耗模式的配置说明与现有 `1、2、4、8……` 实际序列一致。

**Architecture:** 只修改 `L2HConfig` 中 `playerSelfTraitCostMode` 的配置注释。现有配置测试通过读取源码保护完整的新说明并拒绝旧公式，不改服务端、GUI 或退款计算。

**Tech Stack:** Java 17、Forge 1.20.1、JUnit 5、Gradle

## Global Constraints

- 保留现有指数消耗序列 `1、2、4、8……`。
- 服务端扣除、GUI 提示、单级卸载退款和全部卸载退款逻辑均不修改。
- 配置说明必须写成 `2^当前等级` 并列出序列示例。
- JAR 只输出到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`。

---

### Task 1: 修正指数消耗配置说明

**Files:**
- Modify: `src/test/java/com/l2hostility_tweaks/config/L2HConfigTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/config/L2HConfig.java:260-263`

**Interfaces:**
- Consumes: `playerSelfTraitCostMode` 的 Forge 配置注释。
- Produces: 与运行逻辑一致的完整指数消耗说明，不改变任何方法签名或费用返回值。

- [ ] **Step 1: 写配置说明失败测试**

在 `L2HConfigTest` 中加入 `IOException`、`Files` 和 `Path` 导入，并添加：

```java
@Test
void exponentialCostDescriptionMatchesRuntimeSchedule() throws IOException {
    String source = Files.readString(Path.of(
            "src/main/java/com/l2hostility_tweaks/config/L2HConfig.java"));

    assertTrue(source.contains(
            "3 = 指数: 消耗 2^当前等级 个（依次为 1、2、4、8……）"));
    assertFalse(source.contains("2^(当前等级 - 1)"));
}
```

- [ ] **Step 2: 运行测试并确认先失败**

Run: `./gradlew test --tests com.l2hostility_tweaks.config.L2HConfigTest.exponentialCostDescriptionMatchesRuntimeSchedule`

Expected: FAIL；新说明不存在，旧公式仍存在。

- [ ] **Step 3: 最小修改配置注释**

将 `playerSelfTraitCostMode` 的第三行说明从：

```java
"3 = 指数: 消耗 2^(当前等级 - 1) 个")
```

改为：

```java
"3 = 指数: 消耗 2^当前等级 个（依次为 1、2、4、8……）")
```

- [ ] **Step 4: 运行定向测试并确认通过**

Run: `./gradlew test --tests com.l2hostility_tweaks.config.L2HConfigTest.exponentialCostDescriptionMatchesRuntimeSchedule`

Expected: 1 test completed，0 failed。

- [ ] **Step 5: 检查费用逻辑未被修改**

Run: `git diff -- src/main/java/com/l2hostility_tweaks/config/L2HConfig.java src/test/java/com/l2hostility_tweaks/config/L2HConfigTest.java`

Expected: 生产代码差异只有一行配置注释；测试差异只有导入和新测试。

- [ ] **Step 6: 提交说明修复**

```bash
git add src/main/java/com/l2hostility_tweaks/config/L2HConfig.java src/test/java/com/l2hostility_tweaks/config/L2HConfigTest.java
git commit -m "fix: correct exponential self trait cost description"
```

### Task 2: 全量验证与 JAR 输出

**Files:**
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 1 的配置说明修改。
- Produces: 通过完整构建和资源校验的验证 JAR。

- [ ] **Step 1: 执行干净全量验证**

Run: `./gradlew clean test build validateProjectResources`

Expected: `BUILD SUCCESSFUL`，所有测试 0 failures、0 errors、0 skipped。

- [ ] **Step 2: 检查 JAR 配置类与资源**

Run: `D:\JAVA\JAVA_17\bin\jar.exe tf build/libs/l2hostility_tweaks-1.0.0.jar`

Expected: 包含 `com/l2hostility_tweaks/config/L2HConfig.class` 和 `META-INF/mods.toml`。

- [ ] **Step 3: 输出 JAR 并核对摘要**

Run: `Copy-Item -LiteralPath 'build/libs/l2hostility_tweaks-1.0.0.jar' -Destination 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar' -Force`

Run: `Get-FileHash -Algorithm SHA256 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar'`

Expected: 输出 JAR 存在并返回 SHA-256，不写入任何 `mods` 目录。

- [ ] **Step 4: 检查工作树状态**

Run: `git status --short`

Expected: 无未提交文件。
