# HUD 与 BossBar 状态统一实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 HUD 预计算和实际渲染使用同一套 BossBar 互斥规则，避免原版 BossBar 与自定义血条同时消失。

**Architecture:** 在 `L2HHealthOverlay` 内新增包内可测试的 `HudState` 记录和纯方法 `l2fix$resolveHudState`。两个渲染阶段只负责采集输入和应用返回状态，不再各自维护互斥判断。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、JUnit 5、Gradle 8.5。

## Global Constraints

- 没有有效词条目标时隐藏自定义血条并保留原版 BossBar。
- 有目标且没有 BossBar 时显示自定义血条并隐藏原版 BossBar。
- 有目标、有 BossBar 且开启 `hideHudWithBossbar` 时隐藏自定义血条并保留原版 BossBar。
- 有目标、有 BossBar 且关闭 `hideHudWithBossbar` 时显示自定义血条并隐藏原版 BossBar。
- 目标实体 ID 只在自定义 HUD 显示时保留，否则重置为 `-1`。
- 不修改目标检测、显示距离、血条样式、伤害动画、词条图标或 BossBar 活跃状态采集。
- 不把 JAR 部署到 `mods`；验证产物复制到 `C:\Users\Lenovo\Desktop\Ai_Run\output`。

---

### Task 1: 统一 HUD 与 BossBar 状态判定

**Files:**
- Create: `src/test/java/com/l2hostility_tweaks/client/L2HHealthOverlayTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java`

**Interfaces:**
- Consumes: `boolean hasValidTarget`、`boolean bossEventsActive`、`boolean hideHudWithBossbar`。
- Produces: `L2HHealthOverlay.HudState` 和 `static HudState l2fix$resolveHudState(boolean, boolean, boolean)`。

- [ ] **Step 1: Write the failing state-matrix test**

```java
package com.l2hostility_tweaks.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class L2HHealthOverlayTest {

	@Test
	void resolvesHudAndBossbarVisibilityFromOneStateMatrix() {
		assertEquals(new L2HHealthOverlay.HudState(false, false),
				L2HHealthOverlay.l2fix$resolveHudState(false, false, false));
		assertEquals(new L2HHealthOverlay.HudState(true, true),
				L2HHealthOverlay.l2fix$resolveHudState(true, false, true));
		assertEquals(new L2HHealthOverlay.HudState(false, false),
				L2HHealthOverlay.l2fix$resolveHudState(true, true, true));
		assertEquals(new L2HHealthOverlay.HudState(true, true),
				L2HHealthOverlay.l2fix$resolveHudState(true, true, false));
	}
}
```

- [ ] **Step 2: Run the focused test to verify RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.client.L2HHealthOverlayTest`

Expected: FAIL during `compileTestJava` because `HudState` and `l2fix$resolveHudState` do not exist.

- [ ] **Step 3: Add the minimal shared state resolver**

Add inside `L2HHealthOverlay`:

```java
	record HudState(boolean hudActive, boolean hideBossBars) {}

	static HudState l2fix$resolveHudState(boolean hasValidTarget,
			boolean bossEventsActive, boolean hideHudWithBossbar) {
		boolean showCustomHud = hasValidTarget && !(hideHudWithBossbar && bossEventsActive);
		return new HudState(showCustomHud, showCustomHud);
	}
```

In both `render()` and `precomputeHudState()`:

1. When the global HUD is disabled or the client world/player is unavailable, set `hudActive = false`、`hideBossBars = false`、`trackedEntityId = -1` and return.
2. Treat an empty target or player target as `hasValidTarget = false`.
3. Call `l2fix$resolveHudState(hasValidTarget, bossEventsActive, ClientL2HConfig.CLIENT.hideHudWithBossbar.get())`.
4. Assign the returned `hudActive` and `hideBossBars` values to the static fields.
5. Set `trackedEntityId` to the target ID only when `state.hudActive()` is true; otherwise set it to `-1`.
6. In `render()`, call `renderHealthBar` only when `state.hudActive()` is true.

- [ ] **Step 4: Run the focused test to verify GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.client.L2HHealthOverlayTest`

Expected: BUILD SUCCESSFUL with the state-matrix test passing.

- [ ] **Step 5: Inspect the focused diff**

Run: `git diff --check` and `git diff -- src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java src/test/java/com/l2hostility_tweaks/client/L2HHealthOverlayTest.java`

Expected: no whitespace errors; only shared HUD state resolution and its regression test changed.

- [ ] **Step 6: Commit the fix**

```powershell
git add src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java src/test/java/com/l2hostility_tweaks/client/L2HHealthOverlayTest.java
git commit -m "fix: unify hud and bossbar visibility"
```

### Task 2: Complete verification and JAR output

**Files:**
- Verify: `build/test-results/test/TEST-*.xml`
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 1 committed implementation.
- Produces: clean test evidence and a hashed verification JAR.

- [ ] **Step 1: Run a clean full build**

Run: `.\gradlew.bat --no-daemon --console=plain clean build`

Expected: BUILD SUCCESSFUL; all tests report zero failures and zero errors.

- [ ] **Step 2: Verify test totals and JAR content**

Read all `build/test-results/test/TEST-*.xml`, sum `tests`, `failures`, `errors`, and `skipped`, and verify `com/l2hostility_tweaks/client/L2HHealthOverlay.class` is present in the JAR with:

```powershell
& 'D:\JAVA\JAVA_17\bin\jar.exe' tf 'build\libs\l2hostility_tweaks-1.0.0.jar' |
    Select-String 'com/l2hostility_tweaks/client/L2HHealthOverlay.class'
```

Expected: failures `0`, errors `0`, and the class entry is present.

- [ ] **Step 3: Copy and hash the verification JAR**

```powershell
Copy-Item -LiteralPath 'build\libs\l2hostility_tweaks-1.0.0.jar' `
    -Destination 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar' -Force
Get-FileHash -Algorithm SHA256 `
    'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar'
```

Expected: output JAR exists, has non-zero size, and SHA-256 is reported.

- [ ] **Step 4: Confirm repository state**

Run: `git status --short` and `git log -1 --oneline`.

Expected: worktree is clean and the latest code commit is `fix: unify hud and bossbar visibility`.
