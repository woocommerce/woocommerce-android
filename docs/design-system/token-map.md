# Store Design System Token Map

This file maps Woo Mobile Design System i1 design intent to Store runtime tokens.

Figma remains the design-intent source of truth. Android owns the stable runtime API.
Do not expose raw Figma variable names or variable IDs in public Android APIs. Do not include raw P2
or Figma URLs in public repo docs.

Source references use public-repo shorthands:

- P2: `Woo Mobile Design System, i1`, May 27, 2026 (`pe5sF9-5ox-p2`).
- Figma file: `Woo Mobile Design System` (`50XIH5MmOf4xUYEkM6fAm6-fi`).
- Manual export summary: `docs/orchestrator/state/store-design-system-pr2-token-export-summary.md`.
- Local manual export JSON files under `~/Downloads/Woo theme/` are used only for source verification where noted.

Use source shorthands in the source-reference column, optionally with node IDs when useful. Do not
use raw P2 or Figma URLs in public repo docs.

For PR 2 color mapping, use the local export summary
`docs/orchestrator/state/store-design-system-pr2-token-export-summary.md` as the primary source.
Use `~/Downloads/Woo theme/` only to verify source groups that the summary mentions but does not
enumerate, such as alert and palette rows. Do not copy raw variable IDs into repo docs.
The manual `Semantic/*.tokens.json` export is not part of the PR 2 public API. Direct Figma
component inspection found the accessible i1 components binding to core, background, surface,
outline, status-container, alert, palette, spacing, radius, stroke, and type tokens instead.

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
| `WooTheme.colors` | Manual light/dark color exports | See color reconciliation table | See color reconciliation table | Partial projection to `ColorScheme` | production | Grouped public source tokens; not a full Material 3 role mirror. |
| `WooTheme.text` | `Typescale/Android.tokens.json`; `Font theme/Android.tokens.json` | See text table | Same values | Regular projection to `Typography` | needs_android_mapping | Numeric roles and weights are source-backed; font family remains ambiguous. |
| `WooTheme.spacing` | `Value.tokens 2.json` / spacing | `0..64dp` | Same values | No Material role | production | Theme-scoped spacing accessor. |
| `WooTheme.padding` | `Value.tokens 2.json` / padding | `0..64dp` | Same values | No Material role | production | Separate group from spacing even when values match. |

## PR 2 Public Color Surface

`WooTheme.colors` exposes the source-backed color tokens from Figma, the export summary, and approved
manual fallback groups that are used by the core foundation and inspected i1 component nodes. Group
the public API shallowly by source intent; do not collapse the source into a small Material 3-like
subset.

| Public group | Source-backed coverage |
| --- | --- |
| Core | Primary, on-primary, secondary, and on-secondary roles. |
| Background | Section background and section background variant roles, including matching on-colors. |
| Surface | Surface, on-surface tones, inverted surface, and inverted on-surface tones. |
| Outline | `outline` and `outlineVariant`. |
| Status | Top-level status/background container tones and matching on-colors. |
| Overlay | Overlay opacity tokens. Preserve exported alpha values. |
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
- Do not expose the manual `Semantic/*.tokens.json` groups in PR 2 unless a concrete Figma
  component node is confirmed to bind to that token group.
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

## PR3 Component Consumption Notes

PR3 production components consume the existing foundation surface without adding new public token accessors.
Newly promoted fixed visual sizes and component-only shapes remain private component constants unless a later screen
or foundation decision promotes them.

