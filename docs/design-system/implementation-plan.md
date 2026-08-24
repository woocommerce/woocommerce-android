# Store Design System Implementation Plan

This plan sequences the Woo Mobile Design System i1 adapter work for trunk-based delivery.

The rollout direction is decided in [rollout-direction.md](rollout-direction.md). The short version is:

- Build Store-only foundations and production-ready components.
- Migrate the first coherent wave: Dashboard, Products, Orders, More, top Product Detail, and top
  Order Detail.
- Keep Product Detail and Order Detail launched child flows out of scope for this wave.
- After the first wave, converge legacy XML and legacy Compose foundations toward the design-system
  look for safe color/chrome tokens only.

## PR Sequence

### 1. Docs and Checkpoint

Capture the integration model before implementation.

This docs branch is the contract for the future Store Design System foundation build. The absence of
runtime foundation code in this branch is expected and is not a defect. Future implementation should
consume:

- [token-map.md](token-map.md) for source-backed token rows, values, statuses, unresolved items, and
  parser rules.
- [material3-reference.md](material3-reference.md) for Material 3 projection behavior.
- [android-adapter.md](android-adapter.md) for public/internal Android API boundaries and
  parser/mode handling.

Foundation source data comes from `figma-export.json`. The current export omits the former top-level
`Semantic` section and high-contrast modes. If either returns in a future export, `Semantic` remains
traceability-only and high-contrast colors stay out of normal `Light` / `Dark` runtime mapping until
separately scoped.

Expected output:

- Design-system docs skeleton.
- Decision checkpoint with source references and rejected alternatives.
- Material 3 reference for token semantics and Compose interop.
- Token map, component catalog, migration playbook, and rollout direction.

The rollout direction doc is the canonical source for in-scope and out-of-scope screens. Other docs
should link to it instead of repeating the scope table.

### 2. Foundations and Theme

Implement the design-system foundation layer without forcing adoption in product screens.

Expected output:

- Store-only Gradle module `:libs:store-design-system`, separate from `:libs:commons`.
- Pure module-owned foundations and previews under
  `com.woocommerce.android.ui.compose.designsystem.*`.
- `WooDesignSystemTheme`.
- Migration-era wrapper naming: use `WooDesignSystemTheme`, not `WooNewTheme`, while the legacy
  `WooTheme` wrapper exists. Future consolidation can happen after the legacy wrapper is removed.
- `WooTheme` foundation accessors for theme-scoped production APIs.
- `WooDesignSystemThemeWithBackground` providing the real design-system foundation.
- A DS-specific builder, such as `designSystemComposeView {}`, for explicitly migrated screens.
- Existing legacy screens continue using the current legacy Compose root until they are intentionally
  migrated or renamed to `legacyComposeView` at the controlled boundary.
- No root-selection indirection inside shared/default `composeView {}` calls. The root should be
  obvious at the call site.
- Follow [rollout-direction.md](rollout-direction.md) for the exact controlled root-API rename
  boundary and audits.
- Manual i1 runtime tokens, with colors as the XML-safe resource-backed exception and non-color
  foundations remaining Kotlin/Compose-owned.
- Foundation groups for color, typography, spacing, padding, radius, icon size, and stroke.
- Source-backed color tokens exposed through `WooTheme.colors`, with direct core roles plus shallow
  Store authoring groups: container, surface, status, background, overlay, state layer, tint layer,
  alert, and palette.
- Internal Material 3 projections for Material 3 component interop, with Material 3 treated as a
  projection rather than the source of Store foundations.
- Full text roles through `WooTheme.text`.
- Spacing, padding, radius, icon size, and stroke through `WooTheme.spacing`, `WooTheme.padding`,
  `WooTheme.radius`, `WooTheme.iconSize`, and `WooTheme.stroke`.
- Supported status, alert, overlay, state-layer, tint-layer, and palette colors as grouped fields
  under `WooTheme.colors`; no separate `WooTheme.semanticColors`.
