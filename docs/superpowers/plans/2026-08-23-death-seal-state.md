# Death Seal State Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 玩家在保留词条的死亡重生后，负等级词条仍能被识别并按原有到期时间正常解封。

**Architecture:** `MobTraitCap.traits` 的原始等级作为权威数据。`TraitDisableHelper.syncSealedLevelData(CompoundTag, String, int)` 将原始等级投影到封印等级 NBT；玩家克隆恢复循环在初始化每个词条前调用该方法。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、JUnit 5、Gradle 8.5。

## Global Constraints

- 不改变封印持续时间和到期时间快照。
- 不改变玩家词条等级、初始化顺序和能力同步流程。
- 不改变 `keepInventory=false` 的清除行为。
- 不把 JAR 部署到 `mods`。

---

### Task 1: 从原始等级同步封印等级 NBT

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java`
- Create: `src/test/java/com/l2hostility_tweaks/util/TraitDisableHelperTest.java`

**Interfaces:**
- Consumes: `CompoundTag data`、`String traitId`、`int rawLevel`。
- Produces: `public static void syncSealedLevelData(CompoundTag, String, int)`。

- [x] **Step 1: Write the failing tests**

测试负等级 `-3` 写入 `l2htweaks_sealed_level_<ID>=3`，并测试正等级删除同键的陈旧值。

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: FAIL，因为 `syncSealedLevelData` 尚不存在。

- [x] **Step 3: Write minimal implementation**

```java
public static void syncSealedLevelData(CompoundTag data, String traitId, int rawLevel) {
    String key = sealedLevelKey(traitId);
    if (rawLevel < 0) {
        data.putInt(key, Math.abs(rawLevel));
    } else {
        data.remove(key);
    }
}
```

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: PASS。

### Task 2: 接入死亡克隆恢复

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/L2HostilityFix.java`

**Interfaces:**
- Consumes: Task 1 的 `syncSealedLevelData`。
- Produces: 每个恢复词条的原始等级与封印等级 NBT 一致。

- [x] **Step 1: Call the synchronization method**

在 `onPlayerClone` 恢复循环取得 `level` 后、执行 `initialize` 前调用：

```java
TraitDisableHelper.syncSealedLevelData(
        newPlayer.getPersistentData(), entry.getKey().getID(), level);
```

- [x] **Step 2: Run focused and complete verification**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Run: `.\gradlew.bat clean build`

Expected: 两条命令均为 `BUILD SUCCESSFUL`，全部测试零失败。

- [x] **Step 3: Commit**

```powershell
git add docs/superpowers/plans/2026-08-23-death-seal-state.md src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java src/main/java/com/l2hostility_tweaks/L2HostilityFix.java src/test/java/com/l2hostility_tweaks/util/TraitDisableHelperTest.java
git commit -m "fix: restore sealed state after player death"
```