| Component API | Production token consumption | Status | Notes |
| --- | --- | --- | --- |
| `WooTopAppBar` | `WooTheme.colors.surface.default`, `surface.onDefault`, `primary`, `outlineVariant`; `WooTheme.text.bodyLarge.strong`; `WooTheme.spacing.space1`; internal `WooStroke.extraThin` | production | Wraps Material 3 `CenterAlignedTopAppBar` so title, navigation, and action slots are measured instead of using fixed title padding. Matches Figma Top Navigation Bar node `1657:961` for centered title, 64dp bar height, surface background, and bottom divider. Neutral navigation content uses `surface.onDefault`; primary action content uses `primary`; action spacing uses `space1` because action icons already include touch-target padding. The 40dp navigation visual sits inside a 48dp target; neither value is promoted as a token. |
| `WooPageHeader` | `WooTheme.colors.surface.default`, `background.onSection`, `outlineVariant`; `WooTheme.text.headlineSmall.strong`; `WooTheme.padding.padding7`; `WooTheme.spacing.space1`; internal `WooStroke.extraThin` | production_initial | Matches Figma Page Header page node `1168:11407` and component node `1168:11650`: 64dp surface bar, 24dp horizontal title/action inset, optional 40dp right action area capped at 136dp, and bottom divider. |
| `WooPageTitle` | `WooTheme.colors.background.onSection`; `WooTheme.text.headlineSmall.strong` | production_initial | Standalone page-title primitive for page content intros. The title exposes heading semantics and no longer implies the Figma page-header bar composition. |
| `WooBodyText`, `WooLinkedBodyText`, `WooLinkText` | `WooTheme.colors.surface.onVariant`, `surface.onLowest`, `primary`; `WooTheme.text.bodyMedium.regular` and `bodyMedium.emphasized` | production | Body copy uses lower-emphasis source surface text. Link wrappers add underline and link annotations with `primary`; disabled standalone links use `surface.onLowest`. No semantic color carrier is introduced. |
| `WooBadge` | Status container/on-container pairs including `neutralOutlinedContainer` / `onNeutralOutlinedContainer`; `WooTheme.text.bodySmall.regular`; `WooTheme.padding.padding3`; `WooTheme.spacing.space1`; internal `WooStroke.regular`; `MaterialTheme.shapes.medium` / internal `WooRadius.medium` | production_initial | Matches Figma Badge nodes `1164:7609`, `199:19563`, and `199:19562`: 24dp minimum height, `corner-radius/medium`, optional 14dp icon slot, and `bodySmall.regular`. `Caution` maps to warning tokens and `Warning` maps to caution tokens to preserve source naming. Count/badged-box variants are not promoted. |
| `WooPrimaryButton`, `WooSecondaryButton`, `WooTertiaryButton`, `WooButtonSize` | `WooTheme.colors.primary`, `onPrimary`, `secondary`, `onSecondary`, `surface.onLowest`, `outlineVariant`; `WooTheme.text.labelLarge.emphasized`, `labelMedium.emphasized`; `WooTheme.padding.padding5`; `WooTheme.spacing.space3`; internal `WooStroke.medium`; `MaterialTheme.shapes.large` / internal `WooRadius.large` | production_initial | Uses the 12dp large shape projection from Figma Button nodes `1653:5498` / `1653:5502` / `1654:5531`. Primary is filled `primary/onPrimary`, Secondary is filled `secondary/onSecondary`, and Tertiary is outlined with `secondary` and `stroke/weight/medium`. Medium visuals use 48dp with an 18dp leading icon slot; small visuals use 32dp with `labelMedium` and a 14dp leading icon slot. 48dp touch target remains an accessibility rule, not a token. Loading/destructive states are not promoted. |
| `WooCell`, `WooCellContent`, `WooSettingsRow`, `WooSwitchSettingsRow` | `WooTheme.colors.surface.onDefault`, `surface.onVariant`, `surface.onLowest`; `WooTheme.text.titleMedium.emphasized`, `bodyMedium.regular`; `WooTheme.spacing.space1`, `space4`, `space6`; `WooTheme.padding.padding3`, `padding5`, `padding7` | production_initial | Generic `WooCell` uses the recorded generic-cell density: 90dp-ish private minimum height, `padding7` horizontal/vertical padding, and `space6` slot/content gaps. `WooSettingsRow` and `WooSwitchSettingsRow` keep the compact density: 48dp minimum height, `padding5` horizontal, `padding3` vertical, and `space4` slot/content gaps. Enabled title/decorative slots use `surface.onDefault`, descriptions use `surface.onVariant`, and disabled content uses `surface.onLowest` without a row-level alpha. |
| `WooCheckbox`, `WooRadioButton` | `WooTheme.colors.primary`, `onPrimary`, `outline` | production_initial | Controlled Material 3 primitives. Checked/selected emphasis uses `primary`; unchecked boundaries use `outline`; disabled rendering follows Material defaults. Label/group association remains caller-owned. |
| `WooFilterChip` | `WooTheme.colors.surface.onDefault`, `surface.onLowest`, `secondary`, `onSecondary`, `primary`, `outlineVariant`; `WooTheme.text.labelLarge.emphasized`; internal `WooStroke.extraThin` | production_initial | Controlled Material 3 `FilterChip`. Selected container uses `secondary`/`onSecondary`; border uses `outlineVariant` and selected border uses `primary`; disabled label/icon colors use `surface.onLowest`. |
| `WooSectionHeader` | `WooTheme.colors.primary`; `WooTheme.text.labelLarge.emphasized` | production | Section headers expose heading semantics. |
| `WooSwitch` | `WooTheme.colors.primary`, `onPrimary` | production | Pilot-needed Material 3 wrapper, not an inspected i1 Figma component. Only checked emphasis is overridden; Material disabled defaults remain in use. |
| `WooIconButton` | `WooTheme.colors.surface.onDefault`, `primary`, `surface.onLowest` | production | `WooIconButtonEmphasis.Neutral` maps to `surface.onDefault`; `Primary` maps to `primary`; disabled content maps to `surface.onLowest`. The plain variant has no border and keeps the accessible label exposed through button semantics for both the slot and ImageVector overloads. |
| `WooOutlinedIconButton` | `WooTheme.colors.surface.onDefault`, `primary`, `surface.onLowest`, `outlineVariant`; internal `WooStroke.extraThin`; `MaterialTheme.shapes.large` / internal `WooRadius.large` | production | Uses the same `WooIconButtonEmphasis` policy as `WooIconButton`. The visual container follows Figma navigation-button node `1656:5112` as a 40dp visual inside a 48dp target; the accessible label is exposed through button semantics for both the slot and ImageVector overloads. |
| `WooIconContainer` | Palette pairs: `wooPurple.shade0`/`primary`, `sandstone.shade10`/`surface.onHighest`, `wooBlue.shade20`/`wooBlue.shade60`, `wooGreen.shade20`/`wooGreen.shade60`, `wooOrange.shade20`/`wooOrange.shade60`, `wooPink.shade20`/`wooPink.shade60`, `primary`/`onPrimary`; `MaterialTheme.shapes.medium` / internal `WooRadius.medium` | production_initial | Restricted palette tone box matching Figma icon-box node `1447:7243`: 44dp container, `corner-radius/medium`, and 18dp icon. The palette tone enum is intentionally not an open color API. |
| `WooNoticeBanner` | Status container/on-container pairs; `status.neutralOutlinedContainer`, `status.onNeutralOutlinedContainer`, `surface.default`; `WooTheme.text.titleMedium.emphasized`, `bodyMedium.regular`; `WooTheme.padding.padding4`; `WooTheme.spacing.space1`, `space3`; internal `WooStroke.extraThin`; `MaterialTheme.shapes.medium` / internal `WooRadius.medium` | production_initial | Static banner only. Filled tones use status container/content pairs. `NeutralOutlined` uses `surface.default` fill, `neutralOutlinedContainer` border, and `onNeutralOutlinedContainer` content to preserve seven-tone source parity. Actionable/dismissible/live-region behavior is not encoded in PR3. |
| `WooSearchField` | `WooTheme.colors.surface.default`, `surface.onDefault`, `surface.onVariant`, `surface.onLowest`, `primary`, `outlineVariant`; `WooTheme.text.bodyMedium.regular`; `MaterialTheme.shapes.large` / internal `WooRadius.large` | production_initial | Controlled Material 3 `OutlinedTextField`. Focus/cursor use `primary`; border uses `outlineVariant` when not focused; disabled text/icon/placeholder colors use `surface.onLowest`; clear icon size is a private component constant and the clear button follows the field enabled state. |
| `WooTabRow`, `WooTab` | `WooTheme.colors.surface.default`, `surface.onDefault`, `surface.onVariant`, `primary`; `WooTheme.text.labelLarge.emphasized` | needs_android_mapping | Controlled Material 3 top tabs. This is not parity for the Figma bottom Tab Bar source (`919:10678` / `1392:40868` / `1388:40618`), so keep it out of pilot adoption until a source-backed top-tabs design or explicit Store migration need is approved. |
| `WooDivider`, `WooVerticalDivider` | `WooTheme.colors.outlineVariant`; internal `WooStroke.extraThin` | production | Matches Figma Divider node `347:3189`; inset/full/spacer variants do not add production APIs in PR3. |
| `WooLinearProgressIndicator`, `WooCircularProgressIndicator` | `WooTheme.colors.primary`, `secondary` | production | Progress wrappers keep custom loading/progress design replaceable behind the adapter. |