- Source-backed `WooTheme.colors.error` / `onError` projected to Material `error` / `onError` and
  consumed by Checkbox error states.
- Distinct `surface.default` and `surface.bright`, plus source-backed `surfaceDim`,
  `surfaceContainerHighest`, and `surface.onVariantHighest` roles.
- Public `WooTheme.colors.stateLayers.onSurface` colors for `opacity08`, `opacity10`, `opacity16`,
  and `opacity24`; no public state-alpha floats and no Material `ColorScheme` projection.
- Live Figma component evidence maps 08 to disabled filled/tonal button containers, 10 to neutral
  outlined badge and disabled outlined-button border, 16 to disabled checkbox/radio roots, and 24
  to disabled button content, Search placeholder, and disabled choice-control marks/dots. High
  contrast remains unresolved.
- Public `WooTheme.colors.tintLayers.primaryContainer`, `onSurface`, and `primary` colors for
  `opacity08`, `opacity10`, `opacity16`, and `opacity24`; the Segmented Control track consumes
  Primary Container opacity16, while dividers/subtle boundaries consume On Surface opacity16.
- `WooTheme.iconSize` scoped to glyph sizes only.
- Source-backed stroke from `Shape/Stroke/Weight/*` through `WooTheme.stroke`, promoted for
  production component usage.
- Preview wrappers and light/dark previews using `@PreviewLightDark`.
- Token map entries for implemented foundations.
- Design-system module code does not import app resources, legacy app theme classes, Hilt, POS, or
  Store feature packages.

Implementation risks to verify when code work begins:

- Radius projection visual changes: `large` moves from the current `8dp` projection to `12dp`, and
  `extraLarge` moves from the current `8dp` projection to `16dp`.
- Fractional stroke width rendering for `0.5dp`, `0.75dp`, and `1.5dp`.
- High-contrast state-layer behavior, which remains outside the normal runtime mapping.

Foundation work remains opt-in. Do not globally remap existing app theme resources in the foundation
PR. Store design-system color primitives may be module-local Android resources so `WooTheme.colors`
can serve both Compose and targeted XML/View convergence. Non-color foundations remain
Kotlin/Compose-owned. Do not document an app-owned legacy-compatible design-system foundation bridge
as required for this rollout path unless a future implementation explicitly chooses it.

Do not add implementation work to recompute token-map color ratios. The docs intentionally remove
stale manually maintained ratio and alpha-composition notes.

### 3. Components and Preview Catalog

Implement the i1 component catalog with previews, while exposing production APIs only for ready components.

Expected output:

- Compose-first, Material 3-only design-system components under
  `com.woocommerce.android.ui.compose.designsystem`.
- `Woo` component naming.
- Material 3 wrappers where the mapping is close.
- Custom components only where Material 3 is materially different.
- Component catalog status updates.
- Light and dark previews for every component using `@PreviewLightDark`.
- Production APIs for the subset needed by the first-wave tab and top-detail surfaces plus low-risk
  primitives.
- Private/internal preview catalog implementations for unsettled components.

Production screens should consume only production-ready components. In-progress components can remain preview-only.

The initial production subset should cover top/navigation bar, page title/body/link text-token usage,
primary button, list/cell rows, switch, icon button, divider, progress indicator, and the
spacing/radius/color/typography tokens they depend on.

Chrome components should follow [rollout-direction.md](rollout-direction.md): unified visual look,
not one forced implementation. Compose-owned screens use `WooTopAppBar`; heavy XML screens may keep a
DS-looking XML toolbar when needed for compatibility.

### 4. First-Wave Screen Migration

Migrate the first coherent product wave defined in [rollout-direction.md](rollout-direction.md).

Expected output:

