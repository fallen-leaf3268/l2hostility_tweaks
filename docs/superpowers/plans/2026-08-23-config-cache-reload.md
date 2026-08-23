# Config Cache Reload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Invalidate parsed common and client configuration caches when Forge reloads the matching config specification.

**Architecture:** Each config class owns one package-visible invalidation method that clears all caches belonging to that class. `L2HostilityFix` listens for `ModConfigEvent.Reloading`, routes by exact config-spec identity, and leaves unrelated config events untouched. Cached references are volatile so subsequent game and render threads observe invalidation.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, JUnit 5, Gradle 8.5

## Global Constraints

- Preserve all existing defaults and parsing rules.
- Do not parse on every HUD frame or gameplay call.
- Do not modify or rewrite TOML files.
- Do not process other mods' config events.
- Do not deploy the generated JAR to `mods`.

---

### Task 1: Invalidate all common parsed caches

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/config/L2HConfig.java`
- Test: `src/test/java/com/l2hostility_tweaks/config/L2HConfigTest.java`

**Interfaces:**
- Consumes: the seven static parsed-cache references owned by `L2HConfig`.
- Produces: package-visible `static void invalidateCaches()`.

- [x] **Step 1: Add a failing cache invalidation test**

Use reflection to assign non-null sentinel collections to `parsedLevelThresholds`, `parsedPerTraitThresholds`, `parsedLegendaryThresholds`, `parsedExtraLegendaryIds`, `parsedExclusionGroups`, `parsedSealDurationArray`, and `parsedPlayerTraitOverrides`. Call `L2HConfig.invalidateCaches()` and assert all seven fields are null.

- [x] **Step 2: Run the focused test and confirm RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.config.L2HConfigTest`

Expected: compilation fails because `invalidateCaches()` does not exist.

- [x] **Step 3: Implement common invalidation**

Declare the seven parsed-cache references `volatile`. Replace the unused single-cache invalidator with `static void invalidateCaches()` that assigns null to all seven references.

- [x] **Step 4: Run the focused test and confirm GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.config.L2HConfigTest`

Expected: all common config tests pass.

### Task 2: Invalidate the client color cache

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/client/config/ClientL2HConfig.java`
- Create: `src/test/java/com/l2hostility_tweaks/client/config/ClientL2HConfigTest.java`

**Interfaces:**
- Consumes: `parsedColorSegments`.
- Produces: public `static void invalidateCaches()` for cross-package event routing.

- [x] **Step 1: Add a failing client cache test**

Set `parsedColorSegments` to a non-null list with reflection, call `ClientL2HConfig.invalidateCaches()`, and assert the field is null.

- [x] **Step 2: Run the focused test and confirm RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.client.config.ClientL2HConfigTest`

Expected: compilation fails because `invalidateCaches()` does not exist.

- [x] **Step 3: Implement client invalidation**

Declare `parsedColorSegments` volatile and add `public static void invalidateCaches()` that assigns it null.

- [x] **Step 4: Run the focused test and confirm GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.client.config.ClientL2HConfigTest`

Expected: the client config test passes.

### Task 3: Route Forge reload events

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/L2HostilityFix.java`
- Create: `src/main/java/com/l2hostility_tweaks/config/ConfigCacheReloadHandler.java`
- Create: `src/test/java/com/l2hostility_tweaks/config/ConfigCacheReloadHandlerTest.java`

**Interfaces:**
- Consumes: `IConfigSpec<?>` from `ModConfigEvent.Reloading.getConfig().getSpec()`.
- Produces: `ConfigCacheReloadHandler.invalidate(IConfigSpec<?> spec)` and a private reload-event listener.

- [x] **Step 1: Add a failing event-routing test**

Populate one common cache and the client cache with reflection. Call `ConfigCacheReloadHandler.invalidate(L2HConfig.SPEC)` and assert only the common cache is cleared; repopulate both, call with `ClientL2HConfig.CLIENT_SPEC`, and assert only the client cache is cleared.

- [x] **Step 2: Run the focused test and confirm RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.config.ConfigCacheReloadHandlerTest`

Expected: compilation fails because `ConfigCacheReloadHandler` does not exist.

- [x] **Step 3: Implement and register event routing**

Add the isolated `ConfigCacheReloadHandler` using identity comparisons against `L2HConfig.SPEC` and `ClientL2HConfig.CLIENT_SPEC`. Register `this::onConfigReload` on the mod event bus and have the listener pass the event config's spec to the handler.

- [x] **Step 4: Run the focused test and confirm GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.config.ConfigCacheReloadHandlerTest`

Expected: the routing test passes and production code compiles against Forge 1.20.1.

### Task 4: Verify and commit

**Files:**
- Verify all files changed by Tasks 1–3.

**Interfaces:**
- Consumes: cache invalidation and event routing implementation.
- Produces: clean build, reviewed diff, generated JAR, and one implementation commit.

- [x] **Step 1: Run all related tests**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.config.L2HConfigTest --tests com.l2hostility_tweaks.client.config.ClientL2HConfigTest --tests com.l2hostility_tweaks.config.ConfigCacheReloadHandlerTest`

Expected: all related tests pass.

- [x] **Step 2: Run the complete build**

Run: `.\gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL` with zero test failures.

- [x] **Step 3: Review and clean generated logs**

Run `git diff --check`, inspect the diff and XML test totals, then remove only the validated project-local `logs` directory if Gradle generated it.

- [x] **Step 4: Commit the implementation**

Stage the plan, four production files, and three test files. Commit with `fix: refresh parsed config caches on reload` and confirm `git status --short` is empty.
