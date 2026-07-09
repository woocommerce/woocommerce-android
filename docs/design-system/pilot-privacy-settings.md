# Pilot: Privacy Settings

Historical note: this doc records an existing-Compose validation pattern. It does not define current
rollout scope; see [rollout-direction.md](rollout-direction.md).

`PrivacySettingsFragment` was the existing-Compose pilot for Store Management App design-system integration.

The goal is to validate Compose Design System Adoption: an existing Store Compose screen adopts
design-system components and foundations without changing its hosting model or product behavior.

## Target Files

- `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/prefs/PrivacySettingsFragment.kt`
- `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/prefs/PrivacySettingsScreen.kt`
- `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/prefs/PrivacySettingsViewModel.kt`

## Why This Screen

- It is already Compose, so it tests design-system adoption without XML/View layout migration.
- It is a visible Store app settings surface.
- It is not a product, order, payment, fulfillment, or POS workflow.
- It has useful design-system coverage: page text, cells/rows, switches, icon
  buttons, dividers, progress indicator, scrolling, and snackbar-triggering behavior.
- Existing preview coverage includes light, dark, RTL, and smaller-screen variants.

## Behavior To Preserve

- The Fragment remains the host.
- XML nav graph and existing navigation remain.
- `PrivacySettingsViewModel` remains the state and event owner.
- Web option and usage tracker links still open via custom tabs.
- Policies navigation still uses the existing SafeArgs direction.
- Snackbar behavior remains in the Fragment through `UIMessageResolver`.
- `AnalyticsTracker.trackViewShown(this)` still runs on resume.
- Existing string resources remain unless a product copy decision changes them.

## Design-System Coverage Pattern

The pattern consumes production-ready design-system APIs for:

- `WooTheme.*` foundations under `WooDesignSystemThemeWithBackground`.
- Page title/body typography.
- Cell or settings row.
- Switch.
- Icon button.
- Divider.
- Spacing/padding tokens.
- Radius/color tokens required by those components.
- Progress indicator as a thin Material 3 wrapper, even though it is not listed as an i1 Figma component.

If a required component is not production-ready yet, either implement it first or document why the
pattern temporarily uses legacy/current Compose.

## Preview Coverage

Preserve or improve the current previews:

- Design-system foundation in light and dark mode.
- RTL mode.
- Large font scale.
- Smaller screen.

## Verification

Before reusing this as an existing-Compose adoption pattern:

- Verify no accessibility regression from the current Compose screen.
- Verify analytics are unchanged.
- Verify switch interactions still call the existing ViewModel handlers.
- Verify web option and usage tracker links.
- Verify policies navigation.
- Verify snackbar behavior if touched.
- Verify the same screen implementation renders under the selected design-system root.
- Verify DS components do not fall back to hardcoded light defaults.
- Review light and dark previews/screenshots.
- Confirm no POS APIs or patterns were introduced.

## Findings After Implementation

Add findings here only when this pattern reveals something worth preserving for future migrations,
such as a component gap, token mismatch, verification issue, or playbook change. Do not add trivial
implementation notes.
