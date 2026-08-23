# Tooltip Pipeline Design

## Goal

Make every tooltip owned or modified by L2Hostility Tweaks display reliably, coexist with other tooltip providers, and reflect the active gameplay configuration without changing gameplay behavior.

## Confirmed Defects

1. The old Dispell immunity translation exists but cannot be reached. `DispellTrait` overrides `addDetail`, while the existing immunity detail injection targets `MobTrait.addDetail`.
2. Custom enchantment descriptions are only translation entries. The current handler replaces an existing Reprint Counter description but never creates one, so descriptions depend on an external tooltip provider.
3. Reprint Counter replacement returns from the whole tooltip handler. This suppresses later Hostility Essence and Pocket of Restoration tooltip processing on compatible item stacks.
4. Reprint Counter replacement only recognizes a root `TranslatableContents`. Components wrapped by another mod or placed in siblings are not recognized.
5. Abrahadabra and Detector Glasses use separate `ItemStack#getTooltipLines` injections with hard local capture. They duplicate infrastructure and are more sensitive to method-local changes than a return-value processor.

## Architecture

Create a client-only `TooltipPipeline` as the single entry point for item tooltip normalization.

A single client Mixin injects at `ItemStack#getTooltipLines` return and passes the stack, nullable player, tooltip flag, and returned mutable component list to the pipeline. Running after the method returns ensures vanilla, item-level tooltip code, Forge `ItemTooltipEvent` listeners, and external description providers have already contributed their lines.

The existing Forge event subscriber retains rendering and interaction handlers, but item-tooltip mutation moves out of `ClientEventHandler`.

Trait descriptions remain attached to their actual trait `addDetail` methods because they are generated inside L2Hostility's trait description flow rather than as ordinary item metadata.

## Component Matching

The pipeline provides recursive component inspection:

- Match a translation key in the component root or any sibling.
- Preserve unrelated prefixes, suffixes, and styles supplied by other mods.
- Treat a matching description key as the canonical identity for replacement and deduplication.
- Never compare localized display strings.

This supports direct translatable lines and wrapped lines produced by tooltip enhancement mods.

## Enchantment Descriptions

For each supported enchantment, the pipeline performs an upsert:

1. Build the desired description component for the current stack and enchantment level.
2. Find all lines containing that enchantment's description key.
3. Replace the matching component node recursively while preserving its parent, unrelated siblings, and wrapper style.
4. Remove additional matches.
5. If no description exists, insert it immediately after the matching enchantment name line. If the name line cannot be identified, insert after the last recognized enchantment name, or directly after the item name when no enchantment line is recognizable.

Supported descriptions:

- Reprint Counter: armor, enchanted-book, and general variants with calculated reduction.
- Abyss Pocket: static localized description.
- Gluttony Pocket: static localized description.
- L2Hostility Split Suppressor: localized replacement description owned by this resource pack.

Processing every enchantment is independent. No handler may return early from the entire pipeline.

## Item Tooltips

The same pipeline handles:

- Abrahadabra minion-equipment behavior.
- Detector Glasses glow enabled/disabled state.
- Hostility Essence usage level.
- Pocket of Restoration extra sealed-item slots.

Handlers with stable translation keys are idempotent and deduplicate their own canonical keys. Dynamic pocket entries are reconstructed once from NBT after locating the upstream sealed-item section. They are not deduplicated by display text because multiple slots may legitimately contain items with identical names. Invalid or absent optional NBT entries are skipped without suppressing other tooltip lines.

The two old local-capture tooltip Mixins are removed from source and Mixin registration.

## Trait Tooltip Correction

`MobTraitImmunityMixin` continues to add the old Dementor immunity detail because Dementor inherits the base `addDetail` implementation.

Old Dispell immunity detail is added directly from `DispellTraitMixin`, after its customized detail line, and only while the old Dispell configuration is enabled. This matches the actual override hierarchy and keeps the ordinary Dispell count/time description intact.

## Ordering and Compatibility

- The pipeline runs once after all normal tooltip construction.
- Enchantment descriptions are skipped when `ItemStack.TooltipPart.ENCHANTMENTS` is hidden, and additional item details are skipped when `ItemStack.TooltipPart.ADDITIONAL` is hidden.
- Existing external description lines are reused or replaced rather than duplicated.
- Component identity uses translation keys, so locale changes do not affect matching.
- Each feature failure is isolated; absence of one expected upstream line falls back to insertion and does not stop later handlers.
- Client-only classes and Mixin registration prevent dedicated-server classloading regressions.

## Tests

Add focused automated tests for pure tooltip-list operations:

- Root and nested translation-key matching.
- Replace existing description.
- Insert missing description after its enchantment name.
- Remove duplicates.
- Preserve unrelated tooltip lines and wrapper content.
- Process multiple supported tooltips on one stack without short-circuiting.
- Idempotence of translation-key-based upsert operations when applied twice.
- Preservation of repeated dynamic pocket entries representing distinct slots.
- Respect for vanilla enchantment and additional-tooltip `HideFlags`.

Build-time validation is extended to verify that every pipeline-owned translation key exists in both `en_us` and `zh_cn`, and that removed Mixin classes are no longer registered.

## Verification

1. Run the focused tooltip tests and observe them fail before implementation.
2. Implement the pipeline and targeted Dispell correction.
3. Run tooltip tests, resource validation, Mixin registration validation, and the complete Gradle build.
4. Inspect the produced JAR contents for the new pipeline, client Mixin, language resources, and absence of removed Mixin classes.
5. Do not copy the JAR into any `mods` directory.

## Non-Goals

- No gameplay, balance, configuration, NBT schema, or network protocol changes.
- No redesign of upstream L2Hostility trait descriptions.
- No global modification of tooltips belonging to unrelated mods.
