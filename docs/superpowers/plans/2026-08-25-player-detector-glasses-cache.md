# Player Detector Glasses Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the UUID-based detector-glasses cache with independent tick caches stored directly on each `Player` instance.

**Architecture:** A small duck interface exposes the cached glasses query. A new `Player` mixin owns two primitive instance fields and implements the query, while `EntityInvisibleMixin` consumes the interface and retains all existing reveal behavior.

**Tech Stack:** Java 17, Forge 1.20.1, Sponge Mixin 0.8.5, JUnit Jupiter 5.10.2, Gradle.

## Global Constraints

- Preserve `CurioCompat.hasItemInCurioOrSlot` slot-filter semantics.
- Preserve one scan per player instance per tick and next-tick refresh behavior.
- Do not merge this cache with `DetectorGlowState`.
- Do not deploy the JAR to a `mods` directory.
- Copy the verified JAR to `C:\Users\Lenovo\Desktop\Ai_Run\output`.

---

### Task 1: Player-owned detector-glasses cache

**Files:**
- Create: `src/main/java/com/l2hostility_tweaks/util/DetectorGlassesCache.java`
- Create: `src/main/java/com/l2hostility_tweaks/mixin/PlayerDetectorGlassesCacheMixin.java`
- Create: `src/test/java/com/l2hostility_tweaks/mixin/PlayerDetectorGlassesCacheMixinTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/EntityInvisibleMixin.java`
- Modify: `src/main/resources/l2hostility_tweaks.mixins.json`

**Interfaces:**
- Produces: `DetectorGlassesCache#l2fix$hasDetectorGlasses()` returning the current instance's cached equipment state.
- Consumes: `CurioCompat.hasItemInCurioOrSlot(LivingEntity, Item)` and `ForgeRegistries.ITEMS`.

- [ ] **Step 1: Write the failing behavioral and integration tests**

Create `PlayerDetectorGlassesCacheMixinTest` with tests that instantiate two mixin cache owners and verify:

```java
@Test
void cachesWithinTickAndRefreshesOnNextTick() {
    var cache = new PlayerDetectorGlassesCacheMixin();
    cache.l2fix$storeDetectorGlasses(12, true);

    assertTrue(cache.l2fix$isDetectorGlassesCacheValid(12));
    assertFalse(cache.l2fix$isDetectorGlassesCacheValid(13));
    assertTrue(cache.l2fix$getCachedDetectorGlasses());
}

@Test
void differentPlayerInstancesKeepIndependentValues() {
    var first = new PlayerDetectorGlassesCacheMixin();
    var second = new PlayerDetectorGlassesCacheMixin();
    first.l2fix$storeDetectorGlasses(20, true);
    second.l2fix$storeDetectorGlasses(20, false);

    assertTrue(first.l2fix$getCachedDetectorGlasses());
    assertFalse(second.l2fix$getCachedDetectorGlasses());
}
```

Also read the source and Mixin JSON to assert that `PlayerDetectorGlassesCacheMixin` targets `Player`, implements `DetectorGlassesCache`, is registered, `EntityInvisibleMixin` calls the interface, and no longer contains `Map<UUID`, `ConcurrentHashMap`, or `playerGlassesTick`.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.PlayerDetectorGlassesCacheMixinTest
```

Expected: test compilation fails because `PlayerDetectorGlassesCacheMixin` does not exist yet.

- [ ] **Step 3: Add the cache interface and Player mixin**

Create the interface:

```java
package com.l2hostility_tweaks.util;

public interface DetectorGlassesCache {
    boolean l2fix$hasDetectorGlasses();
}
```

Create `PlayerDetectorGlassesCacheMixin` targeting `Player`. It implements the interface, owns a non-static `int` tick initialized to `Integer.MIN_VALUE` and a non-static boolean value, and caches the registry item statically. Its public query casts `this` to `Player`, refreshes only when `tickCount` changes, calls `CurioCompat.hasItemInCurioOrSlot`, stores the boolean before the tick, and returns the cached boolean. Package-private instance methods `l2fix$isDetectorGlassesCacheValid(int)`, `l2fix$storeDetectorGlasses(int, boolean)`, and `l2fix$getCachedDetectorGlasses()` encapsulate and expose the actual cache transitions exercised by the tests.

- [ ] **Step 4: Route visibility checks through the player instance**

Update `EntityInvisibleMixin` so `l2fix$playerHasDetectorGlasses` is:

```java
private static boolean l2fix$playerHasDetectorGlasses(Player player) {
    return ((DetectorGlassesCache) player).l2fix$hasDetectorGlasses();
}
```

Delete the detector item field and lookup method from this mixin, both UUID maps, UUID/Map/concurrent imports, scan logic, and the size-based online-player cleanup.

Register `PlayerDetectorGlassesCacheMixin` in the common `mixins` list.

- [ ] **Step 5: Run the focused test and verify GREEN**

Run:

```powershell
.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.PlayerDetectorGlassesCacheMixinTest
```

Expected: `BUILD SUCCESSFUL`; all tests in the class pass.

- [ ] **Step 6: Commit the cache fix**

```powershell
git add src/main/java/com/l2hostility_tweaks/util/DetectorGlassesCache.java src/main/java/com/l2hostility_tweaks/mixin/PlayerDetectorGlassesCacheMixin.java src/main/java/com/l2hostility_tweaks/mixin/EntityInvisibleMixin.java src/main/resources/l2hostility_tweaks.mixins.json src/test/java/com/l2hostility_tweaks/mixin/PlayerDetectorGlassesCacheMixinTest.java
git commit -m "fix: isolate detector glasses cache per player"
```

### Task 2: Full verification and deliverable

**Files:**
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Deliver: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: the completed player cache implementation from Task 1.
- Produces: a clean, validated release JAR and its SHA-256 digest.

- [ ] **Step 1: Run clean verification**

Run:

```powershell
.\gradlew.bat clean test build validateProjectResources
```

Expected: `BUILD SUCCESSFUL`, zero failed tests, and no resource or Mixin registration errors.

- [ ] **Step 2: Inspect the JAR**

Use JDK `jar tf` and assert the archive includes:

```text
com/l2hostility_tweaks/util/DetectorGlassesCache.class
com/l2hostility_tweaks/mixin/PlayerDetectorGlassesCacheMixin.class
l2hostility_tweaks.mixins.json
```

- [ ] **Step 3: Copy and hash the deliverable**

Copy the built JAR to `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`, then run `Get-FileHash -Algorithm SHA256` and record its size and digest.

- [ ] **Step 4: Confirm clean worktree**

Run `git status --short`. Expected: no output after ignoring or removing generated runtime logs only; do not remove user files.
