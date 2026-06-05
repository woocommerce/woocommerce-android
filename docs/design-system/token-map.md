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

For PR 2 color mapping, use the local export summary
`docs/orchestrator/state/store-design-system-pr2-token-export-summary.md` as the primary source.
Use `~/Downloads/Woo theme/` only to verify source groups that the summary mentions but does not
enumerate, such as alert and palette rows. Do not copy raw variable IDs into repo docs.
The public color-surface decision in this file supersedes any older summary recommendation to keep
semantic/status colors internal.

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
| `WooTheme.colors` | Manual color exports | TBD | TBD | Internal `ColorScheme` projection only | needs_android_mapping | Every source-backed color token, grouped shallowly by intent. |
| `WooTheme.text` | Design type roles | TBD | TBD | Regular projection to `Typography` | needs_android_mapping | Exposes regular, emphasized, and strong variants. |
| `WooTheme.spacing` | Design spacing scale | TBD | TBD | No Material role | needs_android_mapping | Theme-scoped spacing accessor. |
| `WooTheme.padding` | Design padding scale | TBD | TBD | No Material role | needs_android_mapping | Separate group from spacing even when values match. |

These group rows are planning placeholders. When implementation marks a token `production`, add an
individual row for that public API or token.

## PR 2 Public Color Surface

`WooTheme.colors` should expose every source-backed Figma/manual-export color token available for
PR 2. Group the public API shallowly by source intent; do not collapse the source into a small
Material 3-like subset.

| Public group | Source-backed coverage |
| --- | --- |
| Core | Primary, on-primary, secondary, and on-secondary roles. |
| Background | Section background and section background variant roles, including matching on-colors. |
| Surface | Surface, on-surface tones, inverted surface, and inverted on-surface tones. |
| Text | Semantic text primary, secondary, tertiary, disabled, and on-primary. |
| Icon | Semantic icon tokens. |
| Border | `outline`, `outlineVariant`, default border, and focused border. |
| Status | Top-level status/background tones and semantic status tones. Keep duplicate-looking intents separate. |
| Interactive | Interactive primary, destructive, and pressed tokens. |
| Label | Semantic label primary, secondary, tertiary, disabled, and on-primary. |
| Overlay | Overlay opacity tokens and semantic surface overlay. Preserve exported alpha values. |
| Alert | Alert blue, green, red, and yellow rows verified from the manual export fallback. |
| Palette | Sandstone and Woo brand ramps verified from the manual export fallback. |

Public palette/ramp and alert tokens do not automatically approve foreground/background pairing.
Component-specific contrast is still required before using them for text, essential icons, or
required state communication.

## Mapping Rules

- Expose approved Store authoring roles through `WooTheme`, not directly through `MaterialTheme`.
- Preserve source-backed color intent in `WooTheme.colors` instead of generating public Material 3
  aliases.
- Keep `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MaterialTheme.shapes` populated
  as interop projections for Material 3 components, defaults, and helpers.
- Keep Material 3-only projection aliases internal. Do not expose generated fixed roles,
  `surfaceContainer*`, `surfaceDim`, `surfaceBright`, or similar aliases unless they are real
  source-backed tokens.
- `outline` and `outlineVariant` are source-backed and public under `WooTheme.colors`.
- Do not create a separate `WooTheme.semanticColors` group in PR 2.
- If a non-color Figma variable has no clean Material 3 role, add it as an internal adapter token
  first.
- Expose a non-color token with no clean Material 3 role publicly only when a production component
  or pilot screen needs it.
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
