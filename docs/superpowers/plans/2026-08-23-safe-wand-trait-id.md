# Safe Wand Trait ID Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 防止封印工具和词条卸载器因非法词条 ID NBT 抛出资源位置异常。

**Architecture:** `TraitWandHelper.parseTraitId(String)` 使用 `ResourceLocation.tryParse()` 进行无异常解析。`getTrait()` 只查询非空解析结果，失败时继续使用现有默认词条。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、JUnit 5、Gradle 8.5。

## Global Constraints

- 不修改物品 NBT。
- 不改变合法和未注册 ID 的现有回退行为。
- 不改变词条切换顺序。
- 不把 JAR 部署到 `mods`。

---

### Task 1: 安全解析物品词条 ID

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/util/TraitWandHelper.java`
- Modify: `src/test/java/com/l2hostility_tweaks/util/TraitWandHelperTest.java`

**Interfaces:**
- Consumes: 任意 `String` 词条 ID。
- Produces: `static ResourceLocation parseTraitId(String)`，合法时返回资源位置，非法时返回 `null`。

- [x] **Step 1: Write the failing tests**

测试 `l2hostility:split` 正常解析，并测试 `Invalid Trait ID` 返回 `null`。

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitWandHelperTest`

Expected: FAIL，因为 `parseTraitId` 尚不存在。

- [x] **Step 3: Write minimal implementation**

```java
static ResourceLocation parseTraitId(String value) {
    return ResourceLocation.tryParse(value);
}
```

将 `getTrait()` 中的直接构造替换为该方法，并只在结果非空时查询注册表。

- [x] **Step 4: Run focused and complete verification**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitWandHelperTest`

Run: `.\gradlew.bat clean build`

Expected: 两条命令均为 `BUILD SUCCESSFUL`，全部测试零失败。

- [x] **Step 5: Commit**

```powershell
git add docs/superpowers/plans/2026-08-23-safe-wand-trait-id.md src/main/java/com/l2hostility_tweaks/util/TraitWandHelper.java src/test/java/com/l2hostility_tweaks/util/TraitWandHelperTest.java
git commit -m "fix: safely parse wand trait ids"
```
