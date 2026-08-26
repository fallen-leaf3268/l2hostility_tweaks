# LivingEntity Immunity Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the shared one-entity force/gravity immunity caches with isolated per-`LivingEntity` caches that invalidate by tick and tag generation.

**Architecture:** A duck interface exposes force and gravity queries keyed by one 64-bit cache stamp. A `LivingEntity` Mixin owns two independent stamp/result pairs; `ImmunityHelper` only generates stamps, performs scans, and advances the generation on reload.

**Tech Stack:** Java 17, Forge 1.20.1, Sponge Mixin 0.8.5, JUnit 5, Gradle 8.5

## Global Constraints

- Preserve the existing Curios-first, trait-tag-second immunity semantics and debug logging.
- Force and gravity caches are independent and owned by each `LivingEntity` instance.
- Cache stamp is `((long) generation << 32) | (tickCount & 0xffffffffL)`.
- Instance stamps are volatile publication fields and reload generation increments atomically.
- A tag reload invalidates cached entity results even when the entity tick is unchanged.
- Remove all shared entity `WeakReference`, tick, and boolean cache fields.
- Do not add configuration, packets, tooltip text, or deploy the JAR to `mods`.

---

### Task 1: Per-entity cache behavior

**Files:**
- Create: `src/main/java/com/l2hostility_tweaks/util/EntityImmunityCache.java`
- Create: `src/main/java/com/l2hostility_tweaks/mixin/LivingEntityImmunityCacheMixin.java`
- Create: `src/test/java/com/l2hostility_tweaks/mixin/LivingEntityImmunityCacheMixinTest.java`

**Interfaces:**
- Produces: `boolean l2fix$isImmuneToForce(long stamp)` and `boolean l2fix$isImmuneToGravity(long stamp)`.
- Consumes later: `ImmunityHelper.computeImmuneToForce(LivingEntity)` and `computeImmuneToGravity(LivingEntity)`.

- [ ] **Step 1: Write failing behavior tests**

Create a counting subclass of `LivingEntityImmunityCacheMixin` that overrides force and gravity scan hooks. Assert two queries with the same stamp scan once; a new stamp rescans; force and gravity scan independently; two instances at the same stamp retain different results; and a scan result is observable only after its corresponding stamp is stored.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.LivingEntityImmunityCacheMixinTest`

Expected: test compilation fails because the cache Mixin does not exist.

- [ ] **Step 3: Implement the duck interface and Mixin**

Use `Long.MIN_VALUE` as the initial stamp for each cache. On mismatch, execute the matching scan hook, store the boolean result, then store the stamp. The scan hooks call the corresponding public calculation method in `ImmunityHelper`; do not copy scan logic.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.LivingEntityImmunityCacheMixinTest`

Expected: all cache behavior tests pass.

- [ ] **Step 5: Commit**

Commit message: `fix: add per-entity immunity cache`

### Task 2: Runtime integration and generation invalidation

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/util/ImmunityHelper.java`
- Modify: `src/test/java/com/l2hostility_tweaks/util/ImmunityHelperCacheTest.java`
- Modify: `src/main/resources/l2hostility_tweaks.mixins.json`
- Test: `src/test/java/com/l2hostility_tweaks/mixin/LivingEntityImmunityCacheMixinTest.java`

**Interfaces:**
- Consumes: `EntityImmunityCache` from Task 1.
- Produces: package-visible `cacheStamp(int generation, int tick)` for exact stamp tests and generation-based invalidation in `invalidateTagCaches()`.

- [ ] **Step 1: Write failing integration and invalidation tests**

Replace reflection assertions for the old weak-reference cache with assertions that `cacheStamp(7, -1)` preserves both 32-bit values, `invalidateTagCaches()` changes the current stamp at a fixed tick, runtime source casts the entity to `EntityImmunityCache`, the common Mixin list registers `LivingEntityImmunityCacheMixin`, and all six old static entity-cache field names plus `WeakReference` are absent.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.ImmunityHelperCacheTest --tests com.l2hostility_tweaks.mixin.LivingEntityImmunityCacheMixinTest`

Expected: assertions fail because `ImmunityHelper` still owns the shared cache and the Mixin is not registered.

- [ ] **Step 3: Integrate the entity cache**

Make both compute methods public static, replace the shared-cache branches with:

```java
long stamp = cacheStamp(immunityCacheGeneration, entity.tickCount);
return ((EntityImmunityCache) entity).l2fix$isImmuneToForce(stamp);
```

and the gravity equivalent. Remove the six static entity-cache fields and `WeakReference` import. Increment `immunityCacheGeneration` after clearing `traitTagCache` in `invalidateTagCaches()`.

- [ ] **Step 4: Register the common Mixin**

Add `"LivingEntityImmunityCacheMixin"` to the common `mixins` array. Do not place it in `client` or `server` only.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.ImmunityHelperCacheTest --tests com.l2hostility_tweaks.mixin.LivingEntityImmunityCacheMixinTest`

Expected: all selected tests pass.

- [ ] **Step 6: Commit**

Commit message: `fix: isolate living entity immunity caches`

### Task 3: Review, full verification, and artifact

**Files:**
- Output: `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Tasks 1 and 2.
- Produces: independently reviewed source and verified output JAR.

- [ ] **Step 1: Run static safety checks**

Run `git diff --check`; confirm the old weak-reference and split static cache fields no longer occur in runtime sources; confirm the new interface, Mixin, and common Mixin registration exist.

- [ ] **Step 2: Request independent code review**

Review the implementation against the design and this plan. Fix every Critical and Important finding using a failing regression test first, then request re-review.

- [ ] **Step 3: Run the complete lifecycle**

Run: `.\gradlew.bat clean test check build`

Expected: exit code 0, all tests pass, and all 15 tasks complete successfully.

- [ ] **Step 4: Inspect and output the JAR**

Confirm the JAR contains `EntityImmunityCache.class`, `LivingEntityImmunityCacheMixin.class`, `ImmunityHelper.class`, and the updated Mixin JSON. Copy it to `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`, verify source/output SHA-256 equality, and do not copy it to `mods`.

- [ ] **Step 5: Report verification evidence**

Report test count, review conclusion, build result, JAR size, SHA-256, output path, branch state, and worktree cleanliness.
