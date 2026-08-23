# Sealed Trait Exclusion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent sealed player traits from bypassing self-use mutual-exclusion validation.

**Architecture:** Base exclusion presence on the raw stored level instead of active-effect visibility. Expose the non-zero rule as a package-private pure helper and use it in the existing exclusion loop.

**Tech Stack:** Java 17, Sponge Mixin, JUnit 5, Gradle

## Global Constraints

- Do not deploy the generated JAR to a mods directory.
- Do not delete existing traits or modify automatic unsealing.
- Do not change symbol consumption, generation exclusions, or configuration semantics.

---

### Task 1: Include Sealed Traits in Player Exclusions

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixin.java`
- Modify: `src/test/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixinTest.java`

**Interfaces:**
- Consumes: nullable raw stored trait level
- Produces: `l2fix$isPresentForExclusion(Integer)` returning whether the player owns the trait

- [x] **Step 1: Write the failing regression test**

```java
assertTrue(TraitSymbolSelfUseMixin.l2fix$isPresentForExclusion(1));
assertTrue(TraitSymbolSelfUseMixin.l2fix$isPresentForExclusion(-1));
assertFalse(TraitSymbolSelfUseMixin.l2fix$isPresentForExclusion(0));
assertFalse(TraitSymbolSelfUseMixin.l2fix$isPresentForExclusion(null));
```

- [x] **Step 2: Verify RED**

Run: `gradlew.bat test --tests com.l2hostility_tweaks.mixin.TraitSymbolSelfUseMixinTest`

Expected: compilation failure because `l2fix$isPresentForExclusion` does not exist.

- [x] **Step 3: Implement the minimal fix**

```java
static boolean l2fix$isPresentForExclusion(Integer rawLevel) {
    return rawLevel != null && rawLevel != 0;
}
```

Replace the exclusion loop's `entry.getValue() > 0` condition with `l2fix$isPresentForExclusion(entry.getValue())` and retain all other conditions.

- [x] **Step 4: Verify GREEN and the complete build**

Run: `gradlew.bat test --tests com.l2hostility_tweaks.mixin.TraitSymbolSelfUseMixinTest`

Expected: focused tests pass.

Run: `gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [x] **Step 5: Commit the isolated fix**

```text
git add docs/superpowers/specs/2026-08-23-sealed-trait-exclusion-design.md docs/superpowers/plans/2026-08-23-sealed-trait-exclusion.md src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixin.java src/test/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixinTest.java
git commit -m "fix: enforce exclusions for sealed player traits"
```
