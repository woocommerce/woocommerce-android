# Woo Mobile Design System Integration Checkpoint

This checkpoint preserves the planning context for the Store Management App design-system integration.

The current rollout direction is decided in [rollout-direction.md](rollout-direction.md). That file
is the canonical source for first-wave scope and out-of-scope child flows.

Root rollout update: [rollout-direction.md](rollout-direction.md) now supersedes the older
root/foundation-selection notes preserved below. The current direction is explicit screen migration:
migrated screens opt into the design-system root, non-migrated screens stay on the legacy root, and
existing `composeView {}` calls do not get root-selection indirection.

## Scope

- Store Management App only.
- POS is out of scope.
- The goal is least-disruptive, trunk-based integration of the Woo Mobile Design System.
- The first iteration uses the design system discussed in the May 27, 2026 P2: `Woo Mobile Design System, i1`.

## Source References

- P2: `Woo Mobile Design System, i1`, May 27, 2026 (`pe5sF9-5ox-p2`).
- Figma: `Woo Mobile Design System` (`50XIH5MmOf4xUYEkM6fAm6-fi`).

Do not expand these shorthands into raw P2 or Figma URLs in public repo docs.

## Agreed Decisions

- Use an Android Design System Adapter, not a global app rewrite.
- Keep the adapter Store-only, with foundations and components in `:libs:store-design-system`.
- The rollout strategy is decided. First-wave scope is Dashboard, Products, Orders, More,
  top Product Detail, and top Order Detail.
- Product Detail and Order Detail launched child flows are out of scope for the first wave.
- Each migrated first-wave screen should have one design-system UI implementation.
- Migrated screens use an explicit design-system root builder during migration.
- Non-migrated screens stay on the legacy root until the controlled rename boundary in
  [rollout-direction.md](rollout-direction.md).
- The XML bridge explored in earlier branches is out of scope.
- Heavy XML screens may retain an XML shell only where needed for compatibility, with `ComposeView`
  hosting migrated sections, rows, or content areas.
- Toolbar direction is a unified design-system visual look, not one mandatory implementation.
- Compose-owned screens use `WooTopAppBar`.
- Heavy XML screens may keep XML toolbar ownership if the toolbar matches the design-system look and
  preserves `SearchView`, `ActionMode`, collapsing app bars, menu/action ownership, navigation
  ownership, and insets.
- After the first-wave screens, legacy XML and legacy Compose can converge toward the design-system
  look for safe tokens only: colors, chrome, background/surfaces, dividers/outline, and toolbar look.
- Global typography replacement, global spacing/padding replacement, and global status/semantic color
  rewrites remain out of scope.
- Add new design-system foundations and components as deliberately adopted APIs.
- Do not globally remap existing `Woo*`, `WC*`, XML styles, or resource names in the foundation PR.
- Figma is the design-intent source of truth; Android owns the runtime API contract.
- Manually define stable i1 Kotlin/Compose runtime tokens first.
- Structure tokens and docs so a future Figma generation pipeline can update adapter internals later.
- Do not expose raw Figma variable names as public Android APIs.
- Keep a strict token map with Android API/token name, source shorthand or source token name when
  useful, light and dark values, Material 3 role mapping, status, and notes.
- Public `WooTheme.colors` exposes source-backed color tokens used by the core foundation and
  inspected i1 component nodes, grouped shallowly by source intent.
- Do not limit `WooTheme.colors` to a small curated Material 3-like subset.
- Material 3 color roles remain internal interop projection aliases, not the Store authoring surface.
- Do not expose generated Material aliases such as fixed roles or surface-container aliases unless
  they are real source-backed tokens.
- Do not add `WooTheme.semanticColors`; supported status, alert, overlay, and palette colors live as
  grouped fields under `WooTheme.colors`.
- Do not expose top-level `Semantic` entries from `figma-export.json` unless a concrete Figma
  component node is confirmed to bind to that token group.
- `outline` and `outlineVariant` are source-backed and public under `WooTheme.colors`.
- Store design-system color primitives are the XML-safe exception and may live in module-local
  Android resources so Compose and targeted XML/View usage share the same values.
- Non-color foundations remain Kotlin/Compose-owned unless a later approved plan says otherwise.
- Do not keep parallel Kotlin/Compose and XML resource definitions for the same token primitive values.
- Code should publicly expose only production-ready tokens/components.
- In-progress i1 areas can be documented, tracked, or preview-only until stable.
- Preview-only components should not be exposed as reusable product-screen APIs.
- Preview-only implementations should stay private/internal to catalog or preview files under `designsystem.preview`.
- New design-system components are Compose-first and Material 3-only.
- Existing Material 2 usage can remain until touched.
- Use Material 3 wrappers as the default implementation strategy; build custom components only when Material 3 is too different.
- Component PR scope is full i1 catalog with previews, but production APIs only for first-wave needs and low-risk primitives.
- Unsettled components stay private/internal preview catalog implementations.
- Initial production subset covers top/navigation bar, page title/body/link text styles or wrappers,
  primary button, list/cell rows, switch, icon button, divider, progress indicator,
  and the tokens they depend on.
