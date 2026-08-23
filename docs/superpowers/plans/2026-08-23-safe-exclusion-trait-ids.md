# Safe Exclusion Trait IDs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Filter syntactically invalid exclusion trait IDs at configuration parsing and prevent Tooltip name lookup from throwing on any remaining invalid ID.

**Architecture:** `L2HConfig` owns normalization of raw exclusion-group strings and retains only syntactically valid resource IDs. `TraitSymbolMixin` independently parses the ID safely before registry lookup and falls back to literal text. Registry membership is deliberately not validated so optional addon IDs remain supported.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, JUnit 5, Gradle 8.5

## Global Constraints

- Preserve valid exclusion behavior and rule names.
- Preserve syntactically valid but currently unregistered trait IDs.
- Do not rewrite the user's TOML configuration.
- Do not deploy the generated JAR to `mods`.

---

### Task 1: Filter invalid IDs during exclusion parsing

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/config/L2HConfig.java`
- Test: `src/test/java/com/l2hostility_tweaks/config/L2HConfigTest.java`

**Interfaces:**
- Consumes: raw `List<? extends String>` entries in `rule,id,id` form.
- Produces: package-private `static List<ExclusionGroup> parseExclusionGroups(List<? extends String> raw)`.

- [x] **Step 1: Add a failing parser regression test**

Add a test that parses `first,l2hostility:gravity,Invalid Trait ID,addon:future_trait` and asserts the resulting IDs equal `l2hostility:gravity` and `addon:future_trait`.

- [x] **Step 2: Run the focused test and confirm RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.config.L2HConfigTest`

Expected: compilation fails because `parseExclusionGroups` does not exist.

- [x] **Step 3: Extract and harden the parser**

Make `getExclusionGroups()` cache the result of `parseExclusionGroups(COMMON.exclusionGroups.get())`. In the new parser, trim each ID and add it only when `ResourceLocation.tryParse(id) != null`; add a group only when at least one valid ID remains.

- [x] **Step 4: Run the focused test and confirm GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.config.L2HConfigTest`

Expected: all `L2HConfigTest` tests pass.

### Task 2: Defend Tooltip name lookup

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolMixin.java`
- Test: `src/test/java/com/l2hostility_tweaks/mixin/TraitSymbolMixinTest.java`

**Interfaces:**
- Consumes: nullable `Registry<MobTrait>` and raw trait ID string.
- Produces: package-private `static Component l2fix$getTraitName(Registry<MobTrait> registry, String traitId)`.

- [x] **Step 1: Add a failing Tooltip fallback test**

Add a test that calls `l2fix$getTraitName(null, "Invalid Trait ID")` and asserts the component string equals the original ID.

- [x] **Step 2: Run the focused test and confirm RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.TraitSymbolMixinTest`

Expected: compilation fails because `l2fix$getTraitName` does not exist.

- [x] **Step 3: Implement safe Tooltip lookup**

Have the existing instance path call `l2fix$getTraitName(getTraitRegistry(), traitId)`. The helper uses `ResourceLocation.tryParse`; it queries the registry only when both registry and parsed ID are non-null, otherwise returning `Component.literal(traitId)`.

- [x] **Step 4: Run the focused test and confirm GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.TraitSymbolMixinTest`

Expected: all `TraitSymbolMixinTest` tests pass.

### Task 3: Verify and commit

**Files:**
- Verify all files changed by Tasks 1 and 2.

**Interfaces:**
- Consumes: the two regression-tested protections.
- Produces: a clean build, reviewed diff, generated JAR, and one implementation commit.

- [x] **Step 1: Run both focused suites**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.config.L2HConfigTest --tests com.l2hostility_tweaks.mixin.TraitSymbolMixinTest`

Expected: all focused tests pass.

- [x] **Step 2: Run the complete build**

Run: `.\gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL` with zero test failures.

- [x] **Step 3: Review and clean generated logs**

Run `git diff --check`, inspect the diff and test totals, then remove only the validated project-local `logs` directory if Gradle generated it.

- [x] **Step 4: Commit the implementation**

Stage the plan, two production files, and two test files. Commit with `fix: safely handle exclusion trait ids` and confirm `git status --short` is empty.