## Token Ownership And XML/View Promotion

The i1 adapter is Compose-first at the API/component layer, and i1 token primitive values start as
Kotlin/Compose-owned implementation details.

When defining tokens:

- Define color, spacing, radius, icon sizing, typography, and similar primitive values in Kotlin/Compose foundation code first.
- Compose APIs should expose stable `WooTheme` and design-system component surfaces, not raw `R.color`
  or `R.dimen` usage to product screens.
- If a non-migrated XML/View screen needs a design-system token, move only that token's primitive
  value to Android resources and update Compose to read from the same resource.
- XML/View styles may consume promoted token resources only through targeted, opt-in style usage.
- Do not copy token values into separate Kotlin/Compose constants and XML resources.
- Keep `token-map.md` as the audit trail for the token value, source shorthand, Material 3 role
  mapping, status, and notes.
- Avoid global XML theme/resource remapping unless a later design-system decision explicitly changes
  the rollout strategy.
- Do not globally apply design-system XML/View styles in PR 2. Add targeted XML/View style usage only
  when a non-migrated XML/View screen needs design-system styling.

## Public Color Source Reconciliation

Every public `WooTheme.colors` field below is source-backed in both modes. Palette and alert rows are
fallback-verified from local manual export JSON; primary export summary mentions those groups but does
not enumerate each row. The manual semantic export is intentionally omitted from the public surface
because inspected Figma component nodes do not bind to it. Palette/ramp and alert tokens are source
tokens only. They require component-specific contrast evidence before foreground text, essential icon,
or required state use.

