# Unified Tooltip Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace scattered and fragile tooltip mutations with one client-side post-processing pipeline that reliably adds, replaces, orders, and deduplicates all L2Hostility Tweaks tooltip content.

**Architecture:** A client-only return injection calls `TooltipPipeline` after vanilla and Forge tooltip providers complete. Pure `TooltipComponents` operations recursively identify translation keys and upsert lines, while trait-specific details remain injected into the trait class that actually owns `addDetail`.

**Tech Stack:** Java 17, Minecraft Forge 47.4.18, Sponge Mixin 0.8.5, Gradle 8.5, JUnit Jupiter 5.10.2.

## Global Constraints

- Target Minecraft `1.20.1` and Forge `47.4.18`.
- Do not change gameplay, balance values, configuration schemas, NBT schemas, or network protocols.
- Keep tooltip integration client-only and safe for dedicated-server class loading.
- Respect `ItemStack.TooltipPart.ENCHANTMENTS` and `ItemStack.TooltipPart.ADDITIONAL` hide flags.
- Match localized content by translation key, never rendered text.
- Do not deploy the produced JAR to a `mods` directory.
- Use `l2fix$` prefixes for every new Mixin handler.

---

### Task 1: Translation-aware component operations

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/com/l2hostility_tweaks/client/tooltip/TooltipComponents.java`
- Create: `src/test/java/com/l2hostility_tweaks/client/tooltip/TooltipComponentsTest.java`

**Interfaces:**
- Produces: `TooltipComponents.containsTranslation(Component, Collection<String>)`
- Produces: `TooltipComponents.containsTranslation(List<Component>, String)`
- Produces: `TooltipComponents.replaceTranslations(Component, Collection<String>, Component)`
- Produces: `TooltipComponents.upsert(List<Component>, Collection<String>, String, Collection<String>, Component)`
- Produces: `TooltipComponents.upsertOrAppend(List<Component>, Collection<String>, Component)`
- Produces: `TooltipComponents.isVisible(ItemStack, ItemStack.TooltipPart)`

- [ ] **Step 1: Configure JUnit Jupiter and write failing component tests**

Add JUnit 5.10.2 dependencies and `useJUnitPlatform()` to `build.gradle`. Add tests constructing direct and wrapped `Component.translatable(...)` values. Assert recursive matching, nested replacement with preserved prefix, duplicate-line removal, insertion after an enchantment name, fallback after the item name, stable upsert on a second pass, and vanilla hide-flag handling.

```groovy
testImplementation platform('org.junit:junit-bom:5.10.2')
testImplementation 'org.junit.jupiter:junit-jupiter'

