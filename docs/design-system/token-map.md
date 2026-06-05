# Store Design System Token Map

This file maps Woo Mobile Design System i1 design intent to Store runtime tokens.

Figma remains the design-intent source of truth. Android owns the stable runtime API.
Do not expose raw Figma variable names or variable IDs in public Android APIs. Do not include raw P2
or Figma URLs in public repo docs.

Source references use public-repo shorthands:

- P2: `Woo Mobile Design System, i1`, May 27, 2026 (`pe5sF9-5ox-p2`).
- Figma file: `Woo Mobile Design System` (`50XIH5MmOf4xUYEkM6fAm6-fi`).

Use source shorthands in the source-reference column, optionally with node IDs when useful. Do not
use raw P2 or Figma URLs in public repo docs.

## Status Values

- `production`: Stable enough for Store screens to consume.
- `preview_only`: Useful for previews or catalog work, but not ready for production screen adoption.
- `needs_design`: Missing, inconsistent, or not signed off in design.
- `needs_android_mapping`: Clear design intent exists, but the Android token/API still needs implementation work.

## Public Foundation Surface

Production Store screens and design-system components read approved foundations from `WooTheme.*`.
`WooDesignSystemTheme` projects the same source values into `MaterialTheme` for Material 3 component
interop.

| Android API | Source reference | Light value | Dark value | Material 3 role mapping | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `WooTheme.colors` | Design color roles | TBD | TBD | Backed by `ColorScheme` projection | needs_android_mapping | Curated production color roles only; not the full Material 3 role set. |
| `WooTheme.text` | Design type roles | TBD | TBD | Regular projection to `Typography` | needs_android_mapping | Exposes regular, emphasized, and strong variants. |
| `WooTheme.spacing` | Design spacing scale | TBD | TBD | No Material role | needs_android_mapping | Theme-scoped spacing accessor. |
| `WooTheme.padding` | Design padding scale | TBD | TBD | No Material role | needs_android_mapping | Separate group from spacing even when values match. |

These group rows are planning placeholders. When implementation marks a token `production`, add an
individual row for that public API or token.

## Mapping Rules

- Prefer Material 3 role names where the design maps cleanly, such as `surface`, `onSurface`, or `primary`.
- Expose approved Store authoring roles through `WooTheme`, not directly through `MaterialTheme`.
- Keep `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MaterialTheme.shapes` populated
  as interop projections for Material 3 components, defaults, and helpers.
- `WooTheme.colors` should contain only curated, production-approved roles. Do not mirror every
  Material 3 `ColorScheme` role into the public Store API.
- Add Woo-specific semantic colors to `WooTheme.colors` only when the design system adds meaning that
  Material 3 roles do not express and the role is approved for production use.
- Do not create a separate `WooTheme.semanticColors` group in PR 2.
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
- Compose APIs should expose stable `WooTheme` and design-system component surfaces, not raw `R.color`
  or `R.dimen` usage to product screens.
- If a non-migrated XML/View screen needs a design-system token, move only that token's primitive value to Android resources and update Compose to read from the same resource.
- XML/View styles may consume promoted token resources only through targeted, opt-in style usage.
- Do not copy token values into separate Kotlin/Compose constants and XML resources.
- Keep `token-map.md` as the audit trail for the token value, source shorthand, Material 3 role mapping, status, and notes.
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
