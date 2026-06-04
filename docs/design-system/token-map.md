# Store Design System Token Map

This file maps Woo Mobile Design System i1 design intent to Android runtime tokens.

Figma remains the design-intent source of truth. Android owns the stable runtime API. Do not expose raw Figma variable names as public Android API.

Source references use public-repo shorthands:

- P2: `Woo Mobile Design System, i1`, May 27, 2026 (`pe5sF9-5ox-p2`).
- Figma file: `Woo Mobile Design System` (`50XIH5MmOf4xUYEkM6fAm6-fi`).

Use Figma shorthands in the `Figma reference` column, optionally with node IDs when useful. Do not use raw P2 or Figma URLs in public repo docs.

## Status Values

- `production`: Stable enough for Store screens to consume.
- `preview_only`: Useful for previews or catalog work, but not ready for production screen adoption.
- `needs_design`: Missing, inconsistent, or not signed off in design.
- `needs_android_mapping`: Clear design intent exists, but the Android token/API still needs implementation work.

## Token Map

| Android token | Figma reference | Light value | Dark value | Material 3 role mapping | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| TBD | TBD | TBD | TBD | TBD | needs_android_mapping | Fill during foundation implementation. |

## Mapping Rules

- Prefer Material 3 role names where the design maps cleanly, such as `surface`, `onSurface`, or `primary`.
- Add Woo-specific semantic tokens only when the design system adds meaning that Material 3 roles do not express.
- If a Figma variable has no clean Material 3 role, add it as an internal adapter token first.
- Expose a token with no clean Material 3 role publicly only when a production component or pilot screen needs it.
- Keep i1 token primitive values Kotlin/Compose-owned at first.
- Do not create Android XML resources for design-system tokens until a real XML/View use case needs them.
- When XML/View needs a token, move that token's primitive value to Android resources and update Compose to read from the same resource.
- Do not keep parallel Kotlin/Compose and XML resource definitions for the same token primitive value.
- Keep Figma variable names, IDs, or page references in this document, not in public Kotlin API.
- Include light and dark values before marking a token `production`.
- Include the closest Material 3 role when the token maps to `ColorScheme`, typography, shape, or elevation.
- Mark unsettled tokens `preview_only`, `needs_design`, or `needs_android_mapping`.
- Do not wire design-system token resources into app-wide legacy styles by default. Product screens opt in through the design-system theme/components or targeted XML/View style usage.

## Token Ownership And XML/View Promotion

The i1 adapter is Compose-first at the API/component layer, and i1 token primitive values start as Kotlin/Compose-owned implementation details.

When defining tokens:

- Define color, spacing, radius, icon sizing, typography, and similar primitive values in Kotlin/Compose foundation code first.
- Compose APIs should expose stable design-system theme/component surfaces, not raw `R.color` or `R.dimen` usage to product screens.
- If a non-migrated XML/View screen needs a design-system token, move only that token's primitive value to Android resources and update Compose to read from the same resource.
- XML/View styles may consume promoted token resources only through targeted, opt-in style usage.
- Do not copy token values into separate Kotlin/Compose constants and XML resources.
- Keep `token-map.md` as the audit trail for the token value, Figma reference, Material 3 role mapping, status, and notes.
- Avoid global XML theme/resource remapping unless a later design-system decision explicitly changes the rollout strategy.
- Do not globally apply design-system XML/View styles in PR 2. Add targeted XML/View style usage only when a non-migrated XML/View screen needs design-system styling.

## Token Groups

Track i1 foundation groups here as implementation progresses:

- Color roles.
- Typography.
- Spacing.
- Radius.
- Elevation.
- Icon sizing.
- Interaction/state tokens.
