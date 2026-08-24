# Ownable Trait Config Restoration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the upstream `allowTraitOnOwnable` behavior by removing the unconditional pet-trait clear bypass.

**Architecture:** Keep `MobTraitCapTickMixin` for player stage handling and trait-tick immunity, but remove its redirect of the upstream `LinkedHashMap.clear()` call. A reflection regression test prevents the bypass handler from being reintroduced.

**Tech Stack:** Java 17, Sponge Mixin, JUnit 5, ForgeGradle

## Global Constraints

- Do not add a replacement pet-trait configuration.
- Do not change player trait capability initialization.
- Do not change trait tick immunity behavior.
- Preserve the upstream `allowTraitOnOwnable=false` and `true` branches.
- Do not deploy the generated JAR to a `mods` directory.
- Copy the verified JAR only to `C:/Users/Lenovo/Desktop/Ai_Run/output`.

---

### Task 1: Remove the Pet Trait Clear Bypass

**Files:**
- Modify: `src/test/java/com/l2hostility_tweaks/mixin/LegendaryAllowMixinTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/MobTraitCapTickMixin.java`

**Interfaces:**
- Consumes: the existing `MobTraitCapTickMixin` class and JUnit reflection assertions.
- Produces: a mixin without `l2fix$skipPetClear`, allowing the upstream `LinkedHashMap.clear()` call to execute normally.

- [ ] **Step 1: Write the failing regression test**

Add to `LegendaryAllowMixinTest`:

```java
@Test
void doesNotOverrideOwnableTraitConfigClear() {
    assertFalse(Arrays.stream(MobTraitCapTickMixin.class.getDeclaredMethods())
            .anyMatch(method -> method.getName().equals("l2fix$skipPetClear")));
}
```

- [ ] **Step 2: Run the targeted test and verify RED**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.mixin.LegendaryAllowMixinTest`

Expected: one assertion failure because `l2fix$skipPetClear` still exists.

- [ ] **Step 3: Remove the bypass**

Delete this method from `MobTraitCapTickMixin`:

```java
@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/LinkedHashMap;clear()V"), remap = false)
public void l2fix$skipPetClear(LinkedHashMap<?, ?> instance) {
}
```

Delete the unused import:

```java
import java.util.LinkedHashMap;
```

Do not change `l2fix$skipPlayerAutoInit` or `l2fix$skipPlayerTraitTick`.

- [ ] **Step 4: Run the targeted test and verify GREEN**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.mixin.LegendaryAllowMixinTest`

Expected: all four tests in the class pass with zero failures and errors.

- [ ] **Step 5: Review and commit**

Run: `git diff --check`

Expected: no whitespace errors.

```bash
git add src/main/java/com/l2hostility_tweaks/mixin/MobTraitCapTickMixin.java src/test/java/com/l2hostility_tweaks/mixin/LegendaryAllowMixinTest.java
git commit -m "fix: honor ownable trait config"
```

### Task 2: Full Verification and JAR Output

**Files:**
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: completed Task 1.
- Produces: a verified distributable JAR and matching SHA-256 digest.

- [ ] **Step 1: Run the complete clean build**

Run: `./gradlew.bat clean test build`

Expected: `BUILD SUCCESSFUL`, 71 tests, zero failures, and zero errors.

- [ ] **Step 2: Inspect the JAR**

Run: `jar tf build/libs/l2hostility_tweaks-1.0.0.jar`

Expected: `com/l2hostility_tweaks/mixin/MobTraitCapTickMixin.class` and `l2hostility_tweaks.mixins.json` are present.

- [ ] **Step 3: Verify compiled method ownership**

Run: `javap -private -classpath build/libs/l2hostility_tweaks-1.0.0.jar com.l2hostility_tweaks.mixin.MobTraitCapTickMixin`

Expected: `l2fix$skipPetClear` is absent while `l2fix$skipPlayerAutoInit` and `l2fix$skipPlayerTraitTick` remain.

- [ ] **Step 4: Copy and hash the output JAR**

Copy the built JAR to `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`, replacing the previous build. Verify source and output SHA-256 hashes match.

- [ ] **Step 5: Confirm repository state**

Run: `git status --short --branch`

Expected: clean `fix/tooltip-pipeline` worktree.