- Progress indicator is not listed as an i1 Figma component, but should still be wrapped as a thin
  Material 3 adapter for future custom design replacement.
- Do not add more thin Material 3 wrappers beyond the initial production subset unless a later
  design-system decision explicitly expands the catalog.
- Component names use the `Woo` prefix inside the design-system package.
- Do not use `WooDs*` or `WC*` for new design-system components.
- Module: `:libs:store-design-system`, separate from `:libs:commons`.
- The module must not import app `R`, legacy app theme classes, Hilt, POS, or Store feature
  packages. App-layer rollout wiring stays outside the module.
- Package root: `com.woocommerce.android.ui.compose.designsystem`.
- Subpackages: `foundation`, `component`, and `preview`.
- Use a separate opt-in `WooDesignSystemTheme`, Material 3-only.
- `WooDesignSystemTheme` is the migration-era wrapper name while the legacy
  `com.woocommerce.android.ui.compose.theme.WooTheme` wrapper exists. Do not introduce `WooNewTheme`.
  Future consolidation can merge wrapper/accessor naming after the legacy wrapper is removed.
- `WooDesignSystemTheme` installs the Store design-system runtime; `WooTheme.*` is the
  component-facing accessor for theme-scoped foundation values.
- `WooDesignSystemThemeWithBackground` uses the real design-system foundation for explicitly
  migrated screens.
- The legacy Store theme root remains app-owned until the controlled rename boundary in
  [rollout-direction.md](rollout-direction.md).
- The design-system module does not read app resources directly.
- Migrated first-wave screens should cover the design-system root in light and dark mode.
- New design-system foundations, components, preview catalog entries, and first-wave screen updates
  should use `androidx.compose.ui.tooling.preview.PreviewLightDark` for light/dark previews.
- Design-system component previews should wrap content in `WooDesignSystemTheme`, not the legacy
  Store theme root.
- Preview coverage is required for every component; screenshot verification is required for first-wave screens and high-risk components.
- Production components need accessibility review before product-screen adoption.
- Fragment-hosted Compose layout migration means replacing XML/View layout content with Compose while
  keeping Fragments, XML nav graphs, SafeArgs, ViewModels, navigation, and Store app event ownership.
- Compose layout migration is optional per screen, not required for all screens.
- Some screens require substantial work and should stay XML/View while receiving targeted token/style updates when needed.
- Agents may proceed on assigned first-wave screens, while documenting risk signals and preserved behavior.
- Agents should assess and recommend before migrating unassigned heavy screens.
- Agents must include a short candidate assessment in migration or adoption output.
- Add example docs only when first-wave migrations produce reusable findings.

## Considered Approaches

These remain rejected for i1:

- Global replacement of existing `Woo*`, `WC*`, XML styles, theme resources, and resource names.
- A shared or cross-app design-system module, including putting Store foundations in `:libs:commons`.
- A generated-token pipeline before manual i1 Android tokens exist.
- Mandatory Compose layout migration for every Store screen.
- A full app rewrite as part of the UI migration.
- A single toolbar implementation forced across XML-heavy and Compose-owned screens.

## Screen Migration Candidate Criteria

For first-wave screens, use [rollout-direction.md](rollout-direction.md) as the assignment source.
For future unassigned screens, use the
[Candidate Assessment](screen-migration-playbook.md#candidate-assessment) section in the migration
playbook.

## AI-Agent Documentation

Design-system docs:

- `docs/design-system/rollout-direction.md`
- `docs/design-system/android-adapter.md`
- `docs/design-system/token-map.md`
- `docs/design-system/component-catalog.md`
- `docs/design-system/material3-reference.md`
- `docs/design-system/screen-migration-playbook.md`
- `docs/design-system/implementation-plan.md`

Docs should point to `rollout-direction.md` for scope instead of duplicating the in/out table.

## Resolved Open Question

The earlier rejection of a standalone module is superseded by the accepted Store-only module
boundary: `:libs:store-design-system` owns pure foundations/components.

The earlier open migration strategy is resolved by [rollout-direction.md](rollout-direction.md).

The earlier root/foundation-selection strategy is superseded by the explicit-root rollout direction
in [rollout-direction.md](rollout-direction.md). Historical root-selection notes in this checkpoint
should be read as preserved planning context, not the current rollout contract.
