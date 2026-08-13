# Store Design System Component Catalog

This catalog tracks Woo Mobile Design System i1 components for Android.

Production design-system foundations and components live in the Store-only `:libs:store-design-system`
module under `com.woocommerce.android.ui.compose.designsystem.*`.

When comparing catalog components to Figma, use [figma-navigation.md](figma-navigation.md) to
discover source pages and nodes from live Figma metadata instead of relying on local work notes.

The component catalog has three boundaries:

- Figma-backed public production APIs that Store screens may import from `designsystem.component`.
- Public adapter utilities that wrap Material 3 or platform primitives with Store Design System
  tokens, but are not one-to-one Figma components.
- Preview-only samples under `designsystem.preview` for catalog coverage while design, app-shell,
  navigation, or data semantics remain unsettled.

The module must not import app `R`, legacy app theme classes, app modifiers, Hilt, feature packages,
POS APIs, or `WooThemeWithBackground`.

## Figma-Backed Production API Scope

The current module exposes Figma-backed production Compose APIs for:

- Badges.
- Fill, Tonal, and Outlined buttons in medium and small sizes.
- Generic cells and settings rows; Figma `Cell Content` maps to internal row content styling.
- Checkbox, radio button, and filter chip controls.
- Horizontal and vertical dividers.
- Icon containers.
- Static notice banners.
- Page headers.
- Controlled search fields.
- Label-only segment controls with two to five options.
- Modal bottom sheets with Woo-owned styling and caller-owned state/content.
- Top text tabs.
- Design-system top app bars with descriptor actions.

Figma-backed production components must render correctly under the design-system root and must not
rely on static light fallback defaults. Non-migrated screens stay on the legacy root and should not
consume production design-system components until explicitly migrated. Production components read
foundation values through `WooTheme.*`. Prefer `WooTheme.radius` and `WooTheme.iconSize` for
design-system-owned shapes and icon sizes; use Material 3 defaults only where the Material component
API owns the internal behavior.

## Public Adapter Utility Scope

The module also exposes public utility wrappers that are useful for composing Figma-backed
components, but should not be cited as Figma-backed component implementations:

- Plain and outlined icon buttons. Figma has a `navigation-button` treatment; `WooIconButton` and
  `WooOutlinedIconButton` are Material 3 / token adapters used by page headers, top app bars, and
  screen-specific actions.
- Controlled switches. `WooSwitch` is a Material 3 / token adapter used by settings rows.
- Switch settings rows compose the Figma-backed cell layout with the `WooSwitch` Material 3 /
  token adapter.
- Linear and circular progress indicators. These are thin Material 3 wrappers scoped to Store Design
  System colors and sizing.
- `WooDesignSystemToolbar`, `Widget.Woo.DesignSystem.Toolbar`, and
  `ThemeOverlay.Woo.DesignSystem.Toolbar` are XML-facing toolbar bridge APIs for legacy-heavy
  screens that opt into design-system chrome.

## Module-Safe Port Decisions

The broad component branch predated the standalone module and included app-local conveniences. The
split module keeps the component API clean:

- Preview icons and component-owned search/close icons are module-local vector drawables.
- Component previews use `WooDesignSystemTheme` or `WooDesignSystemThemeWithBackground` from the
  library module.
- The Developer Options entry hosts the same module catalog screen through app-level navigation.
- The old `WooThemeWithBackground` legacy-compatible top-app-bar path is not ported into the module.
- `WooTopAppBar` is a design-system-only top app bar.
- XML toolbar convergence is library-owned component infrastructure. The toolbar decorates rendered
  icon actions in place so inflated menus, collapsed `SearchView` triggers, title, navigation,
  overflow, and custom action-view ownership remain AppCompat-owned.
- Product-screen XML toolbar adoption remains migration work; the component split provides the bridge
  and catalog coverage only.
- App screenshot catalog wiring from the broad branch is not moved in this port. The module currently
  provides Compose preview coverage and an interactive Developer Options catalog; screenshot wiring
  can be added separately when the module has an approved screenshot-test setup.

## Preview Standard

