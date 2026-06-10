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

Store design-system APIs should live under:

```text
com.woocommerce.android.ui.designsystem
```

Suggested subpackages:

- `compose`: Compose theme wrappers and shared Compose-facing APIs.
- `compose.foundation`: theme, color, typography, spacing, shape, elevation, and token helpers.
- `compose.component`: production-ready Woo Mobile Design System Compose components.
- `compose.preview`: preview wrappers, sample data, and catalog-only helpers.
- `xml`: XML/View bridge helpers and XML-specific theme-overlay APIs.

Earlier Compose-only PRs started under `com.woocommerce.android.ui.compose.designsystem`. When
XML/View bridge support is added, move those APIs mechanically into the shared
`ui.designsystem.compose` package before adding XML bridge APIs. Keep that move behavior-neutral and
reviewable separately from the retained XML/View pilot.

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
- The new `WooTheme` accessor lives under `com.woocommerce.android.ui.designsystem.compose`.
  The existing `com.woocommerce.android.ui.compose.theme.WooTheme` composable remains the legacy Store
  wrapper until deliberately removed; new design-system code should not import it.
- Do not expose raw Figma variable names as public Android API.
- Public APIs should expose only production-ready tokens and components.
- In-progress i1 areas may be documented, tracked, or preview-only until signed off.
- Preview-only components should not be exposed as reusable product-screen APIs.
- Keep preview-only implementations private/internal to catalog or preview files under
  `designsystem.compose.preview`.

## Theme Root Strategy

Both Store Compose theme roots must satisfy the design-system foundation contract.

- `DesignSystemMode.LEGACY` uses `WooThemeWithBackground`.
- `DesignSystemMode.DESIGN_SYSTEM` uses `WooDesignSystemThemeWithBackground`.
- Default/no explicit `composeView` theme follows `FeatureFlag.NEW_DESIGN_SYSTEM`.
- Explicit `DesignSystemMode.DESIGN_SYSTEM` still forces the real design-system foundation.
- Explicit `DesignSystemMode.LEGACY` still forces the legacy-compatible foundation.
- Do not replace `WooThemeWithBackground` with `WooDesignSystemThemeWithBackground` while the flag is
  off.
- Existing Material 2 usage can remain until touched.
- Do not globally remap existing `Woo*`, `WC*`, XML styles, colors, typography, or app theme
  resources for i1.

The rollout selector should be shared by Compose and XML/View bridge code. If existing implementation
uses the Compose-only name `ComposeTheme`, rename and move it to `DesignSystemMode` when the XML/View
bridge PR lands. Do not introduce a second XML-specific mode enum.

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
that default. Use explicit mode selection only when a screen, preview, or test intentionally needs to
force a foundation:

