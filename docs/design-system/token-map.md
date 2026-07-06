# Store Design System Token Map

This file maps Woo Mobile Design System i1 design intent to Store runtime tokens.

Figma remains the design-intent source of truth. Android owns the stable runtime API.
Do not expose raw Figma variable names or variable IDs in public Android APIs. Do not include raw P2
or Figma URLs or raw variable IDs in public repo docs.

Source references use public-repo shorthands:

- P2: `Woo Mobile Design System, i1`, May 27, 2026 (`pe5sF9-5ox-p2`).
- Figma file: `Woo Mobile Design System` (`50XIH5MmOf4xUYEkM6fAm6-fi`).
- Full Figma variable export: `figma-export.json`.

Use source shorthands in source-reference columns.

Agent note: Figma references use the public GitHub shorthand `{fileKey}-fi`. When using Figma tools,
strip the `-fi` suffix and pass only `{fileKey}` as the Figma `fileKey`.

## Figma Export Parsing Rules

`figma-export.json` is the committed full export of Figma variables used for foundation
reconciliation. Refreshing the export should preserve this parser contract:

- Parse Store runtime/public foundations from non-`Semantic` top-level export sections only.
- Keep the top-level `Semantic` section in the export for traceability, but ignore it when generating
  or updating Store runtime tokens, public token-map rows, `WooTheme.colors`,
  `WooTheme.semanticColors`, or Material 3 projections.
- Use normal `Light` / `Dark` values for runtime color modes. High-contrast modes remain
  export-backed traceability data until an accessibility-mode foundation is separately scoped.
- Use the Android mode for Android typography values.
- If a future component audit proves a `Semantic` variable is the intended source, update this
  token map with the component evidence and approved mapping before consuming that section.

Use the committed full export for row-level source paths and values. Keep unresolved notes in this
token map. Do not use older split token exports as current foundation sources.

The Figma `Color roles` frame validates the role inventory and light-mode values for Primary and
Secondary, Container, Surface, Outline, and Error/alert/success. Background, Overlay, and Palette are
export-backed by `figma-export.json` but are not validated by that frame.

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
| `WooTheme.colors` | `figma-export.json` / `Woo theme` | See color reconciliation table | See color reconciliation table | Projection to `ColorScheme` | production | Grouped Store authoring roles; Material 3 is an interop projection. |
| `WooTheme.text` | `figma-export.json` / `Typescale` and `Font theme` | See text table | Same values | Regular projection to `Typography` | production | Android mode values; Android default font is the runtime equivalent for source `Roboto`. |
| `WooTheme.spacing` | `figma-export.json` / `Spacing/Spacing` | `0..64dp` | Same values | No Material role | production | Theme-scoped spacing accessor. |
| `WooTheme.padding` | `figma-export.json` / `Spacing/Padding` | `0..64dp` | Same values | No Material role | production | Separate group from spacing even when values match. |
| `WooTheme.radius` | `figma-export.json` / `Shape/Corner-Radius` | `0..999dp` | Same values | Partial projection to `Shapes` | production | Includes Woo-only `none` and `full` tokens. |
| `WooTheme.iconSize` | `figma-export.json` / `Icon/Size` | `14..32dp` | Same values | No Material role | production | Glyph sizes only; not touch-target or layout sizing. |

## PR 2 Public Color Surface

`WooTheme.colors` exposes source-backed Store authoring roles from `figma-export.json` / `Woo theme`.
Group the public API shallowly by source intent; do not collapse the source into a small Material
3-like subset.

| Public group | Source-backed coverage |
| --- | --- |
| Core | Primary, on-primary, secondary, and on-secondary roles from the Color roles frame's "Primary and Secondary" section. |
| Container | Primary container, on-primary container, secondary container, and on-secondary container. These are accent containers; status containers stay under `status`. |
| Surface | Surface, surface dim, surface bright, surface container highest, on-default, on-variant, on-variant-lowest, inverted, and on-inverted roles. |
| Outline | `outline` and `outlineVariant`. |
| Status | Error, warning, caution, success, info, and neutral containers plus their on-container colors. |
| Alert | Red, orange, green, and blue alert ramp colors plus their on-colors. |
| Background | Export-backed background roles; not validated by the Color roles frame. |
| Overlay | Export-backed overlay roles; not validated by the Color roles frame. |
| Palette | Export-backed primitive/ramp data exposed as public `WooTheme.colors.palette.*` tokens. |

## Mapping Rules

- Expose approved Store authoring roles through `WooTheme`, not directly through `MaterialTheme`.
- Preserve source-backed color intent in `WooTheme.colors` instead of generating public Material 3
  aliases.
- Keep `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MaterialTheme.shapes` populated
  as interop projections for Material 3 components, defaults, and helpers.