Use `androidx.compose.ui.tooling.preview.PreviewLightDark` for design-system component previews.

Design-system component previews should wrap content in `WooDesignSystemTheme`. Full catalog and
foundation previews may use `WooDesignSystemThemeWithBackground` when a background surface is useful.
Migrate app screen previews separately; do not make the library depend on app preview annotations or
legacy app themes.

Design-system component previews should wrap content in `WooDesignSystemTheme`, not
`WooThemeWithBackground`. Migrated screen previews should cover the design-system root in light and
dark mode.

Preview coverage exists for the production components and for preview-only catalog samples. Screenshot
verification remains required for first-wave screens and high-risk component adoptions, but the
component split does not add module screenshot infrastructure.

## Status Values

- `production`: Stable API, visual states, accessibility behavior, and previews.
- `material_adapter`: Public Material 3 / token adapter; useful in DS composition but not a
  one-to-one Figma component.
- `not_public`: Search/discovery can find a source, but agents should not treat it as public
  production component inventory.
- `preview_only`: Implemented for catalog/design review only; not a reusable production-screen API.
- `needs_design`: Design source or promotion decision is not signed off.
- `needs_android_mapping`: Android API or Material 3 mapping is not resolved for production adoption.

## Catalog

| Component | Android API | Status | Notes |
| --- | --- | --- | --- |
| Badges | `WooBadge`, `WooBadgeTone`, `WooBadgeColors`, `WooBadgeDefaults` | production | Compact label badge with optional decorative leading icon and status tones including `NeutralOutlined`. Prefer semantic tones; use `WooBadgeDefaults.colors(...)` only when a feature must preserve an established custom palette. |
| Buttons | `WooFilledButton`, `WooFilledTonalButton`, `WooOutlinedButton`, `WooButtonSize` | production | Figma-backed Fill/Tonal/Outline treatments exposed with Material 3-aligned Filled, Filled Tonal, and Outlined API names. Supports optional leading icon, enabled/disabled state, medium/small sizes, and 48dp touch target preservation. Use one Filled button per screen, Filled Tonal for alternatives, and Outlined for low-emphasis actions. |
| Cell | `WooCell`, `WooSettingsRow` | production | Surface Bright row shell with On Surface titles, On Surface Variant descriptions/slots, and a settings convenience. Keep one row-level action; avoid duplicate child semantics. Figma `Cell Content` maps to internal row content styling, not a standalone public API. |
| Checkbox | `WooCheckbox` | production | Controlled Material 3 checkbox wrapper. Caller owns label and group semantics. |
| Chip | `WooFilterChip` | production | Controlled filter chip with optional icons. Resting uses Surface Bright plus Tint On Surface 16; selected content uses On Secondary Container. |
| Divider | `WooDivider`, `WooVerticalDivider` | production | Thin divider wrappers using Tint Layers / On Surface / Opacity-16. |
| Icon Button | `WooIconButton`, `WooOutlinedIconButton`, `WooIconButtonEmphasis` | material_adapter | Public Material 3 / token adapters. Requires non-blank content descriptions. Figma has a `navigation-button` treatment for outlined navigation actions; the generic plain/outlined icon-button APIs are Android composition utilities. |
| Icon Container | `WooIconContainer`, `WooIconContainerTone` | production | Restricted palette-tone icon box, decorative by default unless a content description is supplied. |
| Notice Banner | `WooNoticeBanner`, `WooNoticeBannerTone` | production | Static title/description banner. Dismissible/actionable/live-region behavior remains future work. |
| Page Header | `WooPageHeader`, `WooPageHeaderScrollBehavior`, `WooPageHeaderDefaults` | production | Parameter-controlled fixed 64dp and collapsible medium modes on Surface Bright, with optional right actions and a full-width tint-layer bottom divider. Material 3 owns nested scrolling, direct header drag, and decay settling. Attach `nestedScrollConnection` to the scrolling container; call the UI-confined suspending `expand()` operation for tab reselection or equivalent programmatic expansion. Newer expansion calls supersede older animations. Meaningful nested user input and fling boundaries cancel the active expansion before delegation; direct header drag and an already-running Material settle can overlap it. |
| Progress Indicator | `WooLinearProgressIndicator`, `WooCircularProgressIndicator` | material_adapter | Thin Material 3 wrappers, including determinate progress coercion. |
| Radio Button | `WooRadioButton` | production | Controlled Material 3 radio wrapper. Caller owns label and group semantics. |
| Search | `WooSearchField` | production | Controlled Surface Bright search shell with optional clear/external actions, State On Surface 24 placeholder, active On Surface icons, and `surface.surfaceDim` inner field. Search orchestration remains screen-owned. |
| Section Header | No public API | not_public | Figma search may surface `section-header`, but its master lives in `Components Playground`, not on a promoted component page. Agents should ignore it for production inventory and API work for now. |
| Switch | `WooSwitch` | material_adapter | Controlled Material 3 / token adapter with minimum touch target. Figma does not currently expose a canonical `Switch` component in the Mobile Design System library. |
| Switch Settings Row | `WooSwitchSettingsRow` | material_adapter | Composes the Figma-backed cell layout with the `WooSwitch` Material 3 / token adapter. Keep one row-level toggle action and avoid duplicate child semantics. |
| Tabs | `WooTabRow`, `WooTab` | production | Surface Bright top text tabs with Tint On Surface 16 dividers; bottom tab bar parity remains preview-only and app-shell-owned. |
| Top App Bar | `WooTopAppBar`, `WooTopAppBarAction`, `WooDesignSystemToolbar` | production | Surface Bright Android API for Figma's `Top Navigation Bar`. `WooDesignSystemToolbar` applies matching XML chrome without replacing `MenuItem.actionView`, preserving `SearchView`, custom action views, ActionMode, collapsing behavior, overflow, and menu ownership. |
| Segment Control | `WooSegmentControl` | production | Controlled, label-only radio group for two to five nonblank options. The caller owns selection. Figma has no disabled variant, so Android uses established disabled state-layer/content tokens for the whole-control fallback. |
| Modal Bottom Sheet | `WooModalBottomSheet`, `WooModalBottomSheetState`, `rememberWooModalBottomSheetState`, `WooModalBottomSheetDismisser`, `rememberWooModalBottomSheetDismisser` | production | Narrow Store adapter with a Surface Bright container, On Surface content, 16dp top corners, no visible boundary, and a 32x4dp lowest-variant handle. The caller owns composition, state, dismissal, and content; the dismisser coordinates programmatic animated dismissal, while Material owns modal/platform behavior. |
| Bottom Tab Bar | Private catalog sample | preview_only | App-shell navigation ownership remains out of component split scope. |
| Table | Private catalog sample | preview_only | Explicit Surface Bright shell with a thin outer boundary and tint-layer row dividers. Requires data model, scrolling, sorting/selection, and accessibility semantics before a public API. |

## Production Checklist

Before a component is marked `production`:

- It uses `Woo` naming inside `com.woocommerce.android.ui.compose.designsystem`.
- It is in `:libs:store-design-system` and does not depend on the app module.
- It reads production foundation values through `WooTheme.*`, except where a Material 3 API owns
  internal interop behavior.
- It has light and dark previews through `@PreviewLightDark`.
- Required enabled, disabled, long text, large font, and RTL states are covered where applicable.
- Accessibility is reviewed: semantic role, label/contentDescription rules, disabled behavior,
  minimum touch target, and font-scale resilience.
- Its token usage is represented in `docs/design-system/token-map.md` when a source-backed token is
  involved.
- It does not depend on POS APIs or POS design-system concepts.

## Preview-Only Boundary

Preview-only components may exist only as private/internal catalog or preview implementations under
`designsystem.preview`.

Do not expose preview-only components as reusable public APIs. This keeps product-screen migrations
and AI agents from importing components whose API would encode unstable design, navigation, or data
semantics.

Bottom Tab Bar and Table remain preview-only. Segment Control and Modal Bottom Sheet are production
APIs and their interactive catalog examples must use the public components.