tasks.named('test').configure {
    useJUnitPlatform()
}
```

```java
@Test
void replacesNestedTranslationAndPreservesWrapper() {
    Component line = Component.literal("• ")
            .append(Component.translatable("enchantment.l2hostility_tweaks.reprint_counter.desc"));
    Component result = TooltipComponents.replaceTranslations(line,
            Set.of("enchantment.l2hostility_tweaks.reprint_counter.desc"),
            Component.translatable("enchantment.l2hostility_tweaks.reprint_counter.desc_any"));
    assertTrue(TooltipComponents.containsTranslation(result,
            Set.of("enchantment.l2hostility_tweaks.reprint_counter.desc_any")));
    assertEquals("• ", ((LiteralContents) result.getContents()).text());
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.client.tooltip.TooltipComponentsTest`

Expected: compilation fails because `TooltipComponents` does not exist.

- [ ] **Step 3: Implement the minimal recursive utility**

Implement a final utility class with a private constructor. Recursively inspect `TranslatableContents`, rebuild only ancestor component nodes whose siblings need replacement, remove duplicate matching lines, and use index `1` as the insertion fallback when no enchantment name is recognized. Read `HideFlags` without creating NBT:

```java
public static boolean isVisible(ItemStack stack, ItemStack.TooltipPart part) {
    int flags = stack.hasTag() && stack.getTag().contains("HideFlags", Tag.TAG_ANY_NUMERIC)
            ? stack.getTag().getInt("HideFlags")
            : stack.getItem().getDefaultTooltipHideFlags(stack);
    return (flags & part.getMask()) == 0;
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.client.tooltip.TooltipComponentsTest`

Expected: all `TooltipComponentsTest` tests pass.

- [ ] **Step 5: Commit**

```powershell
git add -- build.gradle src/main/java/com/l2hostility_tweaks/client/tooltip/TooltipComponents.java src/test/java/com/l2hostility_tweaks/client/tooltip/TooltipComponentsTest.java
git commit -m "test: cover tooltip component upserts"
```

### Task 2: Central enchantment-description pipeline

**Files:**
- Create: `src/main/java/com/l2hostility_tweaks/client/tooltip/TooltipPipeline.java`
- Create: `src/test/java/com/l2hostility_tweaks/client/tooltip/TooltipPipelineDescriptionsTest.java`

**Interfaces:**
- Consumes: all `TooltipComponents` methods from Task 1.
- Produces: `TooltipPipeline.apply(ItemStack, Player, TooltipFlag, List<Component>)`
- Produces package-private: `TooltipPipeline.applyDescriptions(ItemStack, Map<Enchantment, Integer>, List<Component>)` for focused tests.

- [ ] **Step 1: Write failing description tests**

Cover these scenarios with a synthetic enchantment map and prebuilt tooltip lines: missing static description insertion, wrapped Reprint Counter replacement, armor/book/general Reprint variants, two supported descriptions on one stack, duplicate external descriptions, and hidden enchantments producing no inserted description.

```java
@Test
void processesTwoDescriptionsWithoutShortCircuiting() {
    List<Component> tooltip = new ArrayList<>(List.of(
            Component.literal("Pocket"),
            Component.translatable(reprint.getDescriptionId()),
            Component.translatable(gluttony.getDescriptionId())));
    TooltipPipeline.applyDescriptions(stack, Map.of(reprint, 1, gluttony, 2), tooltip);
    assertTrue(TooltipComponents.containsTranslation(tooltip,
            "enchantment.l2hostility_tweaks.reprint_counter.desc_any"));
    assertTrue(TooltipComponents.containsTranslation(tooltip,
            "enchantment.l2hostility_tweaks.gluttony_pocket.desc"));
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.client.tooltip.TooltipPipelineDescriptionsTest`

Expected: compilation fails because `TooltipPipeline` does not exist.

- [ ] **Step 3: Implement supported description upserts**

Resolve current enchantments once with `EnchantmentHelper.getEnchantments(stack)`. Build the full collection of enchantment name keys for insertion fallback. Process Reprint Counter, Abyss Pocket, Gluttony Pocket, and registry ID `l2hostility:split_suppressor` independently. Reprint Counter recognizes all three description keys and formats the configured reduction with `Locale.ROOT`.

```java
private static void upsertDescription(List<Component> tooltip,
                                      Map<Enchantment, Integer> enchantments,
                                      Enchantment enchantment,
                                      Collection<String> descriptionKeys,
                                      Component description) {
    if (!enchantments.containsKey(enchantment)) return;
    List<String> names = enchantments.keySet().stream()
            .map(Enchantment::getDescriptionId).toList();
    TooltipComponents.upsert(tooltip, descriptionKeys,
            enchantment.getDescriptionId(), names, description);
}
```

- [ ] **Step 4: Run description and utility tests and verify GREEN**

Run: `.\gradlew.bat test --tests 'com.l2hostility_tweaks.client.tooltip.*'`

Expected: all tooltip tests pass.

- [ ] **Step 5: Commit**

```powershell
git add -- src/main/java/com/l2hostility_tweaks/client/tooltip/TooltipPipeline.java src/test/java/com/l2hostility_tweaks/client/tooltip/TooltipPipelineDescriptionsTest.java
git commit -m "feat: add enchantment tooltip pipeline"
```

### Task 3: Move item details to the single return hook

**Files:**
- Create: `src/main/java/com/l2hostility_tweaks/mixin/TooltipPipelineMixin.java`
- Modify: `src/main/java/com/l2hostility_tweaks/client/tooltip/TooltipPipeline.java`
- Modify: `src/main/java/com/l2hostility_tweaks/client/ClientEventHandler.java`
- Modify: `src/main/resources/l2hostility_tweaks.mixins.json`
- Delete: `src/main/java/com/l2hostility_tweaks/mixin/AbrahadabraTooltipMixin.java`
- Delete: `src/main/java/com/l2hostility_tweaks/mixin/DetectorGlassesTooltipMixin.java`
- Test: `src/test/java/com/l2hostility_tweaks/client/tooltip/TooltipComponentsTest.java`

**Interfaces:**
- Consumes: `TooltipPipeline.apply(...)` and translation-aware upsert operations.
- Produces: one client Mixin handler `l2fix$applyTooltipPipeline(...)` at `ItemStack#getTooltipLines` return.

- [ ] **Step 1: Add failing static-item upsert and repeated-pocket-entry tests**

Add tests proving canonical item lines replace instead of duplicate and proving a list containing two identical item-name components retains both entries when only a canonical static line is upserted. The latter guards against applying translation-style deduplication to NBT slot contents.

- [ ] **Step 2: Run the focused tests and verify RED**

Run: `.\gradlew.bat test --tests 'com.l2hostility_tweaks.client.tooltip.*'`

Expected: the new static-item upsert expectation fails before pipeline item handlers exist.

- [ ] **Step 3: Implement item handlers and the return Mixin**

Move Abrahadabra, Detector Glasses, Hostility Essence, and Pocket of Restoration logic into `TooltipPipeline`. Static lines use `upsertOrAppend`; dynamic pocket slot names are inserted in NBT slot order and are never deduplicated by display text. Guard all four item-detail handlers with `TooltipPart.ADDITIONAL` visibility.

```java
@Mixin(ItemStack.class)
public class TooltipPipelineMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void l2fix$applyTooltipPipeline(@Nullable Player player, TooltipFlag flag,
                                            CallbackInfoReturnable<List<Component>> cir) {
        TooltipPipeline.apply((ItemStack) (Object) this, player, flag, cir.getReturnValue());
    }
}
```

Remove `onItemTooltip` from `ClientEventHandler`, remove the two retired Mixin registrations, add `TooltipPipelineMixin` to the client list, and delete the retired source files.

- [ ] **Step 4: Run tooltip tests and resource validation**

Run: `.\gradlew.bat test validateProjectResources`

Expected: tests pass and Mixin registration validation reports no missing or unregistered classes.

- [ ] **Step 5: Commit**

```powershell
git add -- src/main/java/com/l2hostility_tweaks/client src/main/java/com/l2hostility_tweaks/mixin src/main/resources/l2hostility_tweaks.mixins.json src/test
git commit -m "refactor: centralize item tooltip processing"
```

### Task 4: Correct trait detail ownership and validate translations

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/DispellTraitMixin.java`
- Modify: `build.gradle`

**Interfaces:**
- Produces: Mixin handler `l2fix$dispellImmunityDetail(List<Component>, CallbackInfo)` at `DispellTrait.addDetail` tail.
- Extends: `validateProjectResources` with exact pipeline-owned locale keys.

- [ ] **Step 1: Add a failing build-time ownership/translation assertion**

Extend `validateProjectResources` to require all pipeline-owned keys in both locale JSON files and to require `trait.l2hostility_tweaks.dispell.immunity`. Add a source-ownership check that this key occurs in `DispellTraitMixin.java`, not only in a base-class mixin.

- [ ] **Step 2: Run validation and verify RED**

Run: `.\gradlew.bat validateProjectResources`

Expected: failure stating that the Dispell immunity key is not owned by `DispellTraitMixin.java`.

- [ ] **Step 3: Add the targeted tail injection**

```java
@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
private void l2fix$dispellImmunityDetail(List<Component> list, CallbackInfo ci) {
    if (L2HConfig.isOldDispellEnabled()) {
        list.add(Component.translatable("trait.l2hostility_tweaks.dispell.immunity")
                .withStyle(ChatFormatting.GOLD));
    }
}
```

- [ ] **Step 4: Run validation and all tests and verify GREEN**

Run: `.\gradlew.bat test validateProjectResources`

Expected: all tests and validation tasks pass.

- [ ] **Step 5: Commit**

```powershell
git add -- build.gradle src/main/java/com/l2hostility_tweaks/mixin/DispellTraitMixin.java
git commit -m "fix: display old Dispell immunity detail"
```

### Task 5: Full verification and artifact inspection

**Files:**
- Verify only; no expected source changes.

**Interfaces:**
- Verifies all deliverables from Tasks 1-4.

- [ ] **Step 1: Run a clean complete build**

Run: `.\gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL`, tooltip tests pass, JSON resources validate, and Mixin registration is consistent.

- [ ] **Step 2: Inspect the JAR**

Run:

```powershell
& 'D:\JAVA\java25\bin\jar.exe' tf (Get-ChildItem build\libs\*.jar | Select-Object -First 1 -ExpandProperty FullName)
```

Expected: `TooltipPipeline.class`, `TooltipComponents.class`, and `TooltipPipelineMixin.class` are present; `AbrahadabraTooltipMixin.class` and `DetectorGlassesTooltipMixin.class` are absent.

- [ ] **Step 3: Inspect repository state and commits**

Run: `git status --short --branch; git log --oneline -6`

Expected: clean feature worktree with the design, plan, tests, pipeline, item migration, and Dispell fix commits visible.

- [ ] **Step 4: Do not deploy the artifact**

Leave the built JAR under `build/libs`. Do not copy it into any Minecraft `mods` directory.
