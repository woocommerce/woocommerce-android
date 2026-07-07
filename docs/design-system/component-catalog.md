# Store Design System Component Catalog

This catalog tracks Woo Mobile Design System i1 components for Android.

The goal is to implement the i1 foundation and component catalog with previews, while only allowing
production-ready APIs into migrated Store screens.

Production design-system components live in the Store-only `:libs:store-design-system` module under
`com.woocommerce.android.ui.compose.designsystem.*`.

The component PR should provide visual coverage for the full i1 catalog, but it does not need to mark
every component as production-ready. Components needed by the first-wave tab and top-detail surfaces
plus low-risk primitives can become production APIs first; unsettled components should remain
private/internal preview catalog implementations. Rollout scope is defined in
[rollout-direction.md](rollout-direction.md).

## Initial Production Subset

Before first-wave screen migration begins, production-ready APIs should exist for:

- Top/navigation bar.
- Page title/body/link text styles or wrappers.
- Primary button.
- Settings cell/row.
- Section header.
- Switch.
- Icon button.
- Divider.
- Progress indicator.
- Spacing, radius, color, and typography tokens used by those components.

Production components should read approved foundation values through `WooTheme.*`. `MaterialTheme` remains available
inside wrappers when Material 3 components or defaults require interop projection values.

Production components must render correctly under the design-system root and must not rely on static
hardcoded light fallback defaults. Non-migrated screens stay on the legacy root and should not
consume production design-system components until explicitly migrated.

Do not add additional thin wrappers beyond this subset unless a later design-system decision
explicitly expands the catalog. This prevents the adapter from becoming an unlimited Material 3
wrapper library.

## Preview Standard

Use `androidx.compose.ui.tooling.preview.PreviewLightDark` for design-system component previews.

Older Store Compose screens may use the project `LightDarkThemePreviews` annotation. New
design-system foundations, components, preview catalog entries, and first-wave screen updates should
use `@PreviewLightDark`.

Design-system component previews should wrap content in `WooDesignSystemTheme`, not the legacy Store
theme root. Migrated screen previews should cover the design-system root in light and dark mode.

Preview coverage is required for every component. Screenshot verification is required for first-wave
screens and high-risk components, but not for every small primitive component.

## Status Values

- `production`: Stable API, visual states, accessibility behavior, and previews.
- `preview_only`: Implemented for catalog or design review, but not exposed as a reusable production-screen API.
- `needs_design`: Design intent or required states are not signed off.
- `needs_android_mapping`: Android API or Material 3 mapping is not resolved.

## Catalog

| Component | Android API | Strategy | Status | Required previews | Notes |
| --- | --- | --- | --- | --- | --- |
| Badges | TBD | Material 3 wrapper or custom after review | needs_android_mapping | Light/dark, states | i1 component. |
| Buttons | TBD | Material 3 wrapper first | needs_android_mapping | Light/dark, enabled/disabled/loading if supported | First-wave screens need primary actions. |
| Cell | TBD | Material 3 wrapper or custom after review | needs_android_mapping | Light/dark, leading/trailing content | Common migration component. |
| Cell Content | TBD | Custom composition if needed | needs_android_mapping | Light/dark, text density variants | Pair with Cell. |
| Check Box | TBD | Material 3 wrapper first | needs_android_mapping | Light/dark, checked/unchecked/disabled | Form candidate. |
| Chip | TBD | Material 3 wrapper first | needs_android_mapping | Light/dark, selected/unselected/disabled | Filter/search candidate. |
| Divider | TBD | Material 3 wrapper first | needs_android_mapping | Light/dark | Low-risk early component. |
| Icons | TBD | Existing icon assets plus token mapping | needs_android_mapping | Light/dark, sizing | Avoid new naming until icon policy is clear. |
| Navigation Bar | TBD | Material 3 wrapper or app-specific custom | needs_android_mapping | Light/dark, navigation/action/search/collapse states | Unified visual spec across Compose `WooTopAppBar` and DS-looking XML toolbar. |
| Page Header | TBD | Custom composition if needed | needs_android_mapping | Light/dark, title/subtitle variants | Useful for screen migrations. |
| Radio Button | TBD | Material 3 wrapper first | needs_android_mapping | Light/dark, selected/unselected/disabled | Form candidate. |
| Search | TBD | Material 3 wrapper or custom after review | needs_android_mapping | Light/dark, focused/unfocused/query states | Existing search components may inform behavior only. |
| Segment Control | TBD | Preview-only until design signed off | preview_only | Light/dark, selected states | i1 marks this in progress. |
| Sheets | TBD | Material 3 wrapper first | needs_android_mapping | Light/dark, content variants | Migration should preserve navigation/event ownership. |
| Tab Bar | TBD | Material 3 wrapper first | needs_android_mapping | Light/dark, selected/unselected | Existing `WCPrimaryTabRow` remains legacy. |
| Table | TBD | Custom after review | needs_android_mapping | Light/dark, row density variants | Likely high migration cost on real screens. |
| Progress Indicator | TBD | Thin Material 3 wrapper | needs_android_mapping | Light/dark, indeterminate/determinate if supported | Not listed as an i1 Figma component. Candidate wrapper for the component PR. |

## Production Checklist

Before a component is marked `production`:

- It uses `Woo` naming inside `com.woocommerce.android.ui.compose.designsystem`.
- It is wrapped by `WooDesignSystemTheme` in design-system previews.
- It reads production foundation values through `WooTheme.*`, except where a Material 3 API requires `MaterialTheme`
  interop values.
- It has light and dark previews through `@PreviewLightDark`.
- Required states are covered in previews.
- Screenshot verification is completed if the component is high-risk.
- Accessibility is reviewed: semantic role where applicable, label/contentDescription rules, disabled
  state behavior, minimum touch target, and font-scale resilience.
- Its tokens are mapped in `docs/design-system/token-map.md`.
- It does not depend on POS APIs or POS design-system concepts.

## Preview-Only Boundary

Preview-only components may exist only as private/internal catalog or preview implementations, preferably under `designsystem.preview`.

Do not expose preview-only components as reusable public APIs. This keeps product-screen migrations
and AI agents from importing components that are not signed off.