- Keep Material 3-only projection aliases internal. Do not expose generated fixed roles or
  source-missing aliases as public Store roles.
- `surfaceDim` and `surfaceContainerHighest` are promoted source-backed Store roles. Do not filter
  them out as generated Material aliases.
- `surfaceBright` is export-backed and public under `WooTheme.colors.surface`.
- `outline` and `outlineVariant` are source-backed and public under `WooTheme.colors`.
- Keep state layers internal-first until semantic names and mode behavior are approved. Do not create
  public `WooTheme.stateAlpha` floats from mode-aware state-layer color tokens.
- Do not create a separate `WooTheme.semanticColors` group in PR 2.
- If a non-color Figma variable has no clean Material 3 role, add it as an internal adapter token
  first.
- Expose a non-color token with no clean Material 3 role publicly only when a production component
  or first-wave screen needs it.
- Follow [Token Ownership And XML/View Usage](#token-ownership-and-xmlview-usage) for resource-backed
  color and non-color foundation boundaries.
- Include light and dark values before marking a token `production`.
- Include the closest Material 3 role when the token maps to `ColorScheme`, typography, shape, or elevation.
- Mark unsettled tokens `preview_only`, `needs_design`, or `needs_android_mapping`.
- Do not wire design-system token resources into app-wide legacy styles by default. Product screens opt in through the design-system theme/components or targeted XML/View style usage.

## Token Ownership And XML/View Usage

The i1 adapter is Compose-first at the API/component layer. `WooTheme.colors` remains the stable
authoring API, while Store color primitives may be resource-backed implementation details for safe
XML/View convergence. Non-color primitive foundations remain Kotlin/Compose-owned implementation
details.

When defining tokens:

- Keep `WooTheme.colors` as the consuming API for color access even when the underlying color
  primitives are stored in module-local Android resources.
- Define non-color primitive foundations such as spacing, radius, icon sizing, and typography in
  Kotlin/Compose foundation code.
- Compose APIs should expose stable `WooTheme` and design-system component surfaces, not raw `R.color`
  or `R.dimen` usage to product screens.
- If a non-migrated XML/View screen needs a design-system color token, use the shared module-local
  color resource and keep Compose reading the same value through `WooTheme.colors`.
- XML/View styles may consume shared color resources only through targeted, opt-in style usage.
- Do not copy color token values into separate Kotlin/Compose constants and XML resources.
- Keep `token-map.md` as the audit trail for the token value, source shorthand, Material 3 role
  mapping, status, and notes.
- The rollout direction now allows scoped legacy XML and legacy Compose convergence for safe
  color/chrome tokens only; see [rollout-direction.md](rollout-direction.md). Global typography,
  spacing, and status/semantic remapping remain out of scope.
- Do not globally apply design-system XML/View styles in PR 2. Add targeted XML/View style usage only
  when a non-migrated XML/View screen needs design-system styling.

## Public Color Source Reconciliation

Every production `WooTheme.colors` field below is backed by normal `Light` / `Dark` values from
`figma-export.json` according to the Figma export parsing rules.

| Android API | Source path | Export file | Light hex / alpha | Dark hex / alpha | M3 projection | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `WooTheme.colors.primary` | `Woo theme/Primary` | `figma-export.json` | `#873EFF` / 100% | `#873EFF` / 100% | `primary` | production | Core primary. |
| `WooTheme.colors.onPrimary` | `Woo theme/On-Primary` | `figma-export.json` | `#FFFFFF` / 100% | `#FFFFFF` / 100% | `onPrimary` | production | Foreground for `primary`. |
| `WooTheme.colors.secondary` | `Woo theme/Secondary` | `figma-export.json` | `#6108CE` / 100% | `#383146` / 100% | `secondary` | production | Core secondary. |
| `WooTheme.colors.onSecondary` | `Woo theme/On-Secondary` | `figma-export.json` | `#FFFFFF` / 100% | `#F1EDFE` / 100% | `onSecondary` | production | Foreground for `secondary`. |
| `WooTheme.colors.container.primaryContainer` | `Woo theme/Primary-Container` | `figma-export.json` | `#B999FF` / 100% | `#FFFFFF` / 100% | `primaryContainer` | production | Accent container role. |
| `WooTheme.colors.container.onPrimaryContainer` | `Woo theme/On-Primary-Container` | `figma-export.json` | `#2C045D` / 100% | `#FFFFFF` / 100% | `onPrimaryContainer` | production | Foreground for `primaryContainer`. |
| `WooTheme.colors.container.secondaryContainer` | `Woo theme/Secondary-Container` | `figma-export.json` | `#F2EDFF` / 100% | `#FFFFFF` / 100% | `secondaryContainer` | production | Accent container role. |
| `WooTheme.colors.container.onSecondaryContainer` | `Woo theme/On-Secondary-Container` | `figma-export.json` | `#873EFF` / 100% | `#FFFFFF` / 100% | `onSecondaryContainer` | production | Foreground for `secondaryContainer`. |
| `WooTheme.colors.outline` | `Woo theme/Outline/Outline` | `figma-export.json` | `#787C82` / 100% | `#454549` / 100% | `outline` | production | Boundary token. |
| `WooTheme.colors.outlineVariant` | `Woo theme/Outline/Outline-Variant` | `figma-export.json` | `#DCDCDE` / 100% | `#5E5E63` / 100% | `outlineVariant` | production | Subtle boundary token. |
| `WooTheme.colors.background.section` | `Woo theme/Background/Section-Background` | `figma-export.json` | `#F2F2F8` / 100% | `#101517` / 100% | `background` | production | Section background. |
| `WooTheme.colors.background.onSection` | `Woo theme/Background/On-Section-Background` | `figma-export.json` | `#1E1E1E` / 100% | `#FFFFFF` / 100% | `onBackground` | production | Foreground for `section`. |
| `WooTheme.colors.background.sectionVariant` | `Woo theme/Background/Section-Background-Variant` | `figma-export.json` | `#F0F0F0` / 100% | `#101517` / 100% | `surfaceVariant` | production | Variant section background. |
| `WooTheme.colors.background.onSectionVariant` | `Woo theme/Background/On-Section-Background-Variant` | `figma-export.json` | `#1C1C1E` / 100% | `#8B8A8E` / 100% | No direct M3 role | production | Foreground for `sectionVariant`. |
| `WooTheme.colors.surface.default` | `Woo theme/Surface/Surface` | `figma-export.json` | `#FFFFFF` / 100% | `#232529` / 100% | `surface` | production | Default surface. |
| `WooTheme.colors.surface.surfaceDim` | `Woo theme/Surface/Surface-Dim` | `figma-export.json` | `#F6F7F7` / 100% | `#232529` / 100% | `surfaceDim` | production | Promoted source-backed surface role. |
| `WooTheme.colors.surface.surfaceContainerHighest` | `Woo theme/Surface/Surface-Container-Highest` | `figma-export.json` | `#DCDCDE` / 100% | `#232529` / 100% | `surfaceContainerHighest` | production | Promoted source-backed surface role. |
| `WooTheme.colors.surface.onDefault` | `Woo theme/Surface/On-Surface` | `figma-export.json` | `#000000` / 100% | `#FFFFFF` / 100% | `onSurface` | production | Foreground for default surface. |
| `WooTheme.colors.surface.onVariant` | `Woo theme/Surface/On-Surface-Variant` | `figma-export.json` | `#2C3338` / 100% | `#626068` / 100% | `onSurfaceVariant` | production | Variant foreground. |
| `WooTheme.colors.surface.onVariantLowest` | `Woo theme/Surface/On-Surface-Variant-Lowest` | `figma-export.json` | `#50575E` / 100% | `#626068` / 100% | No direct M3 role | production | Lowest variant foreground. |
| `WooTheme.colors.surface.inverted` | `Woo theme/Surface/Inverse-Surface` | `figma-export.json` | `#000000` / 100% | `#FFFFFF` / 100% | `inverseSurface` | production | Inverse surface. |
| `WooTheme.colors.surface.onInverted` | `Woo theme/Surface/On-Inverse-Surface` | `figma-export.json` | `#FFFFFF` / 100% | `#000000` / 100% | `inverseOnSurface` | production | Foreground for inverse surface. |
| `WooTheme.colors.surface.surfaceBright` | `Woo theme/Surface/Surface-Bright` | `figma-export.json` | `#FFFFFF` / 100% | `#232529` / 100% | `surfaceBright` | production | Source-backed surface role. |
| `WooTheme.colors.status.errorContainer` | `Woo theme/Alerts/Error-Container` | `figma-export.json` | `#F6E6E3` / 100% | `#F6E6E3` / 100% | `errorContainer` | production | Error container. |
| `WooTheme.colors.status.onErrorContainer` | `Woo theme/Alerts/On-Error-Container` | `figma-export.json` | `#470000` / 100% | `#470000` / 100% | `onErrorContainer` | production | Foreground for `errorContainer`. |
| `WooTheme.colors.status.warningContainer` | `Woo theme/Alerts/Warning-Container` | `figma-export.json` | `#FDE6BE` / 100% | `#FDE6BE` / 100% | No direct M3 role | production | Warning container. |
| `WooTheme.colors.status.onWarningContainer` | `Woo theme/Alerts/On-Warning-Container` | `figma-export.json` | `#2E1900` / 100% | `#2E1900` / 100% | No direct M3 role | production | Foreground for `warningContainer`. |
| `WooTheme.colors.status.cautionContainer` | `Woo theme/Alerts/Caution-Container` | `figma-export.json` | `#FEE995` / 100% | `#FEE995` / 100% | No direct M3 role | production | Caution container. |
| `WooTheme.colors.status.onCautionContainer` | `Woo theme/Alerts/On-Caution-Container` | `figma-export.json` | `#281D00` / 100% | `#281D00` / 100% | No direct M3 role | production | Foreground for `cautionContainer`. |
| `WooTheme.colors.status.successContainer` | `Woo theme/Alerts/Success-Container` | `figma-export.json` | `#C6F7CD` / 100% | `#C6F7CD` / 100% | No direct M3 role | production | Success container. |
| `WooTheme.colors.status.onSuccessContainer` | `Woo theme/Alerts/On-Success-Container` | `figma-export.json` | `#002900` / 100% | `#002900` / 100% | No direct M3 role | production | Foreground for `successContainer`. |
| `WooTheme.colors.status.infoContainer` | `Woo theme/Alerts/Info-Container` | `figma-export.json` | `#DEEBFA` / 100% | `#DEEBFA` / 100% | No direct M3 role | production | Info container. |
| `WooTheme.colors.status.onInfoContainer` | `Woo theme/Alerts/On-Info-Container` | `figma-export.json` | `#001B4F` / 100% | `#001B4F` / 100% | No direct M3 role | production | Foreground for `infoContainer`. |
| `WooTheme.colors.status.neutralContainer` | `Woo theme/Alerts/Neutral-Container` | `figma-export.json` | `#F4F4F4` / 100% | `#F4F4F4` / 100% | No direct M3 role | production | Neutral container. |
| `WooTheme.colors.status.onNeutralContainer` | `Woo theme/Alerts/On-Neutral-Container` | `figma-export.json` | `#1E1E1E` / 100% | `#1E1E1E` / 100% | No direct M3 role | production | Foreground for `neutralContainer`. |
| `WooTheme.colors.overlay.overlay20` | `Woo theme/Overlay/Opacity-20` | `figma-export.json` | `#000000` / 20% | `#000000` / 20% | No direct M3 role | production | Overlay color. |
| `WooTheme.colors.overlay.overlay50` | `Woo theme/Overlay/Opacity-50` | `figma-export.json` | `#000000` / 50% | `#000000` / 75% | `scrim` | production | Overlay color. |
| `WooTheme.colors.alert.red` | `Woo theme/Alerts/Red` | `figma-export.json` | `#FC4A5B` / 100% | `#DC3545` / 100% | No direct M3 role | production | Alert ramp color. |
| `WooTheme.colors.alert.onRed` | `Woo theme/Alerts/On-Red` | `figma-export.json` | `#FFFFFF` / 100% | `#DC3545` / 100% | No direct M3 role | production | Foreground for `red`. |
| `WooTheme.colors.alert.orange` | `Woo theme/Alerts/Orange` | `figma-export.json` | `#FF9000` / 100% | `#EAAB2D` / 100% | No direct M3 role | production | Alert ramp color. |
| `WooTheme.colors.alert.onOrange` | `Woo theme/Alerts/On-Orange` | `figma-export.json` | `#FFFFFF` / 100% | `#EAAB2D` / 100% | No direct M3 role | production | Foreground for `orange`. |
| `WooTheme.colors.alert.green` | `Woo theme/Alerts/Green` | `figma-export.json` | `#27AE32` / 100% | `#69B66F` / 100% | No direct M3 role | production | Alert ramp color. |
| `WooTheme.colors.alert.onGreen` | `Woo theme/Alerts/On-Green` | `figma-export.json` | `#FFFFFF` / 100% | `#69B66F` / 100% | No direct M3 role | production | Foreground for `green`. |
| `WooTheme.colors.alert.blue` | `Woo theme/Alerts/Blue` | `figma-export.json` | `#1E94D0` / 100% | `#1E94D0` / 100% | No direct M3 role | production | Alert ramp color. |
| `WooTheme.colors.alert.onBlue` | `Woo theme/Alerts/On-Blue` | `figma-export.json` | `#FFFFFF` / 100% | `#1E94D0` / 100% | No direct M3 role | production | Foreground for `blue`. |
| `WooTheme.colors.palette.sandstone.shade5` | `Woo theme/Add-On-Colors/Woo-Sandstone/5` | `figma-export.json` | `#FBF9F6` / 100% | `#FBF9F6` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.sandstone.shade10` | `Woo theme/Add-On-Colors/Woo-Sandstone/10` | `figma-export.json` | `#F1EEEB` / 100% | `#F1EEEB` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.sandstone.shade20` | `Woo theme/Add-On-Colors/Woo-Sandstone/20` | `figma-export.json` | `#E6E2DE` / 100% | `#E6E2DE` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.sandstone.shade40` | `Woo theme/Add-On-Colors/Woo-Sandstone/40` | `figma-export.json` | `#C5C2BF` / 100% | `#C5C2BF` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.sandstone.shade60` | `Woo theme/Add-On-Colors/Woo-Sandstone/60` | `figma-export.json` | `#8B8A89` / 100% | `#8B8A89` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooBlue.shade20` | `Woo theme/Add-On-Colors/Woo-Blue/20` | `figma-export.json` | `#75FFFF` / 100% | `#75FFFF` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooBlue.shade40` | `Woo theme/Add-On-Colors/Woo-Blue/40` | `figma-export.json` | `#1AD0FD` / 100% | `#1AD0FD` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooBlue.shade60` | `Woo theme/Add-On-Colors/Woo-Blue/60` | `figma-export.json` | `#05096C` / 100% | `#05096C` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooGreen.shade20` | `Woo theme/Add-On-Colors/Woo-Green/20` | `figma-export.json` | `#D5FF4A` / 100% | `#D5FF4A` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooGreen.shade40` | `Woo theme/Add-On-Colors/Woo-Green/40` | `figma-export.json` | `#06E782` / 100% | `#06E782` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooGreen.shade60` | `Woo theme/Add-On-Colors/Woo-Green/60` | `figma-export.json` | `#083D2D` / 100% | `#083D2D` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooOrange.shade20` | `Woo theme/Add-On-Colors/Woo-Orange/20` | `figma-export.json` | `#FFE500` / 100% | `#FFE500` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooOrange.shade40` | `Woo theme/Add-On-Colors/Woo-Orange/40` | `figma-export.json` | `#FF9000` / 100% | `#FF9000` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooOrange.shade60` | `Woo theme/Add-On-Colors/Woo-Orange/60` | `figma-export.json` | `#FF4800` / 100% | `#FF4800` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPink.shade20` | `Woo theme/Add-On-Colors/Woo-Pink/20` | `figma-export.json` | `#FCA8FF` / 100% | `#FCA8FF` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPink.shade40` | `Woo theme/Add-On-Colors/Woo-Pink/40` | `figma-export.json` | `#FF45E3` / 100% | `#FF45E3` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPink.shade60` | `Woo theme/Add-On-Colors/Woo-Pink/60` | `figma-export.json` | `#4E0061` / 100% | `#4E0061` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade0` | `Woo theme/Add-On-Colors/Woo-Purple/0` | `figma-export.json` | `#F2EDFF` / 100% | `#F2EDFF` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade5` | `Woo theme/Add-On-Colors/Woo-Purple/5` | `figma-export.json` | `#E1D7FF` / 100% | `#E1D7FF` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade10` | `Woo theme/Add-On-Colors/Woo-Purple/10` | `figma-export.json` | `#D1C1FF` / 100% | `#D1C1FF` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade20` | `Woo theme/Add-On-Colors/Woo-Purple/20` | `figma-export.json` | `#B999FF` / 100% | `#B999FF` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade30` | `Woo theme/Add-On-Colors/Woo-Purple/30` | `figma-export.json` | `#A77EFF` / 100% | `#A77EFF` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade40` | `Woo theme/Add-On-Colors/Woo-Purple/40` | `figma-export.json` | `#873EFF` / 100% | `#873EFF` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade50` | `Woo theme/Add-On-Colors/Woo-Purple/50` | `figma-export.json` | `#720EEC` / 100% | `#720EEC` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade60` | `Woo theme/Add-On-Colors/Woo-Purple/60` | `figma-export.json` | `#6108CE` / 100% | `#6108CE` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade70` | `Woo theme/Add-On-Colors/Woo-Purple/70` | `figma-export.json` | `#5007AA` / 100% | `#5007AA` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade80` | `Woo theme/Add-On-Colors/Woo-Purple/80` | `figma-export.json` | `#3C087E` / 100% | `#3C087E` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade90` | `Woo theme/Add-On-Colors/Woo-Purple/90` | `figma-export.json` | `#2C045D` / 100% | `#2C045D` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.wooPurple.shade100` | `Woo theme/Add-On-Colors/Woo-Purple/100` | `figma-export.json` | `#1F0342` / 100% | `#1F0342` / 100% | No direct M3 role | production | Public palette ramp token. |
| `WooTheme.colors.palette.gray.0..100` | `Woo theme/Add-On-Colors/Gray/0..100` | `figma-export.json` | Gray ramp / 100% | Gray ramp / 100% | No direct M3 role | production | Public gray palette ramp token. |

## Material 3 Color Projection

`WooDesignSystemTheme` builds a partial `ColorScheme` through Material 3 `1.4.0`
`lightColorScheme(...)` / `darkColorScheme(...)` builders. Direct rows reuse public source fields.
Alias rows are internal projection decisions only and are not additional public `WooTheme.colors`
fields. Omitted rows intentionally use Material 3 defaults until a source-backed Store token is
approved. Projection inputs follow the Figma export parsing rules. In this projection table only,
`defaulted` means omitted from the builder to use Material defaults, and `implicit` means resolved by
the Material builder from another supplied source role.

| Material role | Projection source | Status | Notes |
| --- | --- | --- | --- |
| `primary` | `WooTheme.colors.primary` | production | Direct source-backed projection. |
| `onPrimary` | `WooTheme.colors.onPrimary` | production | Direct source-backed projection. |
| `primaryContainer` | `WooTheme.colors.container.primaryContainer` | production | Source-backed accent container role. |
| `onPrimaryContainer` | `WooTheme.colors.container.onPrimaryContainer` | production | Source-backed foreground for `primaryContainer`. |
| `secondary` | `WooTheme.colors.secondary` | production | Direct source-backed projection. |
| `onSecondary` | `WooTheme.colors.onSecondary` | production | Direct source-backed projection. |
| `secondaryContainer` | `WooTheme.colors.container.secondaryContainer` | production | Source-backed accent container role. |
| `onSecondaryContainer` | `WooTheme.colors.container.onSecondaryContainer` | production | Source-backed foreground for `secondaryContainer`. |
| `tertiary` | `WooTheme.colors.secondary` | production | Internal alias to avoid baseline Material pink until a distinct tertiary source appears. |
| `onTertiary` | `WooTheme.colors.onSecondary` | production | Internal alias to avoid baseline Material pink until a distinct tertiary source appears. |
| `tertiaryContainer` | `WooTheme.colors.container.secondaryContainer` | production | Internal alias to avoid baseline Material pink until a distinct tertiary source appears. |
| `onTertiaryContainer` | `WooTheme.colors.container.onSecondaryContainer` | production | Internal alias to avoid baseline Material pink until a distinct tertiary source appears. |
| `background` | `WooTheme.colors.background.section` | production | Direct source-backed projection. |
| `onBackground` | `WooTheme.colors.background.onSection` | production | Direct source-backed projection. |
| `surface` | `WooTheme.colors.surface.default` | production | Direct source-backed projection. |
| `onSurface` | `WooTheme.colors.surface.onDefault` | production | Direct source-backed projection. |
| `surfaceVariant` | `WooTheme.colors.background.sectionVariant` | production | Source-backed projection, not a public Material mirror. |
| `onSurfaceVariant` | `WooTheme.colors.surface.onVariant` | production | Direct source-backed projection. |
| `inverseSurface` | `WooTheme.colors.surface.inverted` | production | Direct source-backed projection. |
| `inverseOnSurface` | `WooTheme.colors.surface.onInverted` | production | Direct source-backed projection. |
| `error` | Material 3 default | defaulted | Intentionally not projected. Source `Error` is a container/background; foreground/control error tokens are pending design. |
| `onError` | Material 3 default | defaulted | Intentionally paired with Material 3 default `error`; do not use `alert.red` as a global control-error token without design approval. |
| `errorContainer` | `WooTheme.colors.status.errorContainer` | production | Direct source-backed projection. |
| `onErrorContainer` | `WooTheme.colors.status.onErrorContainer` | production | Direct source-backed projection. |
| `outline` | `WooTheme.colors.outline` | production | Direct source-backed projection. |
| `outlineVariant` | `WooTheme.colors.outlineVariant` | production | Direct source-backed projection. |
| `scrim` | `WooTheme.colors.overlay.overlay50` | production | Source-backed projection; dark alpha is 75%. |
| `surfaceBright` | `WooTheme.colors.surface.surfaceBright` | production | Direct source-backed projection. |
| `surfaceDim` | `WooTheme.colors.surface.surfaceDim` | production | Promoted source-backed Store surface role. |
| `surfaceContainer` | `WooTheme.colors.background.section` | production | Internal surface alias for navigation bars, menus, cards, and sheets. |
| `surfaceContainerHigh` | `WooTheme.colors.surface.default` | production | Internal surface alias for Material container hierarchy. |
| `surfaceContainerHighest` | `WooTheme.colors.surface.surfaceContainerHighest` | production | Promoted source-backed Store surface role. |
| `surfaceContainerLow` | `WooTheme.colors.background.sectionVariant` | production | Internal surface alias for elevated cards and subtle containers. |
| `surfaceContainerLowest` | `WooTheme.colors.surface.default` | production | Internal surface alias for Material container hierarchy. |
| `surfaceTint` | Material 3 builder default | implicit | Builder default resolves to the passed Woo `primary`. |
| `inversePrimary` | Material 3 default | defaulted | Keeps snackbar/action behavior on Material defaults until a source-backed inverse accent is approved. |
| `primaryFixed*`, `secondaryFixed*`, `tertiaryFixed*` | Material 3 default | defaulted | Fixed roles are omitted until a source-backed Store fixed-role set or consuming component requires them. |

## Public Text Tokens

`WooTheme.text` exposes all 15 Android type roles from `figma-export.json` / `Typescale` and
`Font theme`, with `regular`, `emphasized`, and `strong` variants. The regular variant is projected
to `MaterialTheme.typography`. Android values must read the Android mode, and Android default font is
the runtime equivalent for source `Roboto`. Export role names are hyphenated, while Android docs/API
names are camelCase.

`Typescale/<Role>/Font` resolves through `Font theme/Font/Plain`, whose Android value is `Roboto`.
Android default font is the approved runtime equivalent. Most regular weights use `Weight`, but
`Display-Large` and `Body-Small` use
`Weight-Regular`. Most strong weights use `Weight-Strong`, but `Display-Large` uses
`Weight-Extra-Emphasized`. Displayed tracking values may be rounded in docs; implementation should
preserve source-backed values.

| Android API | Size | Line height | Tracking | Weights | Material projection | Status | Notes |
| --- | ---: | ---: | ---: | --- | --- | --- | --- |
| `WooTheme.text.displayLarge` | `56sp` | `64sp` | `-0.41sp` | regular / medium / bold | `displayLarge.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.displayMedium` | `48sp` | `52sp` | `-0.41sp` | regular / medium / bold | `displayMedium.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.displaySmall` | `36sp` | `44sp` | `-0.41sp` | regular / medium / bold | `displaySmall.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.headlineLarge` | `34sp` | `40sp` | `-1.40sp` | regular / medium / bold | `headlineLarge.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.headlineMedium` | `28sp` | `36sp` | `-0.41sp` | regular / medium / bold | `headlineMedium.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.headlineSmall` | `24sp` | `32sp` | `-1.00sp` | regular / medium / bold | `headlineSmall.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.titleLarge` | `20sp` | `28sp` | `-0.41sp` | regular / medium / bold | `titleLarge.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.titleMedium` | `17sp` | `20sp` | `-0.41sp` | regular / medium / bold | `titleMedium.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.titleSmall` | `14sp` | `16sp` | `-0.41sp` | regular / medium / bold | `titleSmall.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.bodyLarge` | `17sp` | `24sp` | `-0.41sp` | regular / medium / bold | `bodyLarge.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.bodyMedium` | `15sp` | `20sp` | `-0.41sp` | regular / medium / bold | `bodyMedium.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.bodySmall` | `13sp` | `16sp` | `-0.41sp` | regular / medium / bold | `bodySmall.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.labelLarge` | `16sp` | `24sp` | `-0.41sp` | regular / medium / bold | `labelLarge.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.labelMedium` | `14sp` | `20sp` | `-0.41sp` | regular / medium / bold | `labelMedium.regular` | production | Source-backed role using the DS runtime font-family decision. |
| `WooTheme.text.labelSmall` | `10sp` | `14sp` | `-0.07sp` | regular / medium / bold | `labelSmall.regular` | production | Source-backed role using the DS runtime font-family decision. |

## Spacing And Padding

Spacing and padding use the same i1 primitive scale today, but remain separate public groups because
they encode different design intent.

| Android API | Source path | Value | Material projection | Status | Notes |
| --- | --- | ---: | --- | --- | --- |
| `WooTheme.spacing.space0` | `Spacing/Spacing/0` | `0dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space1` | `Spacing/Spacing/1` | `2dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space2` | `Spacing/Spacing/2` | `4dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space3` | `Spacing/Spacing/3` | `8dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space4` | `Spacing/Spacing/4` | `12dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space5` | `Spacing/Spacing/5` | `16dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space6` | `Spacing/Spacing/6` | `20dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space7` | `Spacing/Spacing/7` | `24dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space8` | `Spacing/Spacing/8` | `32dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space9` | `Spacing/Spacing/9` | `40dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space10` | `Spacing/Spacing/10` | `48dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space11` | `Spacing/Spacing/11` | `56dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space12` | `Spacing/Spacing/12` | `64dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding0` | `Spacing/Padding/0` | `0dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding1` | `Spacing/Padding/1` | `2dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding2` | `Spacing/Padding/2` | `4dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding3` | `Spacing/Padding/3` | `8dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding4` | `Spacing/Padding/4` | `12dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding5` | `Spacing/Padding/5` | `16dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding6` | `Spacing/Padding/6` | `20dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding7` | `Spacing/Padding/7` | `24dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding8` | `Spacing/Padding/8` | `32dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding9` | `Spacing/Padding/9` | `40dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding10` | `Spacing/Padding/10` | `48dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding11` | `Spacing/Padding/11` | `56dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding12` | `Spacing/Padding/12` | `64dp` | No Material role | production | Source-ready. |

## Radius, Icon Size, And Internal Stroke

Radius is source-backed and public through `WooTheme.radius`. Icon size is source-backed and public
through `WooTheme.iconSize`, scoped to glyph sizes only. Stroke remains internal in PR2. Do not add
public stroke, elevation, state-layer, or minimum-touch-target accessors on the Store design-system
theme namespace.

| Token | Source path | Value | Material projection | Status | Notes |
| --- | --- | ---: | --- | --- | --- |
| `WooTheme.radius.none` | `Shape/Corner-Radius/None` | `0dp` | No Material role | production | Public zero-radius token. |
| `WooTheme.radius.extraSmall` | `Shape/Corner-Radius/Extra-small` | `2dp` | `MaterialTheme.shapes.extraSmall` | production | Shape projection. |
| `WooTheme.radius.small` | `Shape/Corner-Radius/Small` | `4dp` | `MaterialTheme.shapes.small` | production | Shape projection. |
| `WooTheme.radius.medium` | `Shape/Corner-Radius/Medium` | `8dp` | `MaterialTheme.shapes.medium` | production | Shape projection. |
| `WooTheme.radius.large` | `Shape/Corner-Radius/Large` | `12dp` | `MaterialTheme.shapes.large` | production | Shape projection; visual review risk because current projection is `8dp`. |
| `WooTheme.radius.extraLarge` | `Shape/Corner-Radius/Extra-large` | `16dp` | `MaterialTheme.shapes.extraLarge` | production | Shape projection; visual review risk because current projection is `8dp`. |
| `WooTheme.radius.full` | `Shape/Corner-Radius/Full` | `999dp` | No Material role | production | Woo-only pill/full-radius sentinel. |
| `WooTheme.iconSize.size14` | `Icon/Size/Extra-small` | `14dp` | No Material role | production | Glyph size only. |
| `WooTheme.iconSize.size16` | `Icon/Size/Small` | `16dp` | No Material role | production | Glyph size only. |
| `WooTheme.iconSize.size18` | `Icon/Size/Medium` | `18dp` | No Material role | production | Glyph size only. |
| `WooTheme.iconSize.size20` | `Icon/Size/Large` | `20dp` | No Material role | production | Glyph size only. |
| `WooTheme.iconSize.size24` | `Icon/Size/Large-Increased` | `24dp` | No Material role | production | Glyph size only. |
| `WooTheme.iconSize.size32` | `Icon/Size/Extra-Large` | `32dp` | No Material role | production | Glyph size only. |
| `WooStroke.none` | `Shape/Stroke/Weight/None` | `0dp` | No Material role | preview_only | Internal only. |
| `WooStroke.extraThin` | `Shape/Stroke/Weight/Extra-Thin` | `0.5dp` | No Material role | preview_only | Internal only; verify fractional rendering before broad use. |
| `WooStroke.thin` | `Shape/Stroke/Weight/Thin` | `0.75dp` | No Material role | preview_only | Internal only; verify fractional rendering before broad use. |
| `WooStroke.regular` | `Shape/Stroke/Weight/Regular` | `1dp` | No Material role | preview_only | Internal only. |
| `WooStroke.medium` | `Shape/Stroke/Weight/Medium` | `1.5dp` | No Material role | preview_only | Internal only; verify fractional rendering before broad use. |
| `WooStroke.mediumIncreased` | `Shape/Stroke/Weight/Medium-Increased` | `2dp` | No Material role | preview_only | Internal only. |
| `WooStroke.thick` | `Shape/Stroke/Weight/Thick` | `3dp` | No Material role | preview_only | Internal only. |
| `WooStroke.extraThick` | `Shape/Stroke/Weight/Extra-Thick` | `4dp` | No Material role | preview_only | Internal only. |

## Unresolved Groups

| Group | Source status | Android status | Public API | Notes |
| --- | --- | --- | --- | --- |
| Elevation | No non-`Semantic` elevation, shadow, effect, z-depth, or tonal-elevation source found in `figma-export.json` | needs_design | None | Do not promote legacy app elevation resources or hardcoded shadows to Store Design System tokens without design source. |
| State layers | `Woo theme/State-Layers/On-Surface/Opacity-08`, `Opacity-10`, and `Opacity-16` exist as mode-aware color tokens | needs_android_mapping | None | State semantics and dark/high-contrast behavior need design/API approval; do not create public `WooTheme.stateAlpha` floats. |
| Minimum touch target | No non-`Semantic` minimum-touch-target source found in `figma-export.json` | needs_design | None | Preserve accessible component behavior and legacy `48dp` guidance, but do not create a public token from legacy dimensions or screen-size variables. |
| High-contrast color modes | Present for color tokens | needs_android_mapping | None | Exclude from normal `Light` / `Dark` runtime mapping until separately scoped. |
