# Store Design System Screen Migration Playbook

This playbook is for AI-assisted Store Management App screen migrations.

Use [rollout-direction.md](rollout-direction.md) as the canonical source for first-wave scope. The
assigned first-wave surfaces are Dashboard, Products, Orders, More, top Product Detail, and top Order
Detail.

## Supported Outcomes

There are three supported outcomes:

- XML/View layout to Fragment-hosted Compose layout migration.
- Existing Compose screen adopting the design-system theme, tokens, and components.
- Targeted XML/View visual convergence for screens that remain legacy but need safe color/chrome alignment.

Migration is not mandatory for every screen. Do not infer scope for unlisted screens from the first wave.

## Candidate Assessment

Assess the screen before editing code.

If the screen is named as in scope in [rollout-direction.md](rollout-direction.md), proceed with the
appropriate migration outcome and record the risk signals found during exploration.

A good unassigned candidate:

- Is a low-risk product surface.
- Has visible design-system value: toolbar, text hierarchy, cells, buttons, banners, empty/loading states, or forms.
- Has bounded state and navigation.
- Can be verified with previews and screenshots in light and dark mode.
- Avoids RecyclerView behavior, selection tracking, `ActionMode`, complex custom Views, or major accessibility redesign.
- Has a clear before/after baseline for AI agents.

High-risk unassigned screens require explicit confirmation before editing. High-risk signals include:

- RecyclerView, ListAdapter, PagingDataAdapter, or adapter-heavy migration.
- Selection tracking or `ActionMode`.
- Complex custom Views or compound widgets.
- Shared element transitions or complicated animation behavior.
- Embedded WebView, camera, barcode scanner, media picker, or payment/card-reader UI.
- Product, order, payment editing, or fulfillment flows.
- Many navigation branches or multiple child fragments.
- No reliable preview or screenshot baseline.

For assigned first-wave heavy screens, these risks shape the migration plan; they do not
automatically block work. For unassigned screens, stop and ask whether the screen should be migrated,
partially updated with View/XML styles, or deferred.

## Migration Boundary

Fragment-hosted Compose layout migration means:

- Replace the screen layout/content with Compose.
- Keep the Fragment.
- Keep XML nav graphs and SafeArgs.
- Keep existing ViewModels.
- Keep navigation and Store app event handling in the Fragment.
- Preserve analytics and product behavior.

It does not mean:

- Compose Navigation migration.
- ViewModel rewrite.
- Feature redesign.
- POS migration.
- Global app theme replacement.
- Migrating launched child flows unless they are explicitly assigned.

## Existing Compose Adoption Boundary

Compose Design System Adoption means:

- Keep the existing Fragment or dialog host.
- Keep existing ViewModels, navigation, events, analytics, strings, and product behavior.
- Replace legacy/current Compose styling with production-ready design-system components and
  `WooTheme.*` foundations.
- Import design-system APIs from `com.woocommerce.android.ui.compose.designsystem.*`; the
  implementation is module-owned in `:libs:store-design-system` even though the package name stays
  stable for Store screens.
- Preserve or improve existing preview coverage.

It does not mean:

- Rewriting the screen's state model.
- Moving navigation into Compose.
- Migrating unrelated legacy components.
- Changing product copy or behavior without an explicit product decision.

## Targeted XML/View Visual Convergence Boundary

Targeted XML/View visual convergence means:

- Keep the existing XML/View screen implementation.
- Apply design-system-aligned color or chrome styling only where the change is low-risk.
- Consume shared module-local color resources through targeted XML/View styles when XML/View needs a
  design-system color token. Keep Compose reading the same value through `WooTheme.colors`.
- Use a DS-looking XML toolbar when toolbar ownership must remain in XML.

It does not mean:

- A global XML theme rewrite.
- Global typography, spacing, or status/semantic color replacement.
- A permanent XML bridge for an in-scope migrated screen.
- Rewriting behavior, navigation, or state.

## Rollout Boundary

Migrated first-wave screens use one design-system component tree. Do not create permanent
`LegacyScreen` and `DesignSystemScreen` implementations for ordinary migrations.

- Screen migration is explicit: migrated screens opt into the design-system root; non-migrated
  screens stay on the legacy root.
- During migration work, use a DS-specific builder such as `designSystemComposeView {}` for migrated
  screens.
- Do not add root-selection indirection to existing `composeView {}` calls. A screen is migrated by
  changing its call site to the DS root builder.
- Temporary full-screen fallbacks inside in-scope screens are allowed only for genuinely high-risk
  migration gaps and must include an expiry/removal plan.
