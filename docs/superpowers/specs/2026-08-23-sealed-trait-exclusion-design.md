# Sealed Trait Exclusion Design

## Problem

Player self-use exclusion validation only considers stored trait levels greater than zero. A temporarily sealed conflicting trait has a negative raw level, so it is ignored while adding the other trait. Automatic unsealing later restores both traits without another exclusion check.

## Behavior

- Treat every non-zero raw level as an existing trait during player self-use exclusion validation.
- Positive and sealed negative conflicting traits both reject the symbol use through the existing mutual-exclusion message.
- Null and zero entries remain absent.
- Do not delete existing conflicting traits or change automatic unsealing, symbol consumption, generation exclusions, or configuration semantics.

## Verification

Extend the existing self-use mixin tests to prove positive and negative raw levels are present for exclusion while zero and null are absent. Run the focused test and the complete clean build.
