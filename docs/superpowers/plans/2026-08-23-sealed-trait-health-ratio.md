# Sealed Trait Health Ratio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve a target's pre-refresh health percentage when upgrading an already sealed trait.

**Architecture:** Capture health state immediately before mutating the sealed trait and refreshing its attributes. Put the ratio calculation in a package-private pure helper so edge cases are regression-tested without constructing Minecraft entities.

**Tech Stack:** Java 17, Sponge Mixin, JUnit 5, Gradle

## Global Constraints

- Do not deploy the generated JAR to a mods directory.
- Do not change trait levels, seal state, symbol consumption, or maximum-level validation.

---

### Task 1: Preserve Pre-Refresh Health Percentage

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolMixin.java`
- Create: `src/test/java/com/l2hostility_tweaks/mixin/TraitSymbolMixinTest.java`

**Interfaces:**
- Consumes: old health, old maximum health, and new maximum health as floats
- Produces: `l2fix$scaledHealth(float, float, float)` returning the restored health value

- [x] **Step 1: Write the failing regression test**

```java
assertEquals(30.0f, TraitSymbolMixin.l2fix$scaledHealth(60.0f, 100.0f, 50.0f), 0.0001f);
assertEquals(50.0f, TraitSymbolMixin.l2fix$scaledHealth(120.0f, 100.0f, 50.0f));
assertEquals(1.0f, TraitSymbolMixin.l2fix$scaledHealth(0.0f, 100.0f, 50.0f));
```

- [x] **Step 2: Verify RED**

Run: `gradlew.bat test --tests com.l2hostility_tweaks.mixin.TraitSymbolMixinTest`

Expected: compilation failure because `l2fix$scaledHealth` does not exist.

- [x] **Step 3: Implement the minimal fix**

```java
static float l2fix$scaledHealth(float oldHealth, float oldMaxHealth, float newMaxHealth) {
    float ratio = oldMaxHealth > 0 ? oldHealth / oldMaxHealth : 1.0f;
    return Math.max(1.0f, newMaxHealth * Math.min(1.0f, ratio));
}
```

Capture `oldHealth` and `oldMaxHealth` before updating the sealed level. After `initialize` and capability sync, call `target.setHealth(l2fix$scaledHealth(oldHealth, oldMaxHealth, target.getMaxHealth()))`.

- [x] **Step 4: Verify GREEN and the complete build**

Run: `gradlew.bat test --tests com.l2hostility_tweaks.mixin.TraitSymbolMixinTest`

Expected: focused tests pass.

Run: `gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [x] **Step 5: Commit the isolated fix**

```text
git add docs/superpowers/specs/2026-08-23-sealed-trait-health-ratio-design.md docs/superpowers/plans/2026-08-23-sealed-trait-health-ratio.md src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolMixin.java src/test/java/com/l2hostility_tweaks/mixin/TraitSymbolMixinTest.java
git commit -m "fix: preserve health ratio for sealed trait upgrades"
```