| Android API | Source path | Export file | Light hex / alpha | Dark hex / alpha | M3 projection | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `WooTheme.colors.primary` | `Primary` | `Light.tokens.json` / `Dark.tokens.json` | `#873EFF` / 100% | `#873EFF` / 100% | `primary` | production | Core primary. |
| `WooTheme.colors.onPrimary` | `On Primary` | `Light.tokens.json` / `Dark.tokens.json` | `#FFFFFF` / 100% | `#FFFFFF` / 100% | `onPrimary` | production | Contrast with primary: 5.04:1 in both modes. |
| `WooTheme.colors.secondary` | `Secondary` | `Light.tokens.json` / `Dark.tokens.json` | `#EAE2FE` / 100% | `#383146` / 100% | `secondary`; internal container alias | production | Contrast with onSecondary: 4.04:1 light, 10.78:1 dark. |
| `WooTheme.colors.onSecondary` | `On Secondary` | `Light.tokens.json` / `Dark.tokens.json` | `#873EFF` / 100% | `#F1EDFE` / 100% | `onSecondary`; internal container alias | production | Source foreground for secondary. |
| `WooTheme.colors.outline` | `Outline/Outline` | `Light.tokens.json` / `Dark.tokens.json` | `#787C82` / 100% | `#454549` / 100% | `outline` | production | Boundary token; do not use as required affordance without component contrast review. |
| `WooTheme.colors.outlineVariant` | `Outline/Outline Variant` | `Light.tokens.json` / `Dark.tokens.json` | `#D2D2D8` / 100% | `#5E5E63` / 100% | `outlineVariant` | production | Subtle boundary token; component-specific contrast required. |
| `WooTheme.colors.background.section` | `Background/Section Background` | `Light.tokens.json` / `Dark.tokens.json` | `#F2F2F8` / 100% | `#101517` / 100% | `background` | production | Contrast with onSection: 14.95:1 light, 18.39:1 dark. |
| `WooTheme.colors.background.onSection` | `Background/On Section Background` | `Light.tokens.json` / `Dark.tokens.json` | `#1E1E1E` / 100% | `#FFFFFF` / 100% | `onBackground` | production | Source foreground for section background. |
| `WooTheme.colors.background.sectionVariant` | `Background/Section Background Variant` | `Light.tokens.json` / `Dark.tokens.json` | `#F0F0F0` / 100% | `#101517` / 100% | `surfaceVariant`; internal surface-container alias | production | Contrast with onSectionVariant: 14.93:1 light, 5.36:1 dark. |
| `WooTheme.colors.background.onSectionVariant` | `Background/On Section Background Variant` | `Light.tokens.json` / `Dark.tokens.json` | `#1C1C1E` / 100% | `#8B8A8E` / 100% | No direct M3 role | production | Source foreground for variant section background. |
| `WooTheme.colors.surface.default` | `Surface/Surface` | `Light.tokens.json` / `Dark.tokens.json` | `#FFFFFF` / 100% | `#232529` / 100% | `surface`; internal surface-container aliases | production | Contrast with onDefault: 16.67:1 light, 15.35:1 dark. |
| `WooTheme.colors.surface.onDefault` | `Surface/On Surface` | `Light.tokens.json` / `Dark.tokens.json` | `#1E1E1E` / 100% | `#FFFFFF` / 100% | `onSurface` | production | Source foreground for default surface. |
| `WooTheme.colors.surface.onVariant` | `Surface/On Surface Variant` | `Light.tokens.json` / `Dark.tokens.json` | `#868A94` / 100% | `#868A94` / 100% | `onSurfaceVariant` | production | Variant foreground; verify pairings per component. |
| `WooTheme.colors.surface.onLowest` | `Surface/On Surface Lowest` | `Light.tokens.json` / `Dark.tokens.json` | `#B2B7C0` / 100% | `#626068` / 100% | No direct M3 role | production | Source surface tone; component-specific contrast required. |
| `WooTheme.colors.surface.onHighest` | `Surface/On Surface Highest` | `Light.tokens.json` / `Dark.tokens.json` | `#50575E` / 100% | `#626068` / 100% | No direct M3 role | production | Source surface tone; component-specific contrast required. |
| `WooTheme.colors.surface.inverted` | `Surface/Inverted Surface` | `Light.tokens.json` / `Dark.tokens.json` | `#1C1C1E` / 100% | `#FFFFFF` / 100% | `inverseSurface` | production | Top-level surface source token. |
| `WooTheme.colors.surface.onInverted` | `Surface/On Inverted Surface` | `Light.tokens.json` / `Dark.tokens.json` | `#FFFFFF` / 100% | `#1E1E1E` / 100% | `inverseOnSurface` | production | Source foreground for inverted surface. |
| `WooTheme.colors.surface.onInvertedVariant` | `Surface/On Inverted Surface Variant` | `Light.tokens.json` / `Dark.tokens.json` | `#929298` / 100% | `#8D8D91` / 100% | No direct M3 role | production | Source inverted variant foreground. |
| `WooTheme.colors.status.errorContainer` | `Error` | `Light.tokens.json` / `Dark.tokens.json` | `#F6E6E3` / 100% | `#F6E6E3` / 90% | `errorContainer` | production | Dark status container alpha is 90%; contrast with onErrorContainer: 13.68:1 before alpha composition. |
| `WooTheme.colors.status.onErrorContainer` | `On Error` | `Light.tokens.json` / `Dark.tokens.json` | `#470000` / 100% | `#470000` / 100% | `onErrorContainer` | production | Top-level container foreground. |
| `WooTheme.colors.status.warningContainer` | `Warning` | `Light.tokens.json` / `Dark.tokens.json` | `#FDE6BE` / 100% | `#FDE6BE` / 90% | No direct M3 role | production | Dark status container alpha is 90%; contrast with onWarningContainer: 13.73:1 before alpha composition. |
| `WooTheme.colors.status.onWarningContainer` | `On Warning` | `Light.tokens.json` / `Dark.tokens.json` | `#2E1900` / 100% | `#2E1900` / 100% | No direct M3 role | production | Top-level container foreground. |
| `WooTheme.colors.status.cautionContainer` | `Caution` | `Light.tokens.json` / `Dark.tokens.json` | `#FEE995` / 100% | `#FEE995` / 90% | No direct M3 role | production | Dark status container alpha is 90%; contrast with onCautionContainer: 13.67:1 before alpha composition. |
| `WooTheme.colors.status.onCautionContainer` | `On Caution` | `Light.tokens.json` / `Dark.tokens.json` | `#281D00` / 100% | `#281D00` / 100% | No direct M3 role | production | Top-level container foreground. |
| `WooTheme.colors.status.successContainer` | `Success` | `Light.tokens.json` / `Dark.tokens.json` | `#C6F7CD` / 100% | `#C6F7CD` / 90% | No direct M3 role | production | Dark status container alpha is 90%; contrast with onSuccessContainer: 13.35:1 before alpha composition. |
| `WooTheme.colors.status.onSuccessContainer` | `On Success` | `Light.tokens.json` / `Dark.tokens.json` | `#002900` / 100% | `#002900` / 100% | No direct M3 role | production | Top-level container foreground. |
| `WooTheme.colors.status.infoContainer` | `Info` | `Light.tokens.json` / `Dark.tokens.json` | `#DEEBFA` / 100% | `#DEEBFA` / 90% | No direct M3 role | production | Dark status container alpha is 90%; contrast with onInfoContainer: 13.68:1 before alpha composition. |
| `WooTheme.colors.status.onInfoContainer` | `On Info` | `Light.tokens.json` / `Dark.tokens.json` | `#001B4F` / 100% | `#001B4F` / 100% | No direct M3 role | production | Top-level container foreground. |
| `WooTheme.colors.status.neutralContainer` | `Neutral` | `Light.tokens.json` / `Dark.tokens.json` | `#F4F4F4` / 100% | `#F4F4F4` / 90% | No direct M3 role | production | Dark status container alpha is 90%; contrast with onNeutralContainer: 15.16:1 before alpha composition. |
| `WooTheme.colors.status.onNeutralContainer` | `On Neutral` | `Light.tokens.json` / `Dark.tokens.json` | `#1E1E1E` / 100% | `#1E1E1E` / 100% | No direct M3 role | production | Top-level container foreground. |
| `WooTheme.colors.status.neutralOutlinedContainer` | `Neutral Outlined` | `Light.tokens.json` / `Dark.tokens.json` | `#DBDBDB` / 100% | `#DBDBDB` / 90% | No direct M3 role | production | Dark pairing with onNeutralOutlinedContainer is not contrast-safe; component-specific contrast is required. |
| `WooTheme.colors.status.onNeutralOutlinedContainer` | `On Neutral Outlined` | `Light.tokens.json` / `Dark.tokens.json` | `#1E1E1E` / 100% | `#DBDBDB` / 100% | No direct M3 role | needs_design | Source-backed, but dark foreground/background contrast is unresolved for text or essential icon use. |
| `WooTheme.colors.overlay.overlay20` | `Overlay/Opacity-20` | `Light.tokens.json` / `Dark.tokens.json` | `#000000` / 20% | `#000000` / 20% | No direct M3 role | production | Preserve source alpha; overlay use only. |
| `WooTheme.colors.overlay.overlay50` | `Overlay/Opacity-50` | `Light.tokens.json` / `Dark.tokens.json` | `#000000` / 50% | `#000000` / 75% | `scrim` | production | Dark alpha is 75%, despite token label. |
| `WooTheme.colors.alert.red` | `Alerts/Red` | `Light.tokens.json` / `Dark.tokens.json` | `#FC4A5B` / 100% | `#DC3545` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.alert.yellow` | `Alerts/Yellow` | `Light.tokens.json` / `Dark.tokens.json` | `#EAAB2D` / 100% | `#EAAB2D` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.alert.green` | `Alerts/Green` | `Light.tokens.json` / `Dark.tokens.json` | `#27AE32` / 100% | `#69B66F` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.alert.blue` | `Alerts/Blue` | `Light.tokens.json` / `Dark.tokens.json` | `#1E94D0` / 100% | `#1E94D0` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.sandstone.shade5` | `Sandstone/Sandstone 5` | `Light.tokens.json` / `Dark.tokens.json` | `#FBF9F6` / 100% | `#FBF9F6` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.sandstone.shade10` | `Sandstone/Sandstone 10` | `Light.tokens.json` / `Dark.tokens.json` | `#F1EEEB` / 100% | `#F1EEEB` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.sandstone.shade20` | `Sandstone/Sandstone 20` | `Light.tokens.json` / `Dark.tokens.json` | `#E6E2DE` / 100% | `#E6E2DE` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.sandstone.shade40` | `Sandstone/Sandstone 40` | `Light.tokens.json` / `Dark.tokens.json` | `#C5C2BF` / 100% | `#C5C2BF` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.sandstone.shade60` | `Sandstone/Sandstone 60` | `Light.tokens.json` / `Dark.tokens.json` | `#8B8A89` / 100% | `#8B8A89` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooBlue.shade20` | `Woo Blue/Woo Blue 20` | `Light.tokens.json` / `Dark.tokens.json` | `#75FFFF` / 100% | `#75FFFF` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooBlue.shade40` | `Woo Blue/Woo Blue 40` | `Light.tokens.json` / `Dark.tokens.json` | `#1AD0FD` / 100% | `#1AD0FD` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooBlue.shade60` | `Woo Blue/Woo Blue 60` | `Light.tokens.json` / `Dark.tokens.json` | `#05096C` / 100% | `#05096C` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooGreen.shade20` | `Woo Green/Woo Green 20` | `Light.tokens.json` / `Dark.tokens.json` | `#D5FF4A` / 100% | `#D5FF4A` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooGreen.shade40` | `Woo Green/Woo Green 40` | `Light.tokens.json` / `Dark.tokens.json` | `#06E782` / 100% | `#06E782` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooGreen.shade60` | `Woo Green/Woo Green 60` | `Light.tokens.json` / `Dark.tokens.json` | `#083D2D` / 100% | `#083D2D` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooOrange.shade20` | `Woo Orange/Woo Orange 20` | `Light.tokens.json` / `Dark.tokens.json` | `#FFE500` / 100% | `#FFE500` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooOrange.shade40` | `Woo Orange/Woo Orange 40` | `Light.tokens.json` / `Dark.tokens.json` | `#FF9000` / 100% | `#FF9000` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooOrange.shade60` | `Woo Orange/Woo Orange 60` | `Light.tokens.json` / `Dark.tokens.json` | `#FF4800` / 100% | `#FF4800` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPink.shade20` | `Woo Pink/Woo Pink 20` | `Light.tokens.json` / `Dark.tokens.json` | `#FCA8FF` / 100% | `#FCA8FF` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPink.shade40` | `Woo Pink/Woo Pink 40` | `Light.tokens.json` / `Dark.tokens.json` | `#FF45E3` / 100% | `#FF45E3` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPink.shade60` | `Woo Pink/Woo Pink 60` | `Light.tokens.json` / `Dark.tokens.json` | `#4E0061` / 100% | `#4E0061` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPurple.shade0` | `Woo Puprle/Woo Purple 0` | `Light.tokens.json` / `Dark.tokens.json` | `#F2EDFF` / 100% | `#F2EDFF` / 100% | No direct M3 role | production | Source typo stays in docs only; Android API uses `wooPurple`. |
| `WooTheme.colors.palette.wooPurple.shade5` | `Woo Puprle/Woo Purple 5` | `Light.tokens.json` / `Dark.tokens.json` | `#E1D7FF` / 100% | `#E1D7FF` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPurple.shade10` | `Woo Puprle/Woo Purple 10` | `Light.tokens.json` / `Dark.tokens.json` | `#D1C1FF` / 100% | `#D1C1FF` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPurple.shade20` | `Woo Puprle/Woo Purple 20` | `Light.tokens.json` / `Dark.tokens.json` | `#B999FF` / 100% | `#B999FF` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPurple.shade30` | `Woo Puprle/Woo Purple 30` | `Light.tokens.json` / `Dark.tokens.json` | `#A77EFF` / 100% | `#A77EFF` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPurple.shade40` | `Woo Puprle/Woo Purple 40` | `Light.tokens.json` / `Dark.tokens.json` | `#873EFF` / 100% | `#873EFF` / 100% | No direct M3 role | production | Same hex as top-level primary. |
| `WooTheme.colors.palette.wooPurple.shade50` | `Woo Puprle/Woo Purple 50` | `Light.tokens.json` / `Dark.tokens.json` | `#720EEC` / 100% | `#720EEC` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPurple.shade60` | `Woo Puprle/Woo Purple 60` | `Light.tokens.json` / `Dark.tokens.json` | `#6108CE` / 100% | `#6108CE` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPurple.shade70` | `Woo Puprle/Woo Purple 70` | `Light.tokens.json` / `Dark.tokens.json` | `#5007AA` / 100% | `#5007AA` / 100% | No direct M3 role | production | Source palette token. |
| `WooTheme.colors.palette.wooPurple.shade80` | `Woo Puprle/Woo Purple 80` | `Light.tokens.json` / `Dark.tokens.json` | `#3C087E` / 100% | `#3C087E` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPurple.shade90` | `Woo Puprle/Woo Purple 90` | `Light.tokens.json` / `Dark.tokens.json` | `#2C045D` / 100% | `#2C045D` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |
| `WooTheme.colors.palette.wooPurple.shade100` | `Woo Puprle/Woo Purple 100` | `Light.tokens.json` / `Dark.tokens.json` | `#1F0342` / 100% | `#1F0342` / 100% | No direct M3 role | production | Fallback-verified from local manual export JSON; component-specific contrast required. |