- Dashboard tab surface migrated to design-system UI.
- Products tab surface migrated to design-system UI.
- Orders tab surface migrated to design-system UI.
- More tab surface migrated to design-system UI.
- Top Product Detail surface migrated, with launched child/edit flows left legacy.
- Top Order Detail surface migrated, with launched child flows left legacy.
- Existing Fragments, XML nav graphs, SafeArgs, ViewModels, analytics, strings, and product behavior
  preserved unless there is an explicit product decision.
- One design-system UI implementation per migrated screen. No permanent duplicate legacy/design-system screen trees.
- Migrated screens use the design-system root explicitly. Non-migrated screens stay on the legacy
  root.
- `ComposeView` used for migrated content sections where heavy XML shells must remain for compatibility.
- Previews and screenshot review for migrated surfaces under the design-system root in light and
  dark mode.

Before final merge of the migration branch, follow the controlled root-API rename boundary defined
in [rollout-direction.md](rollout-direction.md).

The XML bridge explored in earlier branches is out of scope. A retained XML shell is allowed only as
a compatibility boundary around migrated design-system content.

### 5. Legacy Theme Convergence

After the first-wave screens, converge existing XML and legacy Compose foundations toward the design-system look.

Expected output:

- Shared module-local color resources where XML/View needs the same primitive values as Compose.
- Background/surface, toolbar/chrome, divider/outline, and verified primary/accent color alignment.
- DS-looking XML toolbar style for legacy-heavy screens that cannot move toolbar ownership to Compose.
- Compose toolbar alignment through `WooTopAppBar` on Compose-owned screens.
- No parallel Kotlin/Compose and XML resource definitions for the same color primitive values.

Do not use this phase for global typography replacement, global spacing/padding replacement, global
status/semantic color rewrites, app-wide card radius changes, or behavior/navigation rewrites.
Typography, spacing, padding, radius, icon size, and stroke remain Kotlin/Compose-owned.

### 6. Playbook Updates

Keep the migration playbook aligned with real first-wave findings.

Expected output:

- Concrete examples from first-wave migrations once they exist.
- Known pitfalls.
- Component usage examples.
- Verification checklist refinements.
- Guidance for screens that remain XML/View because they are outside the first-wave scope.
- Required candidate assessment output for unassigned future screens.

Add new example docs only when first-wave migrations produce reusable findings.

## Delivery Constraints

- POS stays out of scope.
- The rollout scope is defined by [rollout-direction.md](rollout-direction.md).
- No mandatory migration of every screen.
- No global app rewrite.
- No raw Figma variable names in public Android API.
- No raw private Figma node IDs in public docs.
- `figma-export.json` remains an audit/source artifact; do not hand-edit it as part of foundation
  implementation.
- Runtime/public parser logic must ignore a future top-level `Semantic` section unless the contract
  is deliberately updated with approved evidence.
- Normal runtime `Light` / `Dark` values must not infer or substitute high-contrast modes.
- No parallel Kotlin/Compose and XML resource definitions for the same token primitive values.
- No production screen should consume preview-only components.
- Preview-only components should not be exposed as reusable production-screen APIs.
- Migrated screens should not keep permanent duplicate legacy/design-system implementations.
- Out-of-scope child flows may remain legacy without an expiry plan for this wave.
- Temporary full-screen fallbacks inside in-scope migrated screens are allowed only for genuinely
  high-risk migration gaps and require an expiry/removal plan.
- New design-system previews should use `androidx.compose.ui.tooling.preview.PreviewLightDark`.
- Design-system component previews should wrap content in `WooDesignSystemTheme`, not the legacy
  Store theme root.
- Migrated screen previews should cover the design-system root in light and dark mode.
- Final root-API rename should be handled as a dedicated mechanical boundary and verified with
  strict `rg` audits before merge.
- Screenshot verification is required for first-wave screens and high-risk components, not for every
  small primitive component.
- Production components need accessibility review before product-screen adoption.
- Each PR should be reviewable independently.
