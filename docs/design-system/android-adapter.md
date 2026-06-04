# Store Design System Android Adapter

This document defines how the Store Management App adopts the Woo Mobile Design System i1 with the least disruption.

## Scope

- Store Management App only.
- POS is out of scope.
- The adapter lives in the existing Store app UI/theme/resource layer for i1.
- The adapter is opt-in. Existing screens keep their current behavior and styling until deliberately migrated.
- Figma is the design-intent source of truth. Android owns the runtime API contract.
- Source references use public-repo shorthands: P2 `pe5sF9-5ox-p2`, Figma `50XIH5MmOf4xUYEkM6fAm6-fi`. Do not expand them into raw URLs in public repo docs.

## Package

New Compose APIs live under:

```text
com.woocommerce.android.ui.compose.designsystem
```

Suggested subpackages:

- `foundation`: theme, color, typography, spacing, shape, elevation, and token helpers.
- `component`: production-ready Woo Mobile Design System components.
- `preview`: preview wrappers, sample data, and catalog-only helpers.

## Public API Rules

- Use the `Woo` prefix for new design-system components inside the design-system package.
- Do not use `WooDs*` for new APIs.
- Do not use `WC*` for new design-system APIs. `WC*` remains the legacy/shared component namespace.
- Do not expose raw Figma variable names as public Android API.
- Public APIs should expose only production-ready tokens and components.
- In-progress i1 areas may be documented, tracked, or preview-only until signed off.
- Preview-only components should not be exposed as reusable product-screen APIs.
- Keep preview-only implementations private/internal to catalog or preview files under `designsystem.preview`.

## Theme Strategy

Add a separate `WooDesignSystemTheme` for design-system screens and previews.

- The theme is Material 3-only.
- Existing Material 2 usage can remain until touched.
- `composeView` should accept an explicit theme selector and default to the current legacy app theme.
- Migrated design-system screens opt in at the Fragment Compose root.
- Do not globally remap existing `Woo*`, `WC*`, XML styles, colors, typography, or app theme resources for i1.

Expected hosting shape:

```kotlin
composeView(theme = ComposeTheme.DesignSystem) {
    FeedbackCompletedScreen(...)
}
```

The exact enum/type name can be refined during implementation.

## Token Strategy

i1 uses manual Android runtime token definitions first.

- Keep Android token names stable and screen-facing.
- Use Material 3 role names where the design maps cleanly.
- Add Woo-specific semantic tokens only when Material 3 roles do not express the design intent.
- Keep Figma variables with no clean Material 3 role internal first, and expose them publicly only when production components or pilot screens need them.
- Keep Figma variable names or IDs only in documentation and internal mapping metadata.
- Structure token definitions so a future Figma generation pipeline can update adapter internals without changing screen APIs.
- Every token that reaches production APIs must be represented in `docs/design-system/token-map.md`.
- Keep i1 token primitive values Kotlin/Compose-owned at first.
- Do not create Android XML resources for design-system tokens until a real XML/View use case needs them.
- When XML/View needs a token, move that token's primitive value to Android resources and update Compose to read from the same resource.
- Do not keep parallel Kotlin/Compose and XML resource definitions for the same token primitive value.

## Component Strategy

- Implement the i1 catalog as Compose-first components with previews.
- Wrap Material 3 components where the design intent maps cleanly.
- Build custom components only when Material 3 behavior, shape, state, or layout is materially different.
- Do not migrate a screen only to consume a component. Screen migration is deliberate and candidate-based.
- Production screens may import only production-ready components.
- Preview-only components can be shown in the catalog, but must not expose public product-screen APIs for migration agents to consume.

## Pilot Strategy

Use two initial pilots so the adapter validates both likely adoption outcomes:

- `PrivacySettingsFragment`: existing Compose screen adopting the design-system theme, tokens, and components.
- `FeedbackCompletedFragment`: XML/View layout to Fragment-hosted Compose layout migration.

Run `PrivacySettingsFragment` first, then `FeedbackCompletedFragment`, after foundations/components/previews and the `composeView` theme selector are available. Existing Compose adoption is the lower-risk canary for the design-system APIs; the XML/View pilot then validates the larger migration workflow.

Both pilots must preserve existing product behavior, navigation, analytics, strings, and Store app architecture boundaries.

## Delivery Sequence

Follow `docs/design-system/implementation-plan.md`.

The intended sequence is docs first, then foundations/theme, then components/previews, then `composeView` theme selection, then `PrivacySettingsFragment`, then `FeedbackCompletedFragment`, then playbook updates from the pilots.

## View/XML Compatibility

Fragment-hosted Compose layout migration is preferred for new and substantially redesigned Store screens, but it is optional.

Some screens should remain XML/View because migration would require substantial work. Those screens can still receive targeted token/style updates if doing so is low-risk and does not force a global theme replacement.

When XML/View screens need design-system styling, promote only the required token primitive values to Android resources and update Compose to read those same resources.

Do not globally apply design-system XML/View styles in i1 foundation work. Add targeted XML/View style usage only when a non-migrated XML/View screen needs design-system styling.
