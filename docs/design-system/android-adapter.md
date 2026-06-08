# Store Design System Android Adapter

This document defines how the Store Management App adopts the Woo Mobile Design System i1 with the least disruption.

## Scope

- Store Management App only.
- POS is out of scope.
- The adapter lives in the existing Store app UI/theme/resource layer for i1.
- Design-system components are adopted deliberately. Existing screens keep their current behavior and
  styling until migrated; the feature flag controls the default Compose root for screens using the
  shared `composeView` helper.
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
- Use `WooDesignSystemTheme` for the opt-in theme wrapper.
- Treat `WooDesignSystemTheme` as the migration-era wrapper name while the legacy
  `com.woocommerce.android.ui.compose.theme.WooTheme` wrapper still exists. Do not use temporal names
  such as `WooNewTheme`; future consolidation can merge wrapper/accessor naming after the legacy
  wrapper is removed.
- Use `WooTheme` as the design-system foundation accessor object for theme-scoped values such as
  colors, typography, spacing, and padding.
- The new `WooTheme` accessor lives under `com.woocommerce.android.ui.compose.designsystem`.
  The existing `com.woocommerce.android.ui.compose.theme.WooTheme` composable remains the legacy Store
  wrapper until deliberately removed; new design-system code should not import it.
- Do not expose raw Figma variable names as public Android API.
- Public APIs should expose only production-ready tokens and components.
- In-progress i1 areas may be documented, tracked, or preview-only until signed off.
- Preview-only components should not be exposed as reusable product-screen APIs.
- Keep preview-only implementations private/internal to catalog or preview files under `designsystem.preview`.

## Theme Root Strategy

Both Store Compose theme roots must satisfy the design-system foundation contract.

- `ComposeTheme.LEGACY` uses `WooThemeWithBackground`.
- `ComposeTheme.DESIGN_SYSTEM` uses `WooDesignSystemThemeWithBackground`.
- Default/no explicit `composeView` theme follows `FeatureFlag.NEW_DESIGN_SYSTEM`.
- Explicit `ComposeTheme.DESIGN_SYSTEM` still forces the real design-system foundation.
- Explicit `ComposeTheme.LEGACY` still forces the legacy-compatible foundation.
- Do not replace `WooThemeWithBackground` with `WooDesignSystemThemeWithBackground` while the flag is
  off.
- Existing Material 2 usage can remain until touched.
- Do not globally remap existing `Woo*`, `WC*`, XML styles, colors, typography, or app theme
  resources for i1.

`WooDesignSystemThemeWithBackground` provides the real design-system foundation. `WooThemeWithBackground`
continues to provide the legacy app look and must also provide design-system composition locals using
a legacy-compatible foundation.

Expected migrated-screen hosting shape:

```kotlin
composeView {
    PrivacySettingsScreen(...)
}
```

Default hosting lets the feature flag choose the root wrapper. Ordinary migrated screens should use
that default. Use explicit theme selection only when a screen, preview, or test intentionally needs to
force a foundation:

```kotlin
composeView(theme = ComposeTheme.DESIGN_SYSTEM) {
    FeedbackCompletedScreen(...)
}
```

## Rollout Strategy

Migrate screens once to design-system components. Do not keep duplicate `LegacyScreen` and
`DesignSystemScreen` implementations for ordinary migrations.

- Screen code should not branch between legacy and design-system UI trees.
- The active theme root/foundation controls the visual rollout.
- Flag off: the single design-system component tree renders under `WooThemeWithBackground` with a
  legacy-compatible design-system foundation.
- Flag on: the same tree renders under `WooDesignSystemThemeWithBackground` with the real
  design-system foundation.
- Temporary full-screen fallbacks are allowed only for genuinely high-risk migrations and must have
  an explicit expiry/removal plan.
- Do not add broad component variants unless there is a clear bridge need.