## Material 3 Color Projection

`WooDesignSystemTheme` builds a complete `ColorScheme(...)` against Material 3 `1.4.0`. Direct rows
reuse public source fields. Alias rows are internal projection decisions only and are not additional
public `WooTheme.colors` fields.

| Material role | Projection source | Status | Notes |
| --- | --- | --- | --- |
| `primary` | `WooTheme.colors.primary` | production | Direct source-backed projection. |
| `onPrimary` | `WooTheme.colors.onPrimary` | production | Direct source-backed projection. |
| `primaryContainer` | `WooTheme.colors.secondary` | production | Internal alias; no distinct source token. |
| `onPrimaryContainer` | `WooTheme.colors.onSecondary` | production | Internal alias; no distinct source token. |
| `inversePrimary` | `WooTheme.colors.primary` | production | Internal alias; no distinct source token. |
| `secondary` | `WooTheme.colors.secondary` | production | Direct source-backed projection. |
| `onSecondary` | `WooTheme.colors.onSecondary` | production | Direct source-backed projection. |
| `secondaryContainer` | `WooTheme.colors.secondary` | production | Internal alias; no distinct source token. |
| `onSecondaryContainer` | `WooTheme.colors.onSecondary` | production | Internal alias; no distinct source token. |
| `tertiary` | `WooTheme.colors.secondary` | production | Internal alias until a distinct tertiary source appears. |
| `onTertiary` | `WooTheme.colors.onSecondary` | production | Internal alias until a distinct tertiary source appears. |
| `tertiaryContainer` | `WooTheme.colors.secondary` | production | Internal alias until a distinct tertiary container source appears. |
| `onTertiaryContainer` | `WooTheme.colors.onSecondary` | production | Internal alias until a distinct tertiary container source appears. |
| `background` | `WooTheme.colors.background.section` | production | Direct source-backed projection. |
| `onBackground` | `WooTheme.colors.background.onSection` | production | Direct source-backed projection. |
| `surface` | `WooTheme.colors.surface.default` | production | Direct source-backed projection. |
| `onSurface` | `WooTheme.colors.surface.onDefault` | production | Direct source-backed projection. |
| `surfaceVariant` | `WooTheme.colors.background.sectionVariant` | production | Source-backed projection, not a public Material mirror. |
| `onSurfaceVariant` | `WooTheme.colors.surface.onVariant` | production | Direct source-backed projection. |
| `surfaceTint` | `WooTheme.colors.primary` | production | Internal alias; no distinct source token. |
| `inverseSurface` | `WooTheme.colors.surface.inverted` | production | Direct source-backed projection. |
| `inverseOnSurface` | `WooTheme.colors.surface.onInverted` | production | Direct source-backed projection. |
| `error` | `WooTheme.colors.status.errorContainer` | production | Internal alias until a distinct error source token is approved for controls. |
| `onError` | `WooTheme.colors.status.onErrorContainer` | production | Internal alias paired with `error`. |
| `errorContainer` | `WooTheme.colors.status.errorContainer` | production | Direct source-backed projection. |
| `onErrorContainer` | `WooTheme.colors.status.onErrorContainer` | production | Direct source-backed projection. |
| `outline` | `WooTheme.colors.outline` | production | Direct source-backed projection. |
| `outlineVariant` | `WooTheme.colors.outlineVariant` | production | Direct source-backed projection. |
| `scrim` | `WooTheme.colors.overlay.overlay50` | production | Source-backed projection; dark alpha is 75%. |
| `surfaceBright` | `WooTheme.colors.surface.default` | production | Internal alias; no distinct source token. |
| `surfaceDim` | `WooTheme.colors.background.section` | production | Internal alias; no distinct source token. |
| `surfaceContainer` | `WooTheme.colors.background.section` | production | Internal alias; no distinct source token. |
| `surfaceContainerHigh` | `WooTheme.colors.surface.default` | production | Internal alias; no distinct source token. |
| `surfaceContainerHighest` | `WooTheme.colors.surface.default` | production | Internal alias; no distinct source token. |
| `surfaceContainerLow` | `WooTheme.colors.background.sectionVariant` | production | Internal alias; no distinct source token. |
| `surfaceContainerLowest` | `WooTheme.colors.surface.default` | production | Internal alias; no distinct source token. |
| `primaryFixed` | `WooTheme.colors.primary` | production | Internal alias; no distinct source token. |
| `primaryFixedDim` | `WooTheme.colors.secondary` | production | Internal alias; no distinct source token. |
| `onPrimaryFixed` | `WooTheme.colors.onPrimary` | production | Internal alias; no distinct source token. |
| `onPrimaryFixedVariant` | `WooTheme.colors.onSecondary` | production | Internal alias; no distinct source token. |
| `secondaryFixed` | `WooTheme.colors.secondary` | production | Internal alias; no distinct source token. |
| `secondaryFixedDim` | `WooTheme.colors.secondary` | production | Internal alias; no distinct source token. |
| `onSecondaryFixed` | `WooTheme.colors.onSecondary` | production | Internal alias; no distinct source token. |
| `onSecondaryFixedVariant` | `WooTheme.colors.onSecondary` | production | Internal alias; no distinct source token. |
| `tertiaryFixed` | `WooTheme.colors.secondary` | production | Internal alias until a distinct tertiary fixed source appears. |
| `tertiaryFixedDim` | `WooTheme.colors.secondary` | production | Internal alias until a distinct tertiary fixed source appears. |
| `onTertiaryFixed` | `WooTheme.colors.onSecondary` | production | Internal alias until a distinct tertiary fixed source appears. |
| `onTertiaryFixedVariant` | `WooTheme.colors.onSecondary` | production | Internal alias until a distinct tertiary fixed source appears. |

