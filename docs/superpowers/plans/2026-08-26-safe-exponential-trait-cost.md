# Safe Exponential Trait Cost Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate overflow and illegal stacks from exponential player-trait upgrade and unload costs while preserving all default-level values.

**Architecture:** Add one pure `TraitCostHelper` as the authoritative calculator, then route the client tooltip, server payment check, and config refund wrappers through it. Keep delivery separate in `TraitWandHelper`, which splits a refund total into legal stack-sized chunks before inserting or dropping them.

**Tech Stack:** Java 17, Forge 1.20.1, Sponge Mixin, JUnit 5, Gradle 8.5

## Global Constraints

- Mode 3 upgrade cost is `2^currentLevel` only while it fits the trait symbol's actual maximum stack size.
- With maximum stack size 64, payable exponential costs are 1, 2, 4, 8, 16, 32, and 64; higher levels are unpayable.
- With maximum stack size 64, exponential single-level refund is capped at 64 and total refund at 127.
- Modes 1 and 2 retain their existing numerical schedules.
- Every refund `ItemStack` must have a positive count no greater than its item maximum stack size.
- Do not deploy the JAR into a `mods` directory.

---

### Task 1: Authoritative safe cost calculator

**Files:**
- Create: `src/main/java/com/l2hostility_tweaks/util/TraitCostHelper.java`
- Create: `src/test/java/com/l2hostility_tweaks/util/TraitCostHelperTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/config/L2HConfig.java`
- Modify: `src/test/java/com/l2hostility_tweaks/config/L2HConfigTest.java`

**Interfaces:**
- Produces: `TraitCostHelper.UNPAYABLE`, `upgradeCost(int mode, int currentLevel, int maxStackSize)`, `singleRefund(int mode, int currentLevel, int maxStackSize)`, and `totalRefund(int mode, int currentLevel, int maxStackSize)`.
- Produces: `L2HConfig.getUpgradeCost(int currentLevel, int maxStackSize)`, `getUnloadRefund(int currentLevel, int maxStackSize)`, and `getTotalUnloadRefund(int currentLevel, int maxStackSize)`.

- [ ] **Step 1: Write failing calculator tests**

Add tests asserting mode 3 costs `1..64` for levels `0..6`, `UNPAYABLE` for levels `7`, `31`, `32`, negative levels, and non-positive stack sizes; assert single refunds `1, 2, 64, 64, 64` for levels `1, 2, 7, 31, 32`; assert total refunds `1, 3, 127, 127, 127` for those levels. Add mode 1 and mode 2 regression assertions for their existing schedules.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew test --tests com.l2hostility_tweaks.util.TraitCostHelperTest`

Expected: compilation fails because `TraitCostHelper` does not exist.

- [ ] **Step 3: Implement the minimal pure calculator**

Use `UNPAYABLE = -1`. For mode 3, find the largest power of two no greater than `maxStackSize` without overflowing; compare the level with that power's exponent before shifting. Clamp single refund to that largest power and total refund to `2 * largestPower - 1`. Use bounded `long` arithmetic for modes 1 and 2 so no method can return a negative value.

- [ ] **Step 4: Route config wrappers through the helper**

Replace the direct shifts in `L2HConfig` with the three wrapper methods accepting `maxStackSize`. Extend `L2HConfigTest` to assert that `L2HConfig.java` contains no `1 <<` expression.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run: `./gradlew test --tests com.l2hostility_tweaks.util.TraitCostHelperTest --tests com.l2hostility_tweaks.config.L2HConfigTest`

Expected: all selected tests pass.

- [ ] **Step 6: Commit**

Commit message: `fix: make trait cost calculations overflow safe`

### Task 2: Legal refund stack delivery

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/util/TraitWandHelper.java`
- Modify: `src/test/java/com/l2hostility_tweaks/util/TraitWandHelperTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/content/TraitUnloaderWand.java`

**Interfaces:**
- Consumes: safe refund totals from Task 1.
- Produces: `TraitWandHelper.splitCounts(int totalCount, int maxStackSize)` and `giveOrDrop(Player player, Item item, int totalCount)`.

- [ ] **Step 1: Write failing split tests**

