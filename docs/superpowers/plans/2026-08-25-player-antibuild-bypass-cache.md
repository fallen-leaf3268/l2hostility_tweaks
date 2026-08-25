# Player Antibuild Bypass Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the shared UUID antibuild-bypass cache with independent tick caches stored on each `Player` instance and eliminate duplicate arena-trait scans.

**Architecture:** A duck interface exposes a no-argument bypass query. A new Player mixin owns two primitive instance fields and all uncached scan logic; both existing call sites consume the interface directly, allowing the old static helper to be deleted.

**Tech Stack:** Java 17, Forge 1.20.1, Sponge Mixin 0.8.5, Curios API, JUnit Jupiter 5.10.2, Gradle.

## Global Constraints

- Preserve arena-trait, equipment-tag, Curios-tag, and Curios-exception behavior.
- Read `gameTime` from the queried player's own level.
- Preserve one scan per player instance per game tick and next-tick refresh behavior.
- Keep this cache independent from the detector-glasses cache.
- Do not deploy the JAR to a `mods` directory.
- Copy the verified JAR to `C:\Users\Lenovo\Desktop\Ai_Run\output`.

---

### Task 1: Player-owned antibuild-bypass cache

**Files:**
- Create: `src/main/java/com/l2hostility_tweaks/util/AntibuildBypassCache.java`
- Create: `src/main/java/com/l2hostility_tweaks/mixin/PlayerAntibuildBypassCacheMixin.java`
- Create: `src/test/java/com/l2hostility_tweaks/mixin/PlayerAntibuildBypassCacheMixinTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/AntibuildBlockImmuneMixin.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/AntibuildPlaceBypassMixin.java`
- Modify: `src/main/resources/l2hostility_tweaks.mixins.json`
- Delete: `src/main/java/com/l2hostility_tweaks/util/AntibuildBypassHelper.java`

**Interfaces:**
- Produces: `AntibuildBypassCache#l2fix$hasAntibuildBypass()` returning the current player's cached bypass result.
- Consumes: `MobTraitCap`, `L2HFBypassTags.ANTIBUILD_BYPASS`, equipment slots, and the Curios inventory.

- [ ] **Step 1: Write the failing behavior and routing tests**

Create `PlayerAntibuildBypassCacheMixinTest`. Use a `CountingCache` subclass that overrides `l2fix$scanAntibuildBypass()` and verify:

```java
@Test
void cachesWithinGameTickAndRefreshesOnNextTick() {
    var cache = new CountingCache();
    cache.scannedValue = true;

    assertTrue(cache.l2fix$hasAntibuildBypassAtTime(100L));
    assertTrue(cache.l2fix$hasAntibuildBypassAtTime(100L));
    assertEquals(1, cache.scanCount);

    cache.scannedValue = false;
    assertFalse(cache.l2fix$hasAntibuildBypassAtTime(101L));
    assertEquals(2, cache.scanCount);
}
```

Create a second test with two `CountingCache` instances at the same time and opposite values to prove instance isolation. Read both call-site sources and the Mixin JSON to require `AntibuildBypassCache`, require `PlayerAntibuildBypassCacheMixin` registration, forbid `hasArenaTrait` in `AntibuildBlockImmuneMixin`, and require the old helper source to be absent.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.PlayerAntibuildBypassCacheMixinTest
```

Expected: test compilation fails because `PlayerAntibuildBypassCacheMixin` does not exist.

- [ ] **Step 3: Add the cache interface and Player mixin**

Create:

```java
package com.l2hostility_tweaks.util;

public interface AntibuildBypassCache {
    boolean l2fix$hasAntibuildBypass();
}
```

Create `PlayerAntibuildBypassCacheMixin` targeting `Player`. It implements the interface and owns non-static `long l2fix$antibuildBypassTime = Long.MIN_VALUE` and `boolean l2fix$cachedAntibuildBypass`. The public query reads the player's own game time and delegates to `l2fix$hasAntibuildBypassAtTime(long)`. That method refreshes through overridable package-private `l2fix$scanAntibuildBypass()` only when the time changes and stores the boolean before the time.

The production scanner casts `this` to `Player`, returns true for an active arena trait, then scans equipment slots, then Curios slots. Curios exceptions return false exactly as before.

- [ ] **Step 4: Route both callers and remove the helper**

Replace both helper calls with:

```java
((AntibuildBypassCache) player).l2fix$hasAntibuildBypass()
```

For `AntibuildBlockImmuneMixin`, cast `(Object) this` to `AntibuildBypassCache` and remove the separate arena-trait branch. Register `PlayerAntibuildBypassCacheMixin` in the common `mixins` list. Delete `AntibuildBypassHelper.java`.

- [ ] **Step 5: Run the focused test and verify GREEN**

Run:

```powershell
.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.PlayerAntibuildBypassCacheMixinTest
```

Expected: `BUILD SUCCESSFUL`; all tests in the class pass.

- [ ] **Step 6: Commit the cache fix**

```powershell
git add src/main/java/com/l2hostility_tweaks/util/AntibuildBypassCache.java src/main/java/com/l2hostility_tweaks/mixin/PlayerAntibuildBypassCacheMixin.java src/main/java/com/l2hostility_tweaks/mixin/AntibuildBlockImmuneMixin.java src/main/java/com/l2hostility_tweaks/mixin/AntibuildPlaceBypassMixin.java src/main/resources/l2hostility_tweaks.mixins.json src/test/java/com/l2hostility_tweaks/mixin/PlayerAntibuildBypassCacheMixinTest.java
git add -u src/main/java/com/l2hostility_tweaks/util/AntibuildBypassHelper.java
git commit -m "fix: isolate antibuild bypass cache per player"
```

### Task 2: Full verification and deliverable

**Files:**
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Deliver: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: the completed player antibuild-bypass cache from Task 1.
- Produces: a clean, validated release JAR and SHA-256 digest.

- [ ] **Step 1: Run clean verification**

Run:

```powershell
.\gradlew.bat clean test build validateProjectResources
```

Expected: `BUILD SUCCESSFUL`, zero failed tests, and no resource or Mixin registration errors.

- [ ] **Step 2: Inspect the JAR**

Use JDK `jar tf`. Require:

```text
com/l2hostility_tweaks/util/AntibuildBypassCache.class
com/l2hostility_tweaks/mixin/PlayerAntibuildBypassCacheMixin.class
l2hostility_tweaks.mixins.json
```

Forbid `com/l2hostility_tweaks/util/AntibuildBypassHelper.class`.

- [ ] **Step 3: Copy and hash the deliverable**

Copy the built JAR to `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`. Compare source and output SHA-256 values and require equality.

- [ ] **Step 4: Confirm clean worktree**

Remove only untracked runtime logs created by verification after resolving their exact path inside the worktree. Run `git status --short`; expected output is empty.