## Public Text Tokens

`WooTheme.text` exposes all 15 Android type roles with `regular`, `emphasized`, and `strong`
variants. The regular variant is projected to `MaterialTheme.typography`. Android numeric role
values are source-ready, but font family remains unresolved because the Android type export reports
one family while the Android font-theme export reports another.

PR3 production components consume `headlineSmall`, `bodyLarge`, `titleMedium`, `bodyMedium`, `labelLarge`, and
`labelMedium` roles through wrappers. This does not promote global typography status beyond
`needs_android_mapping`; it records that these numeric roles are production-consumed while the
font-family decision remains open.

| Android API | Size | Line height | Tracking | Weights | Material projection | Status | Notes |
| --- | ---: | ---: | ---: | --- | --- | --- | --- |
| `WooTheme.text.displayLarge` | `56sp` | `64sp` | `-0.41sp` | regular / medium / bold | `displayLarge.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.displayMedium` | `48sp` | `52sp` | `-0.41sp` | regular / medium / bold | `displayMedium.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.displaySmall` | `36sp` | `44sp` | `-0.41sp` | regular / medium / bold | `displaySmall.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.headlineLarge` | `34sp` | `40sp` | `-1.40sp` | regular / medium / bold | `headlineLarge.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.headlineMedium` | `28sp` | `36sp` | `-0.41sp` | regular / medium / bold | `headlineMedium.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.headlineSmall` | `24sp` | `32sp` | `-0.75sp` | regular / medium / bold | `headlineSmall.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.titleLarge` | `20sp` | `28sp` | `-0.41sp` | regular / medium / bold | `titleLarge.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.titleMedium` | `17sp` | `20sp` | `-0.41sp` | regular / medium / bold | `titleMedium.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.titleSmall` | `14sp` | `16sp` | `-0.41sp` | regular / medium / bold | `titleSmall.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.bodyLarge` | `17sp` | `24sp` | `-0.41sp` | regular / medium / bold | `bodyLarge.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.bodyMedium` | `14sp` | `20sp` | `-0.41sp` | regular / medium / bold | `bodyMedium.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.bodySmall` | `13sp` | `16sp` | `-0.41sp` | regular / medium / bold | `bodySmall.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.labelLarge` | `16sp` | `24sp` | `-0.41sp` | regular / medium / bold | `labelLarge.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.labelMedium` | `14sp` | `20sp` | `-0.41sp` | regular / medium / bold | `labelMedium.regular` | needs_android_mapping | Font family gate remains open. |
| `WooTheme.text.labelSmall` | `10sp` | `14sp` | `-0.07sp` | regular / medium / bold | `labelSmall.regular` | needs_android_mapping | Font family gate remains open. |

