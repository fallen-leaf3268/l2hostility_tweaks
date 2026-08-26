# Recursive NBT Condition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让实体 JSON 配置完整、精确地递归匹配 NBT，并保证空或无效条件绝不退化为无条件配置。

**Architecture:** 使用 `EntityConfigNbtData` 接口在目标配置对象上保存 `NONE / VALID / INVALID` 三态和原始 `JsonObject`，替换反射字段。`NbtCondition` 负责独立的验证与 JSON-to-NBT 递归匹配；合并 Mixin 负责写入状态，实体配置 Mixin 只依据三态完成普通、条件、禁用分类。

**Tech Stack:** Java 17、Forge 1.20.1、Sponge Mixin、Gson、Minecraft NBT、JUnit 5、Gradle

## Global Constraints

- 复合标签按配置字段递归部分匹配，允许实体包含额外字段。
- 列表按长度、顺序和内容完全匹配。
- 配置格式继续使用 JSON 对象，不引入 SNBT 或新依赖。
- 无效或顶层空条件必须禁用对应条目，不能成为普通配置。
- JAR 只输出到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`。
- 不修改无关逻辑、平衡数值或网络协议。

---

### Task 1: 显式 NBT 条件元数据

**Files:**
- Create: `src/main/java/com/l2hostility_tweaks/util/EntityConfigNbtData.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/EntityConfigConfigMixin.java`
- Test: `src/test/java/com/l2hostility_tweaks/mixin/EntityConfigConfigMixinTest.java`

**Interfaces:**
- Produces: `EntityConfigNbtData.State { NONE, VALID, INVALID }`
- Produces: `void l2fix$setNbtCondition(State state, JsonObject condition)`
- Produces: `State l2fix$getNbtConditionState()`
- Produces: `JsonObject l2fix$getNbtCondition()`

- [ ] **Step 1: Write the failing metadata test**

```java
@Test
void defaultsToNoneAndPreservesExplicitStates() {
    EntityConfigNbtData data = new EntityConfigConfigMixin();
    assertEquals(EntityConfigNbtData.State.NONE, data.l2fix$getNbtConditionState());
    assertNull(data.l2fix$getNbtCondition());

    JsonObject condition = JsonParser.parseString("{\"elite\":true}").getAsJsonObject();
    data.l2fix$setNbtCondition(EntityConfigNbtData.State.VALID, condition);
    assertEquals(EntityConfigNbtData.State.VALID, data.l2fix$getNbtConditionState());
    assertSame(condition, data.l2fix$getNbtCondition());

    data.l2fix$setNbtCondition(EntityConfigNbtData.State.INVALID, null);
    assertEquals(EntityConfigNbtData.State.INVALID, data.l2fix$getNbtConditionState());
    assertNull(data.l2fix$getNbtCondition());
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.EntityConfigConfigMixinTest`

Expected: compilation fails because `EntityConfigNbtData` and its methods do not exist.

- [ ] **Step 3: Implement the interface and Mixin fields**

```java
public interface EntityConfigNbtData {
    enum State { NONE, VALID, INVALID }

    void l2fix$setNbtCondition(State state, JsonObject condition);
    State l2fix$getNbtConditionState();
    JsonObject l2fix$getNbtCondition();
}
```

Make `EntityConfigConfigMixin implements EntityConfigNbtData`, initialize its state to `NONE`, store the `JsonObject`, and implement the three accessors. Reject `state == null`; require a non-null condition only for `VALID`, and normalize the condition to null for `NONE` and `INVALID`.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.EntityConfigConfigMixinTest`

Expected: all tests in the class pass.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/l2hostility_tweaks/util/EntityConfigNbtData.java src/main/java/com/l2hostility_tweaks/mixin/EntityConfigConfigMixin.java src/test/java/com/l2hostility_tweaks/mixin/EntityConfigConfigMixinTest.java
git commit -m "refactor: expose entity NBT condition metadata"
```

### Task 2: 递归 JSON-to-NBT 匹配器

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/condition/NbtCondition.java`
- Create: `src/test/java/com/l2hostility_tweaks/condition/NbtConditionTest.java`

**Interfaces:**
- Consumes: Gson `JsonObject` and Minecraft `Tag`
- Produces: `NbtCondition(JsonObject expectedNbt)`
- Produces: package-visible `static boolean matches(JsonElement expected, Tag actual)`
- Produces: package-visible `static Optional<String> validate(JsonObject expected)`

- [ ] **Step 1: Write failing recursive matcher tests**

Create tests that construct NBT tags directly and assert:

```java
assertTrue(NbtCondition.matches(
        JsonParser.parseString("{\"ForgeData\":{\"elite\":true}}"),
        compoundWithForgeData(true, "extra")));
assertFalse(NbtCondition.matches(
        JsonParser.parseString("{\"ForgeData\":{\"elite\":true}}"),
        compoundWithForgeData(false, "extra")));
assertTrue(NbtCondition.matches(JsonParser.parseString("[1,2,3]"), listOf(1, 2, 3)));
assertFalse(NbtCondition.matches(JsonParser.parseString("[1,3,2]"), listOf(1, 2, 3)));
assertTrue(NbtCondition.matches(JsonParser.parseString("0.1"), FloatTag.valueOf(0.1F)));
assertTrue(NbtCondition.matches(
        JsonParser.parseString("9223372036854775807"), LongTag.valueOf(Long.MAX_VALUE)));
assertFalse(NbtCondition.matches(
        JsonParser.parseString("9223372036854775806"), LongTag.valueOf(Long.MAX_VALUE)));
assertTrue(NbtCondition.validate(JsonParser.parseString("{\"x\":null}").getAsJsonObject()).isPresent());
assertTrue(NbtCondition.validate(new JsonObject()).isPresent());
```

Also test string equality, boolean requiring `ByteTag`, missing object keys, list length mismatch, nested empty object existence, and non-finite `FloatTag`/`DoubleTag` rejection.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew test --tests com.l2hostility_tweaks.condition.NbtConditionTest`

Expected: compilation fails because the JSON constructor, validator, and recursive matcher do not exist.

- [ ] **Step 3: Implement validation and recursive matching**

Validation recursively accepts objects, arrays, strings, booleans, and numbers parseable by `new BigDecimal(primitive.getAsString())`; it rejects null and invalid numeric text. It rejects only the top-level empty object, while nested empty objects remain valid.

Matching rules:

```java
if (expected.isJsonObject()) { /* require CompoundTag; recursively check declared keys */ }
if (expected.isJsonArray()) { /* require ListTag; exact size/order/content */ }
if (primitive.isBoolean()) { /* require ByteTag and compare 0/1 */ }
if (primitive.isString()) { /* require StringTag and exact content */ }
if (primitive.isNumber()) { /* compare normalized BigDecimal values */ }
return false;
```

Normalize integral NBT tags with `BigDecimal.valueOf(numeric.getAsLong())`, `FloatTag` with `new BigDecimal(Float.toString(value))`, and `DoubleTag` with `BigDecimal.valueOf(value)`. Reject non-finite float/double before conversion. Compare using `compareTo(...) == 0`.

Update `test(LivingEntity)` to match the expected root object against persistent data first and serialized entity data as the existing fallback for each declared top-level key. Preserve lazy `saveWithoutId` creation.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `./gradlew test --tests com.l2hostility_tweaks.condition.NbtConditionTest`

Expected: all recursive matcher tests pass.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/l2hostility_tweaks/condition/NbtCondition.java src/test/java/com/l2hostility_tweaks/condition/NbtConditionTest.java
git commit -m "feat: recursively match entity NBT conditions"
```

### Task 3: 三态合并与防降级分类

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/ConfigMergerMixin.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/EntityConfigMixin.java`
- Create: `src/test/java/com/l2hostility_tweaks/mixin/EntityNbtConditionPipelineTest.java`

**Interfaces:**
- Consumes: `EntityConfigNbtData` from Task 1
- Consumes: `NbtCondition.validate(JsonObject)` from Task 2
- Produces: every merged config entry has an explicit `NONE`, `VALID`, or `INVALID` classification

- [ ] **Step 1: Write failing pipeline regression tests**

Add source-structure assertions proving both Mixins cast through `EntityConfigNbtData`, the old reflection fields and `Map<String,Object>` parser are gone, and `EntityConfigMixin` has separate `VALID` and `INVALID` branches. Add a behavior-level state classification helper test:

```java
assertEquals(Decision.ORDINARY, EntityConfigMixin.l2fix$decision(State.NONE));
assertEquals(Decision.CONDITIONAL, EntityConfigMixin.l2fix$decision(State.VALID));
assertEquals(Decision.DISABLED, EntityConfigMixin.l2fix$decision(State.INVALID));
```

The test must also assert `DISABLED != ORDINARY`, guarding the original regression directly.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.EntityNbtConditionPipelineTest`

Expected: test fails because reflection remains and the state decision helper is absent.

- [ ] **Step 3: Replace reflection and implement state-aware classification**

In `ConfigMergerMixin`, replace the nested Map payload with a record containing state and optional `JsonObject`. For every raw entry:

- absent `nbt`: leave state `NONE`;
- non-object, empty, or recursively invalid `nbt`: store `INVALID` and log resource ID, index, and validation reason once;
- valid non-empty object: store `VALID` with a deep copy of the object.

Apply the record to each `EntityConfig.Config` through `EntityConfigNbtData`; delete `NBT_FIELD`, `NBT_FIELD_LOOKED_UP`, `l2fix$parseNbt`, and `l2fix$setNbt`.

In `EntityConfigMixin`, delete the reflected getter. Classify each list entry once through:

```java
static Decision l2fix$decision(EntityConfigNbtData.State state) {
    return switch (state) {
        case NONE -> Decision.ORDINARY;
        case VALID -> Decision.CONDITIONAL;
        case INVALID -> Decision.DISABLED;
    };
}
```

Only `ORDINARY` contributes to `defaultConfigs`; `CONDITIONAL` is removed from `cache` and added to `conditions`; `DISABLED` is removed from `cache` and never added anywhere. Preserve list order for valid condition registration.

- [ ] **Step 4: Run focused and related tests**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.EntityNbtConditionPipelineTest --tests com.l2hostility_tweaks.condition.NbtConditionTest`

Expected: all selected tests pass.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/l2hostility_tweaks/mixin/ConfigMergerMixin.java src/main/java/com/l2hostility_tweaks/mixin/EntityConfigMixin.java src/test/java/com/l2hostility_tweaks/mixin/EntityNbtConditionPipelineTest.java
git commit -m "fix: prevent invalid NBT configs from becoming unconditional"
```

### Task 4: 完整验证与 JAR 输出

**Files:**
- Verify: `src/main/resources/l2hostility_tweaks.mixins.json`
- Output: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: all implementation and tests from Tasks 1–3
- Produces: verified release JAR without deployment

- [ ] **Step 1: Run complete verification**

Run: `./gradlew clean test check build`

Expected: `BUILD SUCCESSFUL`, all tests pass, resource validation passes, and `build/libs/l2hostility_tweaks-1.0.0.jar` exists.

- [ ] **Step 2: Inspect the built archive**

Run:

```powershell
& 'D:\JAVA\JAVA_17\bin\jar.exe' tf build\libs\l2hostility_tweaks-1.0.0.jar | Select-String 'EntityConfigNbtData|NbtCondition|ConfigMergerMixin|EntityConfigMixin'
```

Expected: all four implementation classes are present and `l2hostility_tweaks.mixins.json` remains packaged.

- [ ] **Step 3: Copy and hash the deliverable**

Run:

```powershell
New-Item -ItemType Directory -Force 'C:\Users\Lenovo\Desktop\Ai_Run\output'
Copy-Item -Force build\libs\l2hostility_tweaks-1.0.0.jar 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar'
Get-FileHash 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar' -Algorithm SHA256
```

Expected: output JAR exists with a reported SHA-256 hash; no file is copied to `mods`.

- [ ] **Step 4: Request code review and resolve Important findings**

Review scope: recursive match semantics, numeric precision, invalid-state non-degradation, Mixin registration, and regression risk. Re-run `./gradlew test check build` after any review changes.

- [ ] **Step 5: Record final repository state**

Run: `git status --short; git log -5 --oneline`

Expected: working tree is clean and the NBT implementation commits are visible.
