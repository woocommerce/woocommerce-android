# Store Design System Screen Migration Playbook

This playbook is for AI-assisted Store Management App screen migrations.

Migration is optional per screen. Do not assume every XML/View screen should be migrated to Compose.

There are three supported adoption outcomes:

- XML/View layout to Fragment-hosted Compose layout migration.
- Existing Compose screen adopting the design-system theme, tokens, and components.
- Retained XML/View screen adopting a targeted design-system bridge.

## Candidate Assessment

Assess the screen before editing code.

A good candidate:

- Is a low-risk product surface.
- Has visible design-system value: toolbar, text hierarchy, cells, buttons, banners, empty/loading states, or forms.
- Has bounded state and navigation.
- Can be verified with previews and screenshots in light and dark mode across both
  legacy-compatible and design-system foundations.
- Avoids RecyclerView behavior, selection tracking, `ActionMode`, complex custom Views, or major accessibility redesign.
- Has a clear before/after baseline for AI agents.

High-risk screens require explicit confirmation before editing. High-risk signals include:

- RecyclerView, ListAdapter, PagingDataAdapter, or adapter-heavy migration.
- Selection tracking or `ActionMode`.
- Complex custom Views or compound widgets.
- Shared element transitions or complicated animation behavior.
- Embedded WebView, camera, barcode scanner, media picker, or payment/card-reader UI.
- Product, order, payment editing, or fulfillment flows.
- Many navigation branches or multiple child fragments.
- No reliable preview or screenshot baseline.

If the screen is high-risk, stop and ask whether it should be migrated, updated through the XML/View
design-system bridge, partially updated with narrow Compose islands, or deferred.

Agents do not decide to migrate heavy screens on their own. Agents should assess the screen, explain the risk, recommend an option, and ask the user before editing when substantial migration work is likely.

If the user explicitly assigns a specific screen and it passes the candidate checklist, the agent may proceed without asking again. If high-risk signals appear during exploration, stop and ask before editing.

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

## Existing Compose Adoption Boundary

Compose Design System Adoption means:

- Keep the existing Fragment or dialog host.
- Keep existing ViewModels, navigation, events, analytics, strings, and product behavior.
- Replace legacy/current Compose styling with production-ready design-system components and
  `WooTheme.*` foundations.
- Preserve or improve existing preview coverage.

It does not mean:

- Rewriting the screen's state model.
- Moving navigation into Compose.
- Migrating unrelated legacy components.
- Changing product copy or behavior without an explicit product decision.

## XML/View Design-System Bridge Boundary

Some high-traffic screens are too risky for a full Compose migration but still need visual alignment
with the design-system rollout. Product lists, order lists, editing flows, adapter-heavy screens, and
screens with custom Views may fall into this category.

XML/View bridge adoption means:

- Keep the existing Fragment, XML nav graph, ViewModel, adapters, custom Views, analytics, strings,
  and product behavior.
- Keep the screen primarily XML/View.
- Apply a screen-level theme-overlay bridge only at an opted-in screen root.
- Prefer an `onGetLayoutInflater(...)` helper for Fragment roots that already use
  `BaseFragment(R.layout...)`; per-layout inflate helpers are secondary conveniences.
- Use the shared design-system rollout mode rather than a Compose-only or XML-only selector.
- Use Material/theme attrs first for broad foundation roles such as surface, on-surface, primary,
  error, shape, and text appearances.
- Add custom Woo design-system attrs or promoted Android resources only for semantic gaps proven by
  the target screen.
- Convert only the visible direct-resource gaps needed by the target screen.
- Verify before/after screenshots in light and dark mode.

It does not mean:

- Global app theme replacement.
- Global remapping of all existing XML `Woo.*`, `TextAppearance.Woo.*`, color, typography, or widget
  styles.
- A parallel XML design system that duplicates Compose-owned token primitive values.
- A product list, order list, or editing-flow redesign.
- Silent restyling of unaudited XML/View screens.

The bridge is useful but limited. Theme overlays solve attribute resolution; they do not affect
styles, layouts, drawables, selectors, or custom views that hardcode `@color`, `@dimen`, concrete
`TextAppearance`, or programmatic resource lookups. Each retained XML/View screen needs a short audit
before opting in.

Use a Green/Yellow/Red bridge assessment:

- Green: root and rows inherit context cleanly; visible styles are mostly attr-driven. Use the
  overlay helper and minimal style updates.
- Yellow: context mostly propagates, but a few direct resources, drawables, or custom views block
  parity. Use the overlay plus targeted attr/resource conversion.
- Red: many stale inflaters, programmatic colors, custom views, direct drawables, fragile layout
  behavior, or broad row rewrites are required. Avoid a wholesale bridge; defer, migrate later, or
  use a narrow Compose island for a clean component boundary.