## Spacing And Padding

Spacing and padding use the same i1 primitive scale today, but remain separate public groups because
they encode different design intent.

| Android API | Source path | Value | Material projection | Status | Notes |
| --- | --- | ---: | --- | --- | --- |
| `WooTheme.spacing.space0` | `Spacing/0` | `0dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space1` | `Spacing/1` | `2dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space2` | `Spacing/2` | `4dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space3` | `Spacing/3` | `8dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space4` | `Spacing/4` | `12dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space5` | `Spacing/5` | `16dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space6` | `Spacing/6` | `20dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space7` | `Spacing/7` | `24dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space8` | `Spacing/8` | `32dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space9` | `Spacing/9` | `40dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space10` | `Spacing/10` | `48dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space11` | `Spacing/11` | `56dp` | No Material role | production | Source-ready. |
| `WooTheme.spacing.space12` | `Spacing/12` | `64dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding0` | `Padding/0` | `0dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding1` | `Padding/1` | `2dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding2` | `Padding/2` | `4dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding3` | `Padding/3` | `8dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding4` | `Padding/4` | `12dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding5` | `Padding/5` | `16dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding6` | `Padding/6` | `20dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding7` | `Padding/7` | `24dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding8` | `Padding/8` | `32dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding9` | `Padding/9` | `40dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding10` | `Padding/10` | `48dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding11` | `Padding/11` | `56dp` | No Material role | production | Source-ready. |
| `WooTheme.padding.padding12` | `Padding/12` | `64dp` | No Material role | production | Source-ready. |

