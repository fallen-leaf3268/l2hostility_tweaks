# Legendary Trait Color Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Guarantee a gold default color for KubeJS legendary self-effect and target-effect traits while preserving explicit custom colors.

**Architecture:** Both affected builders establish the normal creation invariant by supplying `ChatFormatting.GOLD` when no script color is configured. Both trait constructors independently normalize a null `IntSupplier` to a gold supplier so direct construction and future builders remain safe. Runtime `getColor()` stays branch-free.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, KubeJS compatibility API, JUnit 5, Gradle 8.5

## Global Constraints

- Preserve explicitly configured colors.
- Do not change effects, durations, amplifiers, trait levels, IDs, or legendary eligibility.
- Do not add runtime logging or exception swallowing.
- Do not deploy the generated JAR to `mods`.

---

### Task 1: Normalize colors in legendary trait objects

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/content/traits/LegendarySelfEffectTrait.java`
- Modify: `src/main/java/com/l2hostility_tweaks/content/traits/LegendaryTargetEffectTrait.java`
- Create: `src/main/java/com/l2hostility_tweaks/content/traits/LegendaryTraitColor.java`
- Create: `src/test/java/com/l2hostility_tweaks/content/traits/LegendaryTraitColorTest.java`

**Interfaces:**
- Consumes: nullable `IntSupplier color` values from constructors and builders.
- Produces: `LegendaryTraitColor.normalize(IntSupplier)` and non-null stored color suppliers.

- [x] **Step 1: Add failing null-color tests**

Call `LegendaryTraitColor.normalize(null)` and assert its value equals `ChatFormatting.GOLD.getColor()`. Pass `() -> 0x123456` and assert the custom value is preserved.

- [x] **Step 2: Run the focused test and confirm RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.content.traits.LegendaryTraitColorTest`

Expected: compilation fails because `LegendaryTraitColor` does not exist.

- [x] **Step 3: Normalize constructor inputs**

Implement `LegendaryTraitColor.normalize` to return the supplied color or a shared gold supplier. Use it in both constructors. Do not change `getColor()` or any effect behavior.

- [x] **Step 4: Run the focused test and confirm GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.content.traits.LegendaryTraitColorTest`

Expected: all four assertions pass.

### Task 2: Establish Builder defaults

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/compat/kubejs/LegendarySelfEffectTraitBuilder.java`
- Modify: `src/main/java/com/l2hostility_tweaks/compat/kubejs/LegendaryTargetEffectTraitBuilder.java`

**Interfaces:**
- Consumes: the inherited nullable `color` field.
- Produces: builders that pass a gold supplier unless the script supplied a color.

- [x] **Step 1: Add the normal creation default**

At the start of each `createObject()`, assign `color = LegendaryTraitColor.normalize(color)` so the builders establish the same non-null invariant as the trait constructors.

- [x] **Step 2: Compile the KubeJS compatibility path**

Run: `.\gradlew.bat compileJava`

Expected: production sources compile successfully with the existing compile-only KubeJS dependency.

- [x] **Step 3: Re-run object-level regression tests**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.content.traits.LegendaryTraitColorTest`

Expected: all fallback and custom-color assertions remain green.

### Task 3: Verify and commit

**Files:**
- Verify all files changed by Tasks 1 and 2.

**Interfaces:**
- Consumes: builder defaults and constructor normalization.
- Produces: clean build, reviewed diff, generated JAR, and one implementation commit.

- [x] **Step 1: Run the complete build**

Run: `.\gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL` with zero test failures.

- [x] **Step 2: Review and clean generated logs**

Run `git diff --check`, inspect the diff and XML test totals, then remove only the validated project-local `logs` directory if Gradle generated it.

- [x] **Step 3: Commit the implementation**

Stage the plan, five production files, and the regression test. Commit with `fix: default legendary trait colors` and confirm `git status --short` is empty.