Before bridge adoption, audit:

- Whether the Fragment root can be inflated through a cloned inflater with a theme overlay.
- Whether adapters inflate rows from `parent.context`.
- Whether custom views inflate children from their constructor context.
- Whether dialogs, popup windows, menus, toolbar action views, or child fragments inflate with an
  unrelated context.
- Which visible styles use direct `@color`, `@dimen`, concrete drawables, or programmatic resource
  lookups.
- Whether dark mode has coherent values for every new attr or promoted resource.

Rows should inherit the overlaid parent context naturally through `LayoutInflater.from(parent.context)`.
Do not pass wrapped contexts through adapters manually unless a screen-specific audit shows no safer
option.

Use narrow Compose islands only when an isolated XML subcomponent would need more styling work than a
clean replacement, such as a banner, empty state, chip group, status block, or CTA section.

The first retained XML/View bridge pilot should be chosen during that PR's planning. Pick a screen
that is complex enough to exercise real bridge mechanics but low traffic enough to keep blast radius
small. Prefer at least two representative signals: XML Fragment root, adapter row inflation, custom
`Woo.*` XML styles, toolbar/chrome, empty/loading state, light/dark sensitivity, or a small
direct-resource gap. Avoid product/order/payment editing, scanners, WebView, heavy selection flows,
and broad product or order list redesign for the first bridge pilot.

## Rollout Boundary

Migrated screens use one design-system component tree. Do not create permanent `LegacyScreen` and
`DesignSystemScreen` implementations for ordinary migrations.

- Default/no explicit `composeView` theme follows `FeatureFlag.NEW_DESIGN_SYSTEM`.
- Flag off renders the screen under `WooThemeWithBackground` with a legacy-compatible
  design-system foundation.
- Flag on renders the same screen under `WooDesignSystemThemeWithBackground` with the real
  design-system foundation.
- Explicit `DesignSystemMode.LEGACY` forces the legacy-compatible foundation.
- Explicit `DesignSystemMode.DESIGN_SYSTEM` forces the real design-system foundation.
- Screen code should not branch between legacy and design-system UI trees.
- Temporary full-screen fallbacks are allowed only for genuinely high-risk migrations and must include
  an expiry/removal plan.

`DesignSystemMode` is the shared rollout selector for Compose roots and XML/View bridge roots. If a
branch still uses the earlier Compose-only `ComposeTheme` name, rename and move it when XML/View bridge
support is added instead of adding a second XML selector.

Design-system components must render under both foundations. If a component only works under
`WooDesignSystemThemeWithBackground`, fix the foundation/component contract instead of adding a
per-screen fallback.

## Bridge Components

Top app bar/chrome migration is a bridge-component concern. Moving from the Activity toolbar to
Compose `WooTopAppBar` changes ownership and structure, not only colors.

- A retained XML/View screen-level bridge affects only the opted-in content subtree. Activity-owned
  toolbar/chrome inflated outside that subtree is out of scope for the bridge and should be handled
  by a separate scoped chrome decision.
- Prefer component-level compatibility driven by the active foundation.
- For a simple retained XML/View screen with no menu, search, or collapsing behavior, a per-screen
  Compose `WooTopAppBar` island above the retained XML body is acceptable. Hide the Activity toolbar
  with `AppBarStatus.Hidden`, keep the XML content bridged at the inflater boundary, and configure the
  top bar through the same default design-system foundation path used by `composeView`.
- Under the legacy-compatible foundation, `WooTopAppBar` should stay close to the existing Activity
  toolbar for title alignment, title typography, nav icon treatment, action colors,
  divider/elevation, height, and insets.
- Under the design-system foundation, `WooTopAppBar` should render the real design-system app bar.
- Prefer a Compose `WooTopAppBar` island over XML toolbar reimplementation unless a later decision
  explicitly chooses an XML/View chrome adapter.
- Do not treat simple-toolbar adoption as proof for menu, search, or collapsing toolbar screens.
  Those need a dedicated audit or pilot because View menus and `SearchView` behavior must be mapped to
  `WooTopAppBarAction` and Compose search affordances, and may expose component API gaps. The likely
  direction for those pilots is still per-screen Compose chrome ownership: hide the Activity toolbar
  for that screen and render `WooTopAppBar` or related Compose chrome in the screen. Do not reopen
  Activity-owned toolbar bridging as the default just because the screen has additional toolbar-owned
  affordances.
- Do not add a duplicate screen implementation just to preserve legacy toolbar chrome.

## Workflow