## Internal Radius And Stroke

Radius and stroke are source-backed but remain internal in PR2. Do not add public radius, stroke,
elevation, icon-sizing, or state accessors on the Store design-system theme namespace.

| Internal token | Source path | Value | Material projection | Status | Notes |
| --- | --- | ---: | --- | --- | --- |
| `WooRadius.none` | `Radius/None` | `0dp` | No Material role | preview_only | Internal only. |
| `WooRadius.extraSmall` | `Radius/Extra-small` | `2dp` | `MaterialTheme.shapes.extraSmall` | production | Internal projection. |
| `WooRadius.small` | `Radius/Small` | `4dp` | `MaterialTheme.shapes.small` | production | Internal projection. |
| `WooRadius.medium` | `Radius/Medium` | `8dp` | `MaterialTheme.shapes.medium` | production | Internal projection; production-consumed by `WooBadge`, `WooIconContainer`, and `WooNoticeBanner`. |
| `WooRadius.large` | `Radius/Large` | `12dp` | `MaterialTheme.shapes.large` | production | Internal projection; production-consumed by the button family, `WooTopAppBar` navigation affordance, `WooOutlinedIconButton`, and `WooSearchField`. |
| `WooRadius.extraLarge` | `Radius/Extra large` | `16dp` | `MaterialTheme.shapes.extraLarge` | production | Internal projection. |
| `WooRadius.full` | `Radius/Full` | `999dp` | No Material role | preview_only | Pill/full-radius sentinel. |
| `WooStroke.none` | `Stroke/None` | `0dp` | No Material role | preview_only | Internal only. |
| `WooStroke.extraThin` | `Stroke/Extra-Thin` | `0.5dp` | No Material role | production | Internal only; production-consumed by `WooFilterChip`, `WooDivider`, `WooVerticalDivider`, `WooTopAppBar`, `WooPageHeader`, `WooOutlinedIconButton`, and `WooNoticeBanner` `NeutralOutlined`. |
| `WooStroke.thin` | `Stroke/Thin` | `0.75dp` | No Material role | preview_only | Internal only. |
| `WooStroke.regular` | `Stroke/Regular` | `1dp` | No Material role | production | Internal only; production-consumed by `WooBadge` `NeutralOutlined`. |
| `WooStroke.medium` | `Stroke/Medium` | `1.5dp` | No Material role | production | Internal only; production-consumed by `WooTertiaryButton`. |
| `WooStroke.mediumIncreased` | `Stroke/Medium Increased` | `2dp` | No Material role | preview_only | Internal only. |
| `WooStroke.thick` | `Stroke/Thick` | `3dp` | No Material role | preview_only | Internal only. |
| `WooStroke.extraThick` | `Stroke/Extra Thick` | `4dp` | No Material role | preview_only | Internal only. |

## Unresolved Groups

| Group | Source status | Android status | Public API | Notes |
| --- | --- | --- | --- | --- |
| Icon sizing | `Value.tokens 3.json` generic `Size` scale exists | needs_android_mapping | None | Probable icon-size source only; generic source label is not accepted for public API. |
| Elevation | No export found | needs_design | None | Do not invent shadow, z-depth, or elevation values. |
| Interaction state alpha | Pressed/focused/overlay related colors exist | needs_design | None | Existing colors are not disabled, hover, focus, dragged, or pressed state-layer alpha primitives. |
| Minimum touch target | No export found | needs_design | None | PR3 components enforce `48dp` where needed as an accessibility rule, not a source-backed design-system token. |
