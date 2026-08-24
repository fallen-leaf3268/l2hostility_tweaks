# Undying Limit Recovery and Tooltip Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reset the Undying activation counter only after a successful temporary unseal and expose the active resurrection limit in every trait detail view.

**Architecture:** Keep seal lifecycle state in `TraitDisableHelper`, where successful unsealing already occurs. Build the configuration-dependent description there as a common-side `Component`, then append it from the existing `MobTraitDescMixin` only for `l2hostility:undying`.

**Tech Stack:** Java 17, Forge 1.20.1, Sponge Mixin 0.8.5, JUnit 5, Gradle 8.5.

## Global Constraints

- `max_resurrections = -1` produces no limit description.
- `seal_duration = 0` keeps the existing no-seal behavior and produces no limit description.
- Positive seal duration resets the counter only after the trait is actually restored.
- Permanent seals never reset the counter.
- No JAR is copied to a `mods` directory; final output goes to `C:\Users\Lenovo\Desktop\Ai_Run\output`.

---

### Task 1: Reset the Undying Counter on Successful Unseal

**Files:**
- Modify: `src/test/java/com/l2hostility_tweaks/util/TraitDisableHelperTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/UndyingTraitMixin.java`

**Interfaces:**
- Produces: `TraitDisableHelper.UNDYING_TRAIT_ID`, `TraitDisableHelper.UNDYING_COUNT_KEY`, and `TraitDisableHelper.onTraitUnsealed(CompoundTag, String)`.
- Consumes: the existing successful `setDisabled(entity, traitId, false, heal)` restoration branch.

- [ ] **Step 1: Write the failing counter-reset tests**

Add tests proving `onTraitUnsealed` removes `UNDYING_COUNT_KEY` for `UNDYING_TRAIT_ID` and leaves it unchanged for another trait ID.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: test compilation fails because the constants and `onTraitUnsealed` do not exist.

- [ ] **Step 3: Implement the minimal unseal reset**

Define the shared IDs in `TraitDisableHelper`, implement `onTraitUnsealed`, and call it inside the matching-trait branch only after the stored positive level has been restored and initialization has completed. Replace duplicate constants in `UndyingTraitMixin` with the shared constants.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: all `TraitDisableHelperTest` cases pass.

- [ ] **Step 5: Commit the lifecycle fix**

```powershell
git add -- src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java src/main/java/com/l2hostility_tweaks/mixin/UndyingTraitMixin.java src/test/java/com/l2hostility_tweaks/util/TraitDisableHelperTest.java
git commit -m "fix: reset undying count after unseal"
```

### Task 2: Add the Configuration-Aware Undying Detail

**Files:**
- Modify: `src/test/java/com/l2hostility_tweaks/util/TraitDisableHelperTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/MobTraitDescMixin.java`
- Modify: `src/main/resources/assets/l2hostility_tweaks/lang/zh_cn.json`
- Modify: `src/main/resources/assets/l2hostility_tweaks/lang/en_us.json`

**Interfaces:**
- Produces: `TraitDisableHelper.buildUndyingLimitDetail(int maxResurrections, int sealDuration)` returning a nullable `Component`.
- Consumes: `L2HConfig.getUndyingMaxResurrections()` and `L2HConfig.getUndyingSealDuration()`.

- [ ] **Step 1: Write failing description tests**

Cover these inputs: `(-1, 60)` returns null, `(3, 0)` returns null, `(3, 60)` returns the timed translation with arguments `3` and `60`, and `(3, -1)` returns the permanent translation with argument `3`.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: test compilation fails because `buildUndyingLimitDetail` does not exist.

- [ ] **Step 3: Implement the common description builder**

Return null for unlimited or zero-duration configurations. Return `Component.translatable("trait.l2hostility_tweaks.undying.limit_timed", maxResurrections, sealDuration)` for positive durations and `Component.translatable("trait.l2hostility_tweaks.undying.limit_permanent", maxResurrections)` for negative durations.

- [ ] **Step 4: Append the detail only to Undying**

Inject at `MobTrait.addDetail` TAIL in `MobTraitDescMixin`, compare the current trait registry name with `UNDYING_TRAIT_ID`, build the configured component, and append a gold-styled line when non-null.

- [ ] **Step 5: Add localized text**

Add Chinese values:

```json
"trait.l2hostility_tweaks.undying.limit_timed": "触发 %s 次后封印词条，封印的词条将在 %s 秒后解封",
"trait.l2hostility_tweaks.undying.limit_permanent": "触发 %s 次后永久封印词条"
```

Add equivalent English values to `en_us.json`.

- [ ] **Step 6: Run the focused test and verify GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: all focused tests pass.

- [ ] **Step 7: Commit the Tooltip feature**

```powershell
git add -- src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java src/main/java/com/l2hostility_tweaks/mixin/MobTraitDescMixin.java src/main/resources/assets/l2hostility_tweaks/lang/zh_cn.json src/main/resources/assets/l2hostility_tweaks/lang/en_us.json src/test/java/com/l2hostility_tweaks/util/TraitDisableHelperTest.java
git commit -m "feat: describe undying resurrection limit"
```

### Task 3: Full Verification and Deliverable

**Files:**
- Verify: `build/test-results/test/TEST-*.xml`
- Deliver: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: the two implementation commits from Tasks 1 and 2.
- Produces: a verified JAR with a recorded SHA-256 hash.

- [ ] **Step 1: Run clean verification**

Run: `.\gradlew.bat clean test build`

Expected: `BUILD SUCCESSFUL`, zero test failures, and a reobfuscated JAR in `build/libs`.

- [ ] **Step 2: Inspect the JAR and repository**

Confirm the JAR contains `MobTraitDescMixin.class`, `TraitDisableHelper.class`, both language files, and the mixin configuration. Run `git diff --check` and confirm `git status --short` has no source changes.

- [ ] **Step 3: Copy and hash the deliverable**

Copy the verified JAR to `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`, never to `mods`, and calculate SHA-256.