```kotlin
composeView(mode = DesignSystemMode.DESIGN_SYSTEM) {
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
- Retained XML/View screens that keep Activity-owned toolbar behavior can use a scoped XML toolbar
  overlay at the Activity toolbar inflation boundary instead of replacing the toolbar with Compose.
  Resolve that overlay through `DesignSystemMode`, keep `setSupportActionBar(...)`, title, up, and
  menu contracts untouched, and read the same promoted Android token resources used by Compose.
- The PR7 retained XML/View pilot applies this only to `AppSettingsActivity` toolbar inflation. When
  the feature flag is on, all settings screens hosted by that Activity get the DS-skinned toolbar;
  this sibling impact is accepted because `FeatureFlag.NEW_DESIGN_SYSTEM` remains the kill switch.
- Menu, overflow, `SearchView`, and collapsing toolbar styling are not proven by the menu-less
  settings pilot. Future menu/search retained XML screens should likely use the same XML toolbar
  overlay direction, but only after a dedicated action-menu/SearchView audit and pilot.

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

Use three pilots so the adapter validates the likely adoption outcomes:

- `PrivacySettingsFragment`: existing Compose screen adopting the design-system theme, tokens, and components.
- `FeedbackCompletedFragment`: XML/View layout to Fragment-hosted Compose layout migration.
- A retained XML/View screen: targeted screen-level XML bridge without full Compose migration.

Run `PrivacySettingsFragment` first, then `FeedbackCompletedFragment`, after
foundations/components/previews and the `composeView` mode selector are available. Existing Compose
adoption is the lower-risk canary for the design-system APIs; the XML/View-to-Compose pilot then
validates the larger migration workflow.

After those two pilots, run one retained XML/View bridge pilot before the final AI migration playbook
pass. Choose the exact screen during that PR's planning. The target should be complex enough to test
real XML/View bridge mechanics but low traffic enough to avoid putting a major commerce workflow at
risk. Prefer a screen with at least two of: XML Fragment root, adapter row inflation, custom `Woo.*`
XML styles, toolbar/chrome, empty/loading state, light/dark sensitivity, or a small direct-resource
gap. Avoid product/order/payment editing, scanners, WebView, heavy selection flows, and broad product
or order list redesign.

All pilots must preserve existing product behavior, navigation, analytics, strings, and Store app architecture boundaries.

## Delivery Sequence

Follow `docs/design-system/implementation-plan.md`.

The intended sequence is docs first, then foundations/theme, then components/previews, then
`composeView` mode selection, then `PrivacySettingsFragment`, then `FeedbackCompletedFragment`, then
a retained XML/View bridge pilot, then final playbook updates from all three pilots.

## View/XML Compatibility

Fragment-hosted Compose layout migration is preferred for new and substantially redesigned Store screens, but it is optional.

Some screens should remain XML/View because migration would require substantial work. Those screens can still receive targeted token/style updates if doing so is low-risk and does not force a global theme replacement.

High-traffic retained XML/View screens may adopt a screen-level design-system bridge instead of a
full Compose migration. The bridge should be opt-in at the screen root, using a themed inflation
context such as `ContextThemeWrapper` plus `LayoutInflater.cloneInContext(...)`, so the screen can
resolve design-system-aware XML attrs without changing the Activity or app theme.

The primary helper should support the existing Fragment layout-resource pattern by overriding
`onGetLayoutInflater(...)` and returning a cloned inflater for the opted-in screen. Per-layout inflate
helpers can exist as narrow conveniences, but they should not be the main integration model because
many Store Fragments already let `BaseFragment(R.layout...)` own root inflation and bind in
`onViewCreated(...)`.

XML bridge opt-in should follow the same shared `DesignSystemMode` decision as Compose roots:
default/no explicit mode follows `FeatureFlag.NEW_DESIGN_SYSTEM`, legacy mode preserves existing View
styling, and design-system mode applies only to the opted-in screen root.

Use Material/theme attrs first for generic foundation roles such as surface, on-surface, primary,
error, shape, and text appearances. Add custom Woo design-system attrs or promoted Android resources
only for semantic gaps that Material attrs cannot express, such as product/order status colors,
notice/banner roles, skeleton/loading colors, chips/tags, or commerce-specific emphasis.

Theme overlays solve attribute resolution, not full visual migration. They do not affect XML that
hardcodes direct `@color`, `@dimen`, concrete drawables, selectors, or programmatic resource lookups.
Before opting in a retained View screen, audit root inflation, RecyclerView row inflation, custom
Views, dialogs/menus/popups, direct-resource styles, drawables/selectors, and dark-mode values.

When XML/View screens need design-system styling, promote only the required token primitive values to Android resources and update Compose to read those same resources.

Do not globally apply design-system XML/View styles in i1 foundation work. Add targeted XML/View style usage only when a non-migrated XML/View screen needs design-system styling.

For retained XML toolbar overlays, apply XML styles only at the known toolbar inflation boundary.
Do not move those toolbar styles into the app theme or broad `Woo.*` style remapping.
