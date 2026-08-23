# Sealed Self-Trait Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent sealed max-level player traits from bypassing self-use validation while preserving below-max unseal-and-upgrade behavior.

**Architecture:** Read the target trait's raw stored level once and derive validation decisions from its absolute value. Isolate the raw-level rules in package-private pure helpers so the regression is testable without constructing Minecraft capability objects.

**Tech Stack:** Java 17, Sponge Mixin, JUnit 5, Gradle

## Global Constraints

- Do not deploy the generated JAR to a mods directory.
- Do not change cost, balance, exclusion, or seal-removal behavior beyond the raw-level validation defect.

---

### Task 1: Validate Raw Sealed Levels

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixin.java`
- Create: `src/test/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixinTest.java`

**Interfaces:**
- Consumes: nullable raw trait levels from `MobTraitCap.traits`
- Produces: `l2fix$isAtMaxLevel(Integer, int)` and `l2fix$projectedTraitCount(Collection<Integer>, Integer)`

- [x] **Step 1: Write failing regression tests**

```java
assertTrue(TraitSymbolSelfUseMixin.l2fix$isAtMaxLevel(-3, 3));
assertFalse(TraitSymbolSelfUseMixin.l2fix$isAtMaxLevel(-2, 3));
assertEquals(3, TraitSymbolSelfUseMixin.l2fix$projectedTraitCount(List.of(1, -2, -1), -2));
```

- [x] **Step 2: Verify the tests fail for the missing raw-level rules**

Run: `gradlew.bat test --tests com.l2hostility_tweaks.mixin.TraitSymbolSelfUseMixinTest`

Expected: compilation failure because the helper methods do not exist.

- [x] **Step 3: Implement the minimal raw-level validation**

```java
static boolean l2fix$isAtMaxLevel(Integer rawLevel, int maxLevel) {
    return rawLevel != null && Math.abs(rawLevel) >= maxLevel;
}

static int l2fix$projectedTraitCount(Collection<Integer> levels, Integer targetRawLevel) {
    int count = (int) levels.stream().filter(value -> value != null && value != 0).count();
    return targetRawLevel == null || targetRawLevel == 0 ? count + 1 : count;
}
```

Use the same raw target level for maximum-count validation, maximum-level validation, and cost calculation.

- [x] **Step 4: Verify focused and complete builds**

Run: `gradlew.bat test --tests com.l2hostility_tweaks.mixin.TraitSymbolSelfUseMixinTest`

Expected: all focused tests pass.

Run: `gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [x] **Step 5: Commit the isolated fix**

```text
git add docs/superpowers/specs/2026-08-23-sealed-self-trait-validation-design.md docs/superpowers/plans/2026-08-23-sealed-self-trait-validation.md src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixin.java src/test/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixinTest.java
git commit -m "fix: validate sealed self-trait upgrades"
```