1. Capture the baseline: current layout, Fragment/dialog host, navigation, analytics, strings, images, and accessibility.
2. Decide whether the screen is a good candidate and which adoption outcome applies.
3. Keep the existing host. For XML/View migration, replace the layout with `composeView`; for existing
   Compose adoption, keep the current Compose root; for XML/View bridge adoption, keep the XML root
   and inflate it through the approved screen-level theme-overlay helper.
4. Keep one screen implementation. Let the default `composeView` mode follow the feature flag unless
   the task explicitly requires forcing `DesignSystemMode.LEGACY` or
   `DesignSystemMode.DESIGN_SYSTEM`. If a pilot opts into the design-system mode, do it only at the
   approved root boundary: the Compose root for Compose screens, or the inflater boundary for retained
   XML/View bridge screens.
5. If the adopted screen replaces the activity toolbar, set the Fragment's `activityAppBarStatus` to `AppBarStatus.Hidden`,
   render `WooTopAppBar` inside the screen `Scaffold` or a narrow ComposeView island above retained XML content, and pass
   navigation callbacks from the Fragment. Use `WindowInsets(0)` for the Compose top bar unless the screen is intentionally
   edge-to-edge, so a Fragment-hosted screen does not add a second status-bar inset. Defer menu/search/collapsing toolbar
   screens to a dedicated audit or pilot rather than fitting them into a simple top-bar island; their likely direction is
   the same per-screen Compose chrome ownership with additional menu/search/collapsing behavior mapping.
6. Treat source examples as component and token guidance, not as a mandate to copy an iOS table layout onto Android. Preserve the
   screen's existing surface/background relationship unless design explicitly asks for a stronger grouping treatment. Rows that
   use surface-keyed content colors should sit on `WooTheme.colors.surface.default`; avoid adding new cards or dividers unless
   the screen already had that hierarchy or the migration needs it for clarity.
7. Build a stateless screen composable and a VM-aware overload only if the screen needs ViewModel state.
8. Use design-system components and `WooTheme.*` foundations where they are production-ready.
9. Do not use preview-only components in production screens. Preview-only implementations should not have public APIs intended for product-screen imports.
10. Preserve existing string resources and analytics event names unless the migration explicitly requires a product copy or tracking change.
11. Add previews for the migrated screen in both foundation states: legacy-compatible light,
   legacy-compatible dark, design-system light, and design-system dark. Add font scale, RTL, or
   orientation previews when the screen is sensitive to those dimensions.
12. Verify pilot screens with screenshot review, targeted tests when behavior changes, and an
   accessibility regression check against the original screen.
   When using the Android preview screenshot plugin, prefer target-filtered commands such as
   `--tests '*ScreenName*'` for update/validate runs so a pilot does not force unrelated screenshot
   references into the same PR. For legacy-compatible dark previews, set preview `uiMode` to night
   instead of only passing `useDarkTheme = true`; the legacy-compatible foundation can depend on
   Android night resources as well as Compose dark-theme state.
13. Verify design-system components under `WooThemeWithBackground` do not fall back to static
   `LightWooColors` or other hardcoded light defaults.
14. Verify existing non-migrated screens using `WooThemeWithBackground` do not visually regress.
15. Verify `composeView` behavior: default/no explicit opt-in follows the feature flag, explicit
   `DesignSystemMode.DESIGN_SYSTEM` forces the real design-system foundation, and explicit
   `DesignSystemMode.LEGACY` uses the legacy-compatible foundation.
16. For XML/View bridge adoption, verify the bridge behavior: default/no explicit opt-in follows
   `FeatureFlag.NEW_DESIGN_SYSTEM`, the legacy path preserves existing View styling, the design-system
   path is scoped to the opted-in screen root, RecyclerView rows inherit through `parent.context`,
   and sibling screens are not restyled.

## Android Migration Skill

When migrating XML/View layouts to Compose, use the Android skill as a workflow reference:

`https://github.com/android/skills/tree/main/jetpack-compose/migration/migrate-xml-views-to-jetpack-compose`

Use it as a per-screen checklist. It is not a mandate to migrate every screen.

## Agent Output Requirements

For each migrated or adopted screen, an agent must report a short candidate assessment:

- Why the screen was a good candidate.
- Which adoption outcome was used.
- Risk signals checked.
- What behavior was intentionally preserved.
- What design-system components and `WooTheme` foundations were used.
- Whether the screen uses a single design-system component tree in both foundation states.
- What stayed in XML/View or legacy Compose and why.
- For XML/View bridge adoption, the Green/Yellow/Red bridge assessment and the inflation/style audit
  summary.
- Preview coverage added, including both foundation states when applicable.
- Verification performed.
- Accessibility checks performed.
- Any follow-up needed before broader migration.