Assert `splitCounts(127, 64)` equals `[64, 63]`, `splitCounts(64, 64)` equals `[64]`, and zero, negative count, or non-positive maximum returns an empty list. Assert every result is positive and no greater than the maximum.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew test --tests com.l2hostility_tweaks.util.TraitWandHelperTest`

Expected: compilation fails because `splitCounts` does not exist.

- [ ] **Step 3: Implement bounded splitting and delivery**

Implement `splitCounts` with division and remainder rather than decrementing one item at a time. Implement the item/count overload by creating one `ItemStack` per returned chunk and invoking the existing stack overload.

- [ ] **Step 4: Update all unload paths**

Pass `trait.asItem().getMaxStackSize()` into the safe config refund wrappers and replace oversized `new ItemStack(item, refund)` calls with the item/count delivery overload. Use `long` for the full-unload display accumulator so totals across multiple traits cannot wrap negative.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run: `./gradlew test --tests com.l2hostility_tweaks.util.TraitWandHelperTest --tests com.l2hostility_tweaks.util.TraitCostHelperTest`

Expected: all selected tests pass.

- [ ] **Step 6: Commit**

Commit message: `fix: split trait refunds into legal item stacks`

### Task 3: Client and server integration

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixin.java`
- Modify: `src/test/java/com/l2hostility_tweaks/mixin/TraitSymbolSelfUseMixinTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/client/PlayerTraitScreen.java`
- Modify: `src/main/java/com/l2hostility_tweaks/init/L2HTweaksLang.java`
- Modify: `src/main/resources/assets/l2hostility_tweaks/lang/en_us.json`
- Modify: `src/main/resources/assets/l2hostility_tweaks/lang/zh_cn.json`

**Interfaces:**
- Consumes: `L2HConfig.getUpgradeCost(int currentLevel, int maxStackSize)` and `TraitCostHelper.UNPAYABLE`.
- Produces: one localized unpayable-upgrade message shared by the GUI and server rejection path.

- [ ] **Step 1: Write failing integration contract tests**

Extend `TraitSymbolSelfUseMixinTest` to assert the mixin uses `L2HConfig.getUpgradeCost`, handles `TraitCostHelper.UNPAYABLE` before inventory comparison and shrink, and contains no direct `1 << currentLevel`. Add a source assertion for `PlayerTraitScreen` that it uses the same config wrapper and unpayable translation key.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.TraitSymbolSelfUseMixinTest`

Expected: assertions fail because both runtime paths still calculate mode 3 directly.

- [ ] **Step 3: Replace server calculation and add hard rejection**

Calculate using the held symbol stack's maximum size. When the result is `UNPAYABLE`, send the localized red message, return `InteractionResultHolder.fail(stack)`, and never execute count comparison, trait mutation, or `shrink`.

- [ ] **Step 4: Replace client calculation and add translations**

Calculate using `e.owner().asItem().getMaxStackSize()`. Render the new gold localized unavailable message instead of a numeric cost when the result is `UNPAYABLE`. Add matching `en_us` and `zh_cn` keys and the constant in `L2HTweaksLang`.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.TraitSymbolSelfUseMixinTest --tests com.l2hostility_tweaks.util.TraitCostHelperTest`

Expected: all selected tests pass.

- [ ] **Step 6: Commit**

Commit message: `fix: reject unpayable exponential trait upgrades`

### Task 4: Full verification and artifact output

**Files:**
- Output only: `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: all implementation tasks.
- Produces: verified release JAR and SHA-256.

- [ ] **Step 1: Run source and diff checks**

Run `git diff --check` and search runtime Java sources for direct exponent shifts in trait cost paths.

Expected: no whitespace errors and no direct unsafe shift remains.

- [ ] **Step 2: Run the complete build lifecycle**

Run: `./gradlew clean test check build`

Expected: exit code 0 with all tests and verification tasks passing.

- [ ] **Step 3: Inspect and copy the JAR**

Confirm the built JAR contains `TraitCostHelper`, the modified mixin, the unloader, and both language files. Copy it to `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`, then compare source/output SHA-256 hashes.

- [ ] **Step 4: Request code review and address findings**

Review the commits against this plan. Fix all Critical and Important findings with focused regression tests, then rerun the complete build lifecycle.

- [ ] **Step 5: Report evidence**

Report test count, build result, JAR size, SHA-256, output location, and the exact behavior for levels 6, 7, 31, and 32.
