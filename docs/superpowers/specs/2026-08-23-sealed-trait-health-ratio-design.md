# Sealed Trait Health Ratio Design

## Problem

`TraitSymbolMixin` calculates a target's health ratio after `trait.initialize(target, 0)` has already refreshed attribute modifiers. If that refresh removes a stale maximum-health modifier, the calculated ratio describes the new state and cannot preserve the pre-refresh health percentage.

## Behavior

- Capture current health and maximum health before changing the sealed level or refreshing attributes.
- After the refresh and capability sync, restore the captured health percentage against the new maximum health.
- Cap the restored percentage at 100% and retain the existing minimum of one health point.
- Do not change sealed levels, symbol consumption, maximum-level rejection, or client/server flow.

## Verification

A pure regression test verifies that `60/100` becomes `30/50`, over-maximum health is capped at the new maximum, and zero health retains the existing one-point floor. Then run the complete clean build.
