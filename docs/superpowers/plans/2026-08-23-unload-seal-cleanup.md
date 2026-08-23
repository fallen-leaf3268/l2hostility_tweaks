# Unload Seal Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 卸载词条时完整清除封印等级和到期时间，阻止旧等级被后续解封恢复。

**Architecture:** `TraitDisableHelper.clearSealData(CompoundTag, String)` 统一管理两类封印键。词条卸载器的单级、整组和全部模式都调用该方法，不执行完整解封流程。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、JUnit 5、Gradle 8.5。

## Global Constraints

- 不改变卸载等级和返还数量。
- 不改变词条初始化、生命值比例和能力同步顺序。
- 不处理本项以外的封印生命周期路径。
- 不把 JAR 部署到 `mods`。

---

### Task 1: 统一清除封印元数据

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java`
- Modify: `src/test/java/com/l2hostility_tweaks/util/TraitDisableHelperTest.java`

**Interfaces:**
- Consumes: `CompoundTag data`、`String traitId`。
- Produces: `public static void clearSealData(CompoundTag, String)`。

- [x] **Step 1: Write the failing test**

预置 `l2htweaks_sealed_level_<ID>` 和 `sealExpiryKey(ID)`，调用 `clearSealData` 后断言两者均不存在。

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: FAIL，因为 `clearSealData` 尚不存在。

- [x] **Step 3: Write minimal implementation**

```java
public static void clearSealData(CompoundTag data, String traitId) {
    data.remove(sealedLevelKey(traitId));
    data.remove(sealExpiryKey(traitId));
}
```

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: PASS。

### Task 2: 接入三种卸载模式

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/content/TraitUnloaderWand.java`

**Interfaces:**
- Consumes: Task 1 的 `clearSealData`。
- Produces: 单级、整组和全部卸载均不遗留封印元数据。

- [x] **Step 1: Replace all three single-key removals**

将三处：

```java
player.getPersistentData().remove(TraitDisableHelper.sealExpiryKey(trait.getID()));
```

替换为：

```java
TraitDisableHelper.clearSealData(player.getPersistentData(), trait.getID());
```

- [x] **Step 2: Run focused and complete verification**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Run: `.\gradlew.bat clean build`

Expected: 两条命令均为 `BUILD SUCCESSFUL`，全部测试零失败。

- [x] **Step 3: Commit**

```powershell
git add docs/superpowers/plans/2026-08-23-unload-seal-cleanup.md src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java src/main/java/com/l2hostility_tweaks/content/TraitUnloaderWand.java src/test/java/com/l2hostility_tweaks/util/TraitDisableHelperTest.java
git commit -m "fix: clear seal data when unloading traits"
```
