# Sealed Self-Trait Validation Design

## Problem

Player self-use reads `MobTraitCap#getTraitLevel`, which deliberately exposes sealed negative levels as zero. A sealed max-level trait therefore passes the max-level check and can consume symbols without gaining a level. The same zero/positive-only view can undercount stored sealed traits in the maximum trait-count check.

## Behavior

- Validation uses the raw value stored in `cap.traits`.
- Any non-zero raw value, positive or negative, counts as an existing trait.
- Maximum-level validation compares `abs(rawLevel)` with the trait maximum.
- A sealed max-level trait is rejected without removing the seal or consuming symbols.
- A sealed below-max trait retains existing behavior: self-use removes the seal and increases the absolute level by one.
- Cost and balance calculations retain their current behavior.

## Verification

Regression tests cover rejection of raw `-3` at maximum level 3, acceptance of raw `-2`, and projected trait counting with sealed entries. Run the focused test first, then `gradlew.bat clean build`.
