# Store Design System Android Adapter

This document defines how the Store Management App adopts the Woo Mobile Design System i1 with the least disruption.

The current rollout scope is defined in [rollout-direction.md](rollout-direction.md). This adapter
doc defines the technical boundaries that support that rollout.

## Scope

- Store Management App only.
- POS is out of scope.
- Design-system foundations and components live in the Store-only `:libs:store-design-system`
  module, not `:libs:commons`.
- The first rollout wave migrates Dashboard, Products, Orders, More, top Product Detail, and top Order Detail.
- Product Detail and Order Detail launched child flows remain out of scope for the first wave.
- Design-system components are adopted deliberately. Existing screens keep their current behavior and
  styling until migrated or until the legacy theme convergence phase touches safe color/chrome tokens.
- Figma is the design-intent source of truth. Android owns the runtime API contract.
- Source references use public-repo shorthands: P2 `pe5sF9-5ox-p2`, Figma
  `50XIH5MmOf4xUYEkM6fAm6-fi`. Do not expand them into raw URLs in public repo docs.
- Agents should use [figma-navigation.md](figma-navigation.md) when inspecting Figma components or
  collecting live Figma screenshots.
- Foundation source values come from `docs/design-system/figma-export.json`. Approved Android
  overrides are documented in this adapter and [token-map.md](token-map.md); the export remains an
  audit/source artifact and must not be hand-edited to apply them.

## Package

New Compose APIs live under:

```text
com.woocommerce.android.ui.compose.designsystem
```

Subpackages:

- `foundation`: theme, color, typography, spacing, shape, elevation, and token helpers.
- `component`: production-ready Woo Mobile Design System components.
- `preview`: preview wrappers, sample data, and catalog-only helpers.

The Gradle module boundary is stricter than the package boundary. `:libs:store-design-system` must
not depend on app `R`, legacy app theme classes, Hilt, POS, or Store feature packages. App-layer
rollout wiring should stay outside the module.

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
  colors, typography, spacing, padding, radius, icon size, and stroke. State and tint layers are
  public groups within `WooTheme.colors`, not separate foundation accessors.
- The new `WooTheme` accessor lives under `com.woocommerce.android.ui.compose.designsystem`.
  The existing `com.woocommerce.android.ui.compose.theme.WooTheme` composable remains the legacy Store
  wrapper until deliberately removed; new design-system code should not import it.
- Follow-up `WOOMOB-3515` tracks a detekt guardrail for files that import both the legacy
  `com.woocommerce.android.ui.compose.theme.*` APIs and the design-system
  `com.woocommerce.android.ui.compose.designsystem.*` APIs during migration.
- Do not expose raw Figma variable names as public Android API.
- Public APIs should expose only production-ready tokens and components.
- In-progress i1 areas may be documented, tracked, or preview-only until signed off.
- Preview-only components should not be exposed as reusable product-screen APIs.
- Keep preview-only implementations private/internal to catalog or preview files under `designsystem.preview`.
- The planned public foundation surface is:
  - `WooTheme.colors` with direct core roles plus `container`, `surface`, `status`, `background`,
    `overlay`, `stateLayers`, `tintLayers`, `alert`, and `palette` groups.
  - `WooTheme.text` with `regular`, `emphasized`, and `strong` variants.
  - `WooTheme.spacing` and `WooTheme.padding` as separate APIs with identical value scales.
  - `WooTheme.radius` including Woo-only `none` and `full` plus Material projection roles.
  - `WooTheme.iconSize` scoped to glyph sizes only.
  - `WooTheme.stroke` for source-backed border and divider widths used by production components.
- Expose primitive palette ramps intentionally through public `WooTheme.colors.palette.*` fields.
- Keep primitive literals in palette XML resources and alias exact matching semantic XML resources
  to those primitives. Keep resolved ARGB literals for opacity tokens that cannot be represented as
  a normal Android color-resource alias.
- Treat `surface.bright`, `surfaceDim`, `surfaceContainerHighest`, and `surface.onVariantHighest` as
  source-backed public Store roles, not generated Material aliases. Generic `surface.default` and
  `surface.bright` are distinct in both modes.