- Out-of-scope child flows may remain legacy for this wave without an expiry plan.

Before the final merge of the migration branch, follow the controlled root-API rename boundary and
audit steps defined in [rollout-direction.md](rollout-direction.md). A legacy-compatible
design-system foundation bridge is not required by this rollout path unless a future implementation
explicitly chooses it.

## Chrome Components

Top app bar/chrome migration is a component and rollout concern. Moving from the Activity toolbar to
Compose `WooTopAppBar` changes ownership and structure, not only colors.

The current toolbar direction is a unified design-system visual look, not one forced implementation:

- Compose-owned screens use `WooTopAppBar`.
- Heavy XML screens may keep XML toolbar ownership if the toolbar matches the design-system look.
- For migrated Compose-owned screens, `WooTopAppBar` renders under the design-system root.
- For legacy-heavy screens, preserve existing toolbar ownership unless a scoped migration chooses a
  DS-looking XML toolbar or an explicit Compose chrome migration.
- Preserve `SearchView`, `ActionMode`, collapsing app bars, menu/action ownership, navigation
  ownership, and insets.
- Do not add a duplicate screen implementation just to preserve legacy toolbar chrome.

## Workflow

1. Check [rollout-direction.md](rollout-direction.md) for scope before deciding what to migrate.
2. Capture the baseline: current layout, Fragment/dialog host, navigation, analytics, strings,
   images, and accessibility.
3. Decide which adoption outcome applies.
4. Keep the existing host. For XML/View migration, replace content with Compose or embed `ComposeView`
   sections inside a compatibility XML shell. For existing Compose adoption, keep the current Compose
   root.
5. Keep one migrated screen implementation. Use an explicit DS root builder for migrated screens;
   keep non-migrated screens on the legacy root.
6. Build a stateless screen composable and a VM-aware overload only if the screen needs ViewModel state.
7. Use design-system components and `WooTheme.*` foundations where they are production-ready.
8. Do not use preview-only components in production screens. Preview-only implementations should not
   have public APIs intended for product-screen imports.
9. Preserve existing string resources and analytics event names unless the migration explicitly requires a product copy or tracking change.
10. Add previews for the migrated screen under the design-system root in light and dark mode. Add
    RTL and large-font coverage for row-heavy screens.
11. Verify first-wave screens with screenshot review, targeted tests when behavior changes, and an
    accessibility regression check against the original screen.
12. Verify design-system components do not fall back to static light defaults under the
    design-system root.
13. Before final merge, verify the controlled root-API rename boundary with the strict `rg` audits
    defined in [rollout-direction.md](rollout-direction.md).

### Segment Control and Modal Bottom Sheet adoption

- Use `WooSegmentControl` only for controlled, label-only selection with two to five options. Pass
  the selected index and update it in the caller; do not add internal selection or per-item enabled
  state. The whole-control disabled treatment is an Android fallback because Figma has no disabled
  variant.
- Keep modal sheet composition, `WooModalBottomSheetState`, dismissal callbacks, and business
  content in the screen. Use `rememberWooModalBottomSheetState()` and `WooModalBottomSheet` without
  reaching through to Material sheet types or adding screen-specific styling knobs. For animated
  programmatic dismissal, use `rememberWooModalBottomSheetDismisser()` while retaining caller-owned
  composition and business visibility.
- The Woo wrapper owns the semantic scrim color. Material owns modal gestures, scrim rendering, back
  handling, focus/pane/traversal semantics, maximum width, insets, IME, and platform behavior.
  Screen code must not reproduce those behaviors around the Woo wrapper.
- Migrating a sheet or segment control does not authorize copy, analytics, navigation, ViewModel,
  loading/error, or merchant-action changes.

## Android Migration Skill

When migrating XML/View layouts to Compose, use the Android skill as a workflow reference:

`https://github.com/android/skills/tree/main/jetpack-compose/migration/migrate-xml-views-to-jetpack-compose`

Use it as a per-screen checklist. It is not a mandate to migrate every screen.

## Agent Output Requirements

For each migrated, adopted, or visually converged screen, an agent must report a short assessment:

- Whether the screen is in the first-wave scope.
- Which adoption outcome was used.
- Risk signals checked.
- What behavior was intentionally preserved.
- What design-system components and `WooTheme` foundations were used.
- Whether the screen uses a single design-system component tree.
- What stayed in XML/View or legacy Compose and why.
- Preview coverage added, including light/dark DS-root coverage.
- Verification performed.
- Accessibility checks performed.
- Any follow-up needed before broader migration.
