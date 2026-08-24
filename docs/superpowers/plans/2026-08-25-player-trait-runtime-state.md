# Player Trait Runtime State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve Undying resurrection counts and seal expiry times across `keepInventory=true` player death clones without copying unrelated persistent NBT.

**Architecture:** `TraitDisableHelper` owns pure `CompoundTag` snapshot and restore operations for the exact runtime-state keys. `L2HostilityFix` stores one `CompoundTag` per dead player UUID and consumes it during clone restoration, while sealed levels continue to be rebuilt from restored negative trait levels.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, ForgeGradle, JUnit 5, Minecraft NBT

## Global Constraints

- Snapshot every long key beginning with `l2htweaks_seal_expiry_`.
- Snapshot the integer key `l2fix$undying_count` only when it exists with the correct type.
- Do not snapshot unrelated NBT or `l2htweaks_sealed_level_*` keys.
- Restore runtime state only for the existing `keepInventory=true` trait-preservation path.
- Preserve the existing `keepInventory=false` behavior that clears player traits.
- Do not deploy the generated JAR to a `mods` directory.
- Copy the verified JAR only to `C:/Users/Lenovo/Desktop/Ai_Run/output`.

---

### Task 1: Add Runtime-State Snapshot and Restore Helpers

**Files:**
- Modify: `src/test/java/com/l2hostility_tweaks/util/TraitDisableHelperTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java`

**Interfaces:**
- Consumes: `CompoundTag`, `SEAL_EXPIRY_PREFIX`, and `UNDYING_COUNT_KEY`.
- Produces: `static CompoundTag snapshotRuntimeState(CompoundTag data)` and `static void restoreRuntimeState(CompoundTag target, CompoundTag snapshot)`.

- [ ] **Step 1: Write failing snapshot and restore tests**

Add these tests to `TraitDisableHelperTest`:

```java
@Test
void snapshotsAndRestoresOnlyManagedRuntimeState() {
    String firstExpiry = TraitDisableHelper.sealExpiryKey("l2hostility:undying");
    String secondExpiry = TraitDisableHelper.sealExpiryKey("l2hostility:split");
    CompoundTag source = new CompoundTag();
    source.putLong(firstExpiry, 1200L);
    source.putLong(secondExpiry, -1L);
    source.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, 2);
    source.putInt("l2htweaks_sealed_level_l2hostility:undying", 1);
    source.putString("unrelated", "value");

    CompoundTag snapshot = TraitDisableHelper.snapshotRuntimeState(source);
    source.putLong(firstExpiry, 9999L);
    CompoundTag target = new CompoundTag();
    TraitDisableHelper.restoreRuntimeState(target, snapshot);

    assertEquals(1200L, target.getLong(firstExpiry));
    assertEquals(-1L, target.getLong(secondExpiry));
    assertEquals(2, target.getInt(TraitDisableHelper.UNDYING_COUNT_KEY));
    assertFalse(target.contains("l2htweaks_sealed_level_l2hostility:undying"));
    assertFalse(target.contains("unrelated"));
    target.putLong(firstExpiry, 7777L);
    assertEquals(1200L, snapshot.getLong(firstExpiry));
}

@Test
void keepsUndyingCountAbsentWhenSourceDoesNotContainIt() {
    CompoundTag source = new CompoundTag();
    source.putLong(TraitDisableHelper.sealExpiryKey("l2hostility:split"), 400L);

    CompoundTag snapshot = TraitDisableHelper.snapshotRuntimeState(source);
    CompoundTag target = new CompoundTag();
    TraitDisableHelper.restoreRuntimeState(target, snapshot);

    assertFalse(target.contains(TraitDisableHelper.UNDYING_COUNT_KEY));
}
```

- [ ] **Step 2: Run the targeted tests and verify RED**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: compilation fails because `snapshotRuntimeState` and `restoreRuntimeState` do not exist.

- [ ] **Step 3: Implement the minimal helper methods**

Add the `Tag` import and these methods to `TraitDisableHelper`:

```java
public static CompoundTag snapshotRuntimeState(CompoundTag data) {
    CompoundTag snapshot = new CompoundTag();
    for (String key : data.getAllKeys()) {
        if (key.startsWith(SEAL_EXPIRY_PREFIX) && data.contains(key, Tag.TAG_LONG)) {
            snapshot.putLong(key, data.getLong(key));
        }
    }
    if (data.contains(UNDYING_COUNT_KEY, Tag.TAG_INT)) {
        snapshot.putInt(UNDYING_COUNT_KEY, data.getInt(UNDYING_COUNT_KEY));
    }
    return snapshot;
}

public static void restoreRuntimeState(CompoundTag target, CompoundTag snapshot) {
    for (String key : snapshot.getAllKeys()) {
        if (key.startsWith(SEAL_EXPIRY_PREFIX) && snapshot.contains(key, Tag.TAG_LONG)) {
            target.putLong(key, snapshot.getLong(key));
        }
    }
    if (snapshot.contains(UNDYING_COUNT_KEY, Tag.TAG_INT)) {
        target.putInt(UNDYING_COUNT_KEY, snapshot.getInt(UNDYING_COUNT_KEY));
    }
}
```