- Expose `WooTheme.stroke` because production design-system components use the source-backed stroke
  scale. Fractional stroke widths still need visual verification in components that use them.
- Expose `WooTheme.colors.stateLayers.onSurface.opacity08`, `opacity10`, `opacity16`, and
  `opacity24` as complete mode-aware `Color` values. Do not expose public `WooTheme.stateAlpha` or
  other raw alpha floats.
- Keep `stateLayers` separate from `surface`; the supplied export resolves the normal light
  state-layer base and `surface.onDefault` to `#101517` while preserving distinct semantic groups.
- State-layer colors are Store authoring tokens and do not project into Material `ColorScheme`.
- Expose `WooTheme.colors.error` / `onError` together and project them to Material `error` /
  `onError`.
- Expose Primary Container, On Surface, and Primary tint layers as complete mode-aware colors under
  `WooTheme.colors.tintLayers`; the Segmented Control consumes `primaryContainer.opacity16`, and
  dividers/subtle component boundaries consume `onSurface.opacity16`.
- High-contrast state-layer values remain unresolved and stay outside normal `Light` / `Dark`
  runtime mapping.
- Do not create public `WooTheme.minimumTouchTarget` from legacy dimensions or screen-size variables.

## Theme Root Strategy

Screen migration is explicit: migrated screens opt into the design-system root, and non-migrated
screens stay on the legacy root. Do not add root-selection indirection to arbitrary/default
`composeView {}` calls; a screen is migrated by changing its call site to the DS root builder.

During migration work, introduce or use a DS-specific builder such as `designSystemComposeView {}`.
The controlled root-API rename boundary is defined in [rollout-direction.md](rollout-direction.md);
keep that file as the source for exact rename steps and audits.

The Store design-system module still owns pure foundations/components and still does not import app
`R`, Hilt, POS, or Store feature packages. Do not document a legacy-compatible design-system
foundation bridge as required for this rollout path unless a future implementation explicitly
chooses that bridge.

Do not globally remap existing `Woo*`, `WC*`, XML styles, colors, typography, or app theme resources
in the foundation PR. Later legacy convergence is intentionally narrower: safe color/chrome tokens only,
as described in [rollout-direction.md](rollout-direction.md).

## Rollout Strategy

Migrate screens once to design-system components. Do not keep duplicate `LegacyScreen` and
`DesignSystemScreen` implementations for ordinary migrations.

- Use explicit root builders to make migrated and non-migrated call sites clear.
- Screen code should not branch between legacy and design-system UI trees as a permanent structure.
- The first-wave scope is fixed in [rollout-direction.md](rollout-direction.md).
- Temporary full-screen fallbacks inside in-scope screens are allowed only for genuinely high-risk
  migration gaps and must have an explicit expiry/removal plan.
- Out-of-scope child flows may remain legacy for this wave without an expiry plan.
- Do not add broad component variants unless there is a clear rollout need.

No design-system component should depend on hardcoded light fallback defaults. Rendering a
design-system component under the design-system root must receive valid `WooTheme.colors`,
`WooTheme.text`, `WooTheme.spacing`, `WooTheme.padding`, `WooTheme.radius`, `WooTheme.iconSize`, and
`WooTheme.stroke` values.

## Chrome Component Compatibility

Top app bar/chrome migration is not just a token change. Moving from the Activity toolbar to Compose
`WooTopAppBar` changes chrome ownership and structure.

The direction is a unified design-system visual look, not one mandatory implementation:

- Compose-owned screens use `WooTopAppBar`.
- The module `WooTopAppBar` is design-system-only and lives in `:libs:store-design-system`.
- Heavy XML screens may keep XML toolbar ownership if the toolbar matches the design-system look.
- XML-heavy screens that need visual parity can use `WooDesignSystemToolbar` from
  `:libs:store-design-system` for automatic design-system chrome. The library also owns
  `Widget.Woo.DesignSystem.Toolbar` and `ThemeOverlay.Woo.DesignSystem.Toolbar` for XML opt-ins.
  Visible inflated icon actions are decorated in place; text actions stay borderless, and
  expanded/custom action views remain screen-owned.