No design-system component should depend on hardcoded light fallback defaults. Rendering a
design-system component under `WooThemeWithBackground` must receive valid `WooTheme.colors`,
`WooTheme.text`, `WooTheme.spacing`, and `WooTheme.padding` values.

## Legacy-Compatible Foundation

`WooThemeWithBackground` should provide design-system locals using values mapped from the existing
legacy app theme.

- Source legacy-compatible colors from existing Android color resources, including the resources used
  by `com.woocommerce.android.ui.compose.theme.WooColors`, so light/dark behavior remains
  resource-driven.
- Map `WooTheme.colors.background.section` to the current window/background concept.
- Map `WooTheme.colors.surface.default` to the current surface/card/row background concept.
- Map surface `on*` roles to existing on-surface text colors.
- Map `outline` and `outlineVariant` to existing divider/outline resources.
- Map `primary` and `onPrimary` to current primary resources.
- Provide legacy-compatible shapes/radius.
- Provide spacing and padding tokens, either shared with the design-system foundation or explicitly
  mapped if needed.
- Map typography when practical. Exact line wrapping is not part of the compatibility guarantee.

The compatibility promise is broad visual closeness, not pixel-perfect legacy rendering. Density and
layout are not guaranteed unless the component itself supports them.

## Bridge Component Compatibility

Top app bar/chrome migration is not just a token change. Moving from the Activity toolbar to Compose
`WooTopAppBar` changes chrome ownership and structure.

- Under the legacy-compatible foundation, `WooTopAppBar` should look and behave close to the existing
  Activity toolbar.
- Under the design-system foundation, `WooTopAppBar` should render the real design-system app bar.
- Consider title alignment, title typography, navigation icon treatment, action colors,
  divider/elevation, height, and insets.
- Prefer component-level compatibility driven by the active foundation instead of screen-level
  duplicate implementations.

## Token Strategy

i1 uses manual Kotlin/Compose runtime token definitions first.

- Use `docs/design-system/material3-reference.md` for official Material 3 role semantics, Compose
  API pointers, and default scale references while mapping i1 foundations.
- Keep adapter token names stable and screen-facing.
- Use source-backed names and shallow intent groups for public Store authoring roles under `WooTheme`.
- Product-screen and design-system component code should read approved foundations from `WooTheme`,
  for example `WooTheme.colors.primary`, `WooTheme.text.titleMedium.emphasized`,
  `WooTheme.spacing.space5`, and `WooTheme.padding.padding5`.
- `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MaterialTheme.shapes` are interop
  projections for Material 3 components, defaults, and helpers. Use them when a Material API requires
  them, not as the primary Store design-system authoring surface.
- `WooTheme.colors` should expose the source-backed PR 2 color tokens used by the core foundation and
  inspected i1 component nodes, grouped shallowly by source intent.
- Do not limit `WooTheme.colors` to a small curated Material 3-like subset.
- Do not expose the manual `Semantic/*.tokens.json` groups in PR 2 unless a concrete Figma
  component node is confirmed to bind to that token group.
- Do not create `WooTheme.semanticColors`; supported status, alert, overlay, and palette colors live
  as grouped fields under `WooTheme.colors`.
- Keep Material 3-only projection aliases internal. Do not expose generated Material aliases such as
  fixed roles or surface-container aliases unless those names are real source-backed tokens.
- `outline` and `outlineVariant` are source-backed tokens and public under `WooTheme.colors`.
- Preserve source intent with shallow groups such as background, surface, outline, status, overlay,
  alert, and palette. Core colors remain top-level roles on `WooTheme.colors`.
- Palette/ramp and alert tokens are source-backed, but they do not automatically approve
  foreground/background pairing. Check component-specific contrast before using them for text,
  essential icons, or required state communication.
- Keep unresolved non-color Figma variables tracked in docs or internal mapping first, but
  source-backed color tokens still belong in the public PR 2 color surface.
- Internal `WooRadius`, `WooStroke`, and unresolved foundation groups may exist as implementation
  catalog entries without public accessors until a production component or pilot needs them.
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