- [ ] **Step 4: Run the targeted tests and verify GREEN**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: all `TraitDisableHelperTest` tests pass with zero failures and errors.

- [ ] **Step 5: Commit the helper and tests**

```bash
git add src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java src/test/java/com/l2hostility_tweaks/util/TraitDisableHelperTest.java
git commit -m "feat: snapshot player trait runtime state"
```

### Task 2: Integrate the Unified Snapshot with Player Death Cloning

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/L2HostilityFix.java`

**Interfaces:**
- Consumes: `TraitDisableHelper.snapshotRuntimeState(CompoundTag)` and `TraitDisableHelper.restoreRuntimeState(CompoundTag, CompoundTag)` from Task 1.
- Produces: a UUID-keyed `deathTraitRuntimeState` lifecycle that replaces `deathSealExpiry`.

- [ ] **Step 1: Replace the seal-only map**

Replace:

```java
private static final java.util.Map<java.util.UUID, java.util.Map<String, Long>> deathSealExpiry = new java.util.HashMap<>();
```

with:

```java
private static final java.util.Map<java.util.UUID, CompoundTag> deathTraitRuntimeState = new java.util.HashMap<>();
```

Add `net.minecraft.nbt.CompoundTag` to the imports.

- [ ] **Step 2: Capture a fresh runtime-state snapshot on every death event**

Replace the seal-only collection block with:

```java
java.util.UUID uuid = player.getUUID();
CompoundTag runtimeState = TraitDisableHelper.snapshotRuntimeState(player.getPersistentData());
if (runtimeState.isEmpty()) {
    deathTraitRuntimeState.remove(uuid);
} else {
    deathTraitRuntimeState.put(uuid, runtimeState);
}
LOGGER.info("DEATH: snapshotted {} traits + {} runtime state entries for player={} uuid={}",
        snapshot.size(), runtimeState.size(), player.getName().getString(), uuid);
```

Use `uuid` for the existing `deathSnapshots.put` and `deathMeta.put` calls. Removing an old entry when the new snapshot is empty prevents a canceled or earlier death event from leaking stale state into a later clone.

- [ ] **Step 3: Update lifecycle cleanup**

Replace every `deathSealExpiry.clear()` and `deathSealExpiry.remove(uuid)` with the corresponding `deathTraitRuntimeState` call in server-stop, logout, and clone handlers.

- [ ] **Step 4: Restore runtime state on the keep-inventory path**

In `onPlayerClone`, retrieve:

```java
CompoundTag runtimeState = deathTraitRuntimeState.remove(uuid);
```

Update the clone diagnostic placeholders from `seals` to `runtimeState`. After restoring `deathMeta` and before rebuilding sealed levels, add:

```java
if (runtimeState != null) {
    TraitDisableHelper.restoreRuntimeState(newPlayer.getPersistentData(), runtimeState);
    LOGGER.info("CLONE: restored {} runtime state entries", runtimeState.size());
}
```

Keep this after the existing early return for `keepInventory=false`, so runtime state is not restored when traits are cleared.

- [ ] **Step 5: Compile and run the targeted helper tests**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.util.TraitDisableHelperTest`

Expected: project compiles and every targeted test passes.

- [ ] **Step 6: Review the integration diff and commit**

Run: `git diff --check`

Expected: no whitespace errors.

```bash
git add src/main/java/com/l2hostility_tweaks/L2HostilityFix.java
git commit -m "fix: preserve undying count across player death"
```

### Task 3: Full Verification and JAR Output

**Files:**
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: completed Tasks 1 and 2.
- Produces: a verified distributable JAR and recorded SHA-256 digest.

- [ ] **Step 1: Run the complete clean build**

Run: `./gradlew.bat clean test build`

Expected: `BUILD SUCCESSFUL`, with all test suites reporting zero failures and zero errors.

- [ ] **Step 2: Inspect the JAR contents**

Run: `jar tf build/libs/l2hostility_tweaks-1.0.0.jar`

Expected: output contains `com/l2hostility_tweaks/L2HostilityFix.class`, `com/l2hostility_tweaks/util/TraitDisableHelper.class`, and `l2hostility_tweaks.mixins.json`.

- [ ] **Step 3: Copy and hash the output JAR**

Copy `build/libs/l2hostility_tweaks-1.0.0.jar` to `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`, replacing the previous verification build. Compute SHA-256 for both files and verify the hashes match.

- [ ] **Step 4: Confirm repository state**

Run: `git status --short --branch`

Expected: clean `fix/tooltip-pipeline` worktree with the design, plan, helper, integration, and test commits present.