- Migrated Compose-owned screens render `WooTopAppBar` under the design-system root.
- Legacy-heavy screens can preserve existing toolbar ownership unless a scoped migration chooses a
  DS-looking XML toolbar or an explicit Compose chrome migration.
- Preserve `SearchView`, `ActionMode`, collapsing app bars, menu/action ownership, navigation
  ownership, and insets.
- A DS-looking XML toolbar is acceptable for legacy-heavy screens when changing toolbar ownership would
  turn the UI migration into a broader app rewrite.

## Token Strategy

i1 uses manual Kotlin/Compose runtime token definitions first.

- Use `docs/design-system/material3-reference.md` for official Material 3 role semantics, Compose
  API pointers, and default scale references while mapping i1 foundations.
- Keep adapter token names stable and screen-facing.
- Use source-backed names and shallow intent groups for public Store authoring roles under `WooTheme`.
- Product-screen and design-system component code should read approved foundations from `WooTheme`,
  for example `WooTheme.colors.primary`, `WooTheme.text.titleMedium.emphasized`,
  `WooTheme.spacing.space5`, `WooTheme.padding.padding5`, `WooTheme.radius.medium`, and
  `WooTheme.stroke.regular`. State-aware components consume complete colors such as
  `WooTheme.colors.stateLayers.onSurface.opacity16`.
- `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MaterialTheme.shapes` are interop
  projections for Material 3 components, defaults, and helpers. Use them when a Material API requires
  them, not as the primary Store design-system authoring surface.
- `WooTheme.colors` should expose source-backed Store authoring roles from `figma-export.json` /
  `Woo Theme`, grouped shallowly by source intent. Primitive palettes come from top-level `Colors`.
- Do not limit `WooTheme.colors` to a small curated Material 3-like subset.
- Parse Store runtime/public foundations from the current top-level foundation sections in
  `figma-export.json`.
- The current export omits top-level `Semantic`. If a future export includes it, keep it for
  traceability but ignore it for Store runtime/public mapping and Material projections unless a
  component audit updates the contract.
- Use normal `Light` / `Dark` values for runtime color modes. The current export omits high-contrast
  modes; accessibility-mode mapping remains separately scoped.
- Use Android typography mode values from `Typescale`; `Typescale/<Role>/Font` resolves through
  `Font Theme/Font/Plain`, whose Android value is `Roboto`. Android default font is the accepted
  runtime equivalent.
- Do not create `WooTheme.semanticColors`; supported status, alert, overlay, state-layer, tint-layer,
  and palette colors live as grouped fields under `WooTheme.colors`.
- Keep Material 3-only projection aliases internal. Do not expose generated Material aliases such as
  fixed roles or surface-container aliases unless those names are real source-backed tokens.
- The runtime may intentionally leave unsupported Material 3 roles on builder defaults when a Store
  token would be a guess. Project source-backed container and promoted surface roles from Store
  fields.
- `outline` and `outlineVariant` are source-backed tokens and public under `WooTheme.colors`.
- Preserve source intent with direct core roles plus shallow groups such as container, surface,
  status, alert, background, overlay, state layer, tint layer, and palette.
- Keep unresolved non-color Figma variables tracked in docs or internal mapping first, but
  source-backed color tokens still belong in the public foundation surface.
- `WooTheme.radius`, `WooTheme.iconSize`, and `WooTheme.stroke` are public because they are
  source-backed design-system foundations. `WooTheme.iconSize` is glyph-size only; it is not a
  generic layout-size or touch-target API. `WooTheme.stroke` is scoped to border and divider widths.
  Unresolved foundation groups may stay documented-only.
- Keep source token names in documentation and internal mapping metadata when useful, but do not
  record raw Figma variable IDs in repo docs.
- Structure token definitions so a future Figma generation pipeline can update adapter internals
  without changing screen APIs.
- Every token that reaches production APIs must be represented in `docs/design-system/token-map.md`.
- Keep `WooTheme.colors` as the Store authoring API even when color primitives are resource-backed internally.
- Store design-system color primitives are the XML-safe exception and may live in module-local
  Android color resources so Compose and targeted XML/View usage share the same values.
- Non-color foundations remain Kotlin/Compose-owned: typography, spacing, padding, radius, icon
  size, and stroke do not move to Android resources as part of this adapter layer.
- Do not keep parallel Kotlin/Compose and XML resource definitions for the same token primitive value.

### Runtime State Layers

The runtime `stateLayers` group follows the Figma hierarchy by nesting the `onSurface` base role
before its opacity variants. It contains complete normal-mode colors rather than reusable alpha
constants:

| API suffix | Light Figma `RRGGBBAA` / Android `AARRGGBB` | Dark Figma `RRGGBBAA` / Android `AARRGGBB` | Verified use |
| --- | --- | --- | --- |
| `onSurface.opacity08` | `#10151714` / `#14101517` | `#FFFFFF14` / `#14FFFFFF` | Disabled filled and tonal button containers. |
| `onSurface.opacity10` | `#1015171A` / `#1A101517` | `#FFFFFF1A` / `#1AFFFFFF` | Neutral outlined badge and disabled outlined-button border. |
| `onSurface.opacity16` | `#10151729` / `#29101517` | `#FFFFFF29` / `#29FFFFFF` | Disabled checkbox/radio roots. |
| `onSurface.opacity24` | `#1015173D` / `#3D101517` | `#FFFFFF3D` / `#3DFFFFFF` | Disabled button content, Search placeholder, and disabled choice-control marks/dots. |

The checked-in export directly supplies all four normal-mode state-layer colors. High-contrast
state-layer behavior remains unresolved because those modes are not included in the current export.

### Runtime Tint Layers

The runtime `tintLayers` group exposes complete mode-aware colors under `primaryContainer`,
`onSurface`, and `primary`, each with `opacity08`, `opacity10`, `opacity16`, and `opacity24`.

- Segmented Control binds its track to `primaryContainer.opacity16`.
- Dividers and subtle component boundaries bind to `onSurface.opacity16`.
- Light `onSurface.opacity24` intentionally uses `#1E1E1E` as its RGB base, unlike the State Layer.
- WOOMOB-3552 approves `#6D469C` as the dark Primary Container tint base for the complete family:
  Android `#146D469C`, `#1A6D469C`, `#296D469C`, and `#3D6D469C` for 08/10/16/24. The checked-in
  export is stale for 08/10/16 and remains unchanged as the audit/source artifact.
- Primary tint layers use `#873EFF` at the matching alpha in both modes.

Keep these as complete mode-aware colors rather than public alpha floats. The committed export keeps
its explicit RGBA values; runtime XML aliases are allowed only where exact and semantically appropriate.

## Component Strategy

- Implement the i1 catalog as Compose-first components with previews.
- Wrap Material 3 components where the design intent maps cleanly.
- Build custom components only when Material 3 behavior, shape, state, or layout is materially different.
- Do not migrate a screen only to consume a component. Screen migration follows [rollout-direction.md](rollout-direction.md).
- Production screens may import only production-ready components.
- Preview-only components can be shown in the catalog, but must not expose public product-screen APIs for migration agents to consume.

## Delivery Sequence

Follow `docs/design-system/implementation-plan.md`.

The current sequence is docs, foundation API gate resolution, foundations/theme,
components/previews, first-wave screen migration, safe legacy theme convergence, and playbook updates
from real migration findings.

## View/XML Compatibility

Fragment-hosted Compose layout migration is preferred for new and substantially redesigned Store
surfaces, but it is not mandatory for every legacy flow.

For first-wave heavy XML surfaces, use `ComposeView` for migrated sections, rows, or content areas
while retaining the XML shell only where compatibility requires it. The retained shell is not the XML
bridge strategy; it is a compatibility boundary around migrated design-system content.

Some out-of-scope child flows should remain XML/View during this wave. Those screens can still receive
targeted token/style updates during legacy convergence if doing so is low-risk and does not force a
global theme replacement.

When XML/View screens need design-system styling, consume only the required shared color resources
and keep Compose reading those same resources through `WooTheme.colors`.

Do not create XML resources for non-color primitives as the primary implementation. Typography,
spacing, padding, radius, icon size, stroke, elevation, and minimum touch target remain
Kotlin/Compose-owned or unresolved unless a later approved plan changes that boundary. State and
tint layers are color primitives, so their shared module-local resources follow the same ownership
rule as other `WooTheme.colors` values.
