# Store Design System Implementation Plan

This plan sequences the Woo Mobile Design System i1 adapter work for trunk-based delivery.

The goal is to avoid a long-lived branch, avoid global app changes, and keep product-screen adoption
deliberate and validated through pilots.

## PR Sequence

### 1. Docs and Checkpoint

Capture the agreed integration model before implementation.

Expected output:

- Design-system docs skeleton.
- Decision checkpoint with source references and rejected alternatives.
- Material 3 reference for token semantics and Compose interop.
- Candidate criteria and pilot definitions.

Keep this as one docs PR. The checkpoint, implementation plan, token map, component catalog, migration playbook, and pilot docs are one coherent decision package.

### 2. Foundations and Theme

Implement the design-system foundation layer without adopting it in product screens.

Expected output:

- `WooDesignSystemTheme`.
- Migration-era wrapper naming: use `WooDesignSystemTheme`, not `WooNewTheme`, while the legacy
  `WooTheme` wrapper exists. Future consolidation can happen after the legacy wrapper is removed.
- `WooTheme` foundation accessors for theme-scoped production APIs.
- `WooDesignSystemThemeWithBackground` providing the real design-system foundation.
- `WooThemeWithBackground` also providing design-system composition locals through a
  legacy-compatible foundation.
- Manual i1 Kotlin/Compose runtime tokens.
- Foundation groups for color, typography, spacing, radius, elevation, icon sizing, and interaction/state tokens.
- Source-backed PR 2 color tokens used by the core foundation and inspected i1 component nodes
  exposed through `WooTheme.colors`, grouped shallowly by source intent.
- Internal Material 3 `ColorScheme` projection for Material 3 components, defaults, and helpers.
- Full text roles through `WooTheme.text`, with the regular projection installed in `MaterialTheme.typography`.
- Spacing and padding through `WooTheme.spacing` and `WooTheme.padding`.
- Supported status, alert, overlay, and palette colors as grouped fields under `WooTheme.colors`;
  no separate `WooTheme.semanticColors`.
- Manual `Semantic/*.tokens.json` groups remain out of the PR 2 public API unless a concrete Figma
  component node is confirmed to bind to that token group.
- No generated public Material aliases such as fixed roles or surface-container aliases unless those
  names are real source-backed tokens.
- Palette/ramp and alert tokens documented as source-backed but requiring component-specific contrast
  checks before foreground/background pairing.
- Internal adapter tokens for unresolved non-color Figma variables, with public exposure only when
  needed by production components or pilots.
- Kotlin/Compose-owned token primitive values for i1 foundations.
- No Android XML resources for design-system tokens until a real XML/View use case needs them.
- Promotion rule: when XML/View needs a token, move that token's primitive value to Android resources and update Compose to read from the same resource.
- No parallel Kotlin/Compose and XML resource definitions for the same token primitive values.
- Preview wrappers.
- Token map entries for implemented foundations.
- Light and dark previews using `@PreviewLightDark`.
- Design-system previews wrapped in `WooDesignSystemTheme`.
- Foundation previews or tests proving design-system components do not fall back to static
  `LightWooColors` when rendered under `WooThemeWithBackground`.

Keep this PR opt-in only. Do not globally remap existing app theme resources.

Do not globally apply design-system XML/View styles in PR 2. Add targeted XML/View style usage only when a non-migrated XML/View screen needs design-system styling.

### 3. Components and Preview Catalog

Implement the full i1 component catalog with previews.

Expected output:

- Compose-first, Material 3-only design-system components under the Store design-system Compose
  package. Initial Compose-only work may start under `com.woocommerce.android.ui.compose.designsystem`;
  the XML/View bridge PR should mechanically move it to `com.woocommerce.android.ui.designsystem.compose`.
- `Woo` component naming.
- Material 3 wrappers where the mapping is close.
- Custom components only where Material 3 is materially different.
- Component catalog status updates.
- Light and dark previews for every component using `@PreviewLightDark`.
- Production APIs for the subset needed by the initial pilots and low-risk primitives.
- Private/internal preview catalog implementations for unsettled components.

Production screens should consume only production-ready components. In-progress components can remain preview-only.

Preview-only implementations should stay private/internal to the catalog or preview files, preferably
under `designsystem.compose.preview`, so migration agents do not import unsettled APIs into product
screens.

The initial production subset should cover top/navigation bar, page title/body/link text styles or wrappers, primary button, settings cell/row, section header, switch, icon button, divider, progress indicator, and the spacing/radius/color/typography tokens they depend on.

Production components should read approved foundations through `WooTheme.*`. Material 3 defaults may
use `MaterialTheme` internally as an interop projection.

Progress indicator is not listed as an i1 Figma component. Include it as a thin Material 3 wrapper so future custom loading/progress design can replace the implementation behind the adapter.

Do not add more thin Material 3 wrappers beyond the initial production subset unless a later design-system decision explicitly expands the catalog.

Bridge components such as `WooTopAppBar` may need deeper compatibility than token mapping. Prefer
component-level compatibility driven by the active foundation over screen-level duplicate
implementations.

### 4. `composeView` Mode Selector

Add explicit design-system mode selection to the Store app `composeView` helper.

Expected output:

- Default/no explicit mode selection follows `FeatureFlag.NEW_DESIGN_SYSTEM`.
- `DesignSystemMode.LEGACY` uses `WooThemeWithBackground` with the legacy-compatible design-system
  foundation.
- `DesignSystemMode.DESIGN_SYSTEM` uses `WooDesignSystemThemeWithBackground` with the real
  design-system foundation.
- If this PR initially introduced the Compose-only `ComposeTheme` name, the XML/View bridge PR
  renames and moves it to shared `DesignSystemMode`.
- Existing non-migrated screens using `WooThemeWithBackground` do not visually regress.
- No duplicate legacy/design-system screen implementations are introduced.

### 5. Existing-Compose Pilot: Privacy Settings

Update `PrivacySettingsFragment` to consume the opt-in design-system layer.

Expected output:

- Existing Fragment hosting remains.
- Existing ViewModel, navigation, events, analytics, strings, and behavior remain.
- One screen implementation uses design-system components in both foundation states.
- Previews cover legacy-compatible light/dark and design-system light/dark. Add RTL and large-font
  coverage for row-heavy content.
- Real findings are added to the screen migration playbook.

### 6. XML/View Pilot: Feedback Completed

Migrate `FeedbackCompletedFragment` from XML/View layout to Fragment-hosted Compose.

Expected output:

- Existing Fragment, nav graph destinations, SafeArgs, analytics, strings, and behavior remain.
- One screen implementation uses design-system components in both foundation states.
- The root wrapper remains controlled by `composeView` mode selection and
  `FeatureFlag.NEW_DESIGN_SYSTEM`.
- The XML layout is removed only after the Compose replacement is verified.
- Real findings are added to the screen migration playbook.

### 7. Retained XML/View Bridge Pilot

Add one pilot for a retained XML/View screen that adopts the design-system foundation through a
screen-level XML bridge instead of migrating to Compose.

Expected output:

- Select the exact pilot screen during PR planning, not in advance.
- Use a screen that is complex enough to test real XML/View bridge mechanics but low traffic enough
  to avoid putting a major commerce workflow at risk.
- Prefer a target with at least two of: XML Fragment root, adapter row inflation, custom `Woo.*` XML
  styles, toolbar/chrome, empty/loading state, light/dark sensitivity, or a small direct-resource gap.
- Avoid product/order/payment editing, scanners, WebView, heavy selection flows, and broad product or
  order list redesign.
- Move existing Compose design-system APIs from `com.woocommerce.android.ui.compose.designsystem` to
  `com.woocommerce.android.ui.designsystem.compose` as a behavior-neutral package change.
- Rename and move the Compose-only rollout selector to a shared `DesignSystemMode`, then use that
  selector for both `composeView` and XML/View bridge opt-in.
- Add XML/View bridge APIs under `com.woocommerce.android.ui.designsystem.xml`.
- The primary XML helper supports `onGetLayoutInflater(...)` so existing
  `BaseFragment(R.layout...)` screens can opt in without rewriting root inflation.
- Default/no explicit XML bridge mode follows `FeatureFlag.NEW_DESIGN_SYSTEM`; legacy mode preserves
  existing View styling; design-system mode applies only to the opted-in screen root.
- The PR7 pilot target is `MainSettingsFragment` retained XML content, with an `AppSettingsActivity`
  toolbar overlay inflated through the same `DesignSystemMode`. The toolbar overlay is Activity-wide
  for settings screens under the flag because the existing Activity owns toolbar/title/up behavior.
- Use Material/theme attrs first. Add custom Woo attrs or promoted Android resources only for
  semantic gaps proven by the pilot.
- Use promoted Android resources for token primitives needed by XML and update Compose to read those
  same resources. Do not duplicate Kotlin/Compose and XML primitive values.
- Menu, overflow, `SearchView`, and collapsing-toolbar behavior are not validated by this settings
  pilot. Later retained XML menu/search screens should likely follow the same XML toolbar overlay
  direction, but require a dedicated menu/SearchView audit and pilot before applying it.
- Preserve existing Fragment hosting, XML nav graphs, ViewModel, adapters, analytics, strings, and
  product behavior.
- Verify before/after screenshots in light and dark mode for legacy and design-system paths.
- Add only non-trivial findings to the screen migration playbook.

Keep this PR targeted. The package move, shared mode rename, XML bridge helper, and retained
XML/View pilot should be separated into reviewable commits when practical.

### 8. AI Migration Playbook Updates

Update docs based on the three pilots before asking AI agents to migrate additional screens.

Expected output:

- Concrete examples from all three adoption outcomes.
- Known pitfalls.
- Component usage examples.
- Verification checklist refinements.
- Guidance for screens that should remain XML/View.
- A clear rule that agents must ask before migrating heavy screens.
- A proceed rule for assigned screens that pass the candidate checklist.
- Required candidate assessment output for every migrated or adopted screen.
- Pilot-doc findings only when there is non-trivial learning to preserve.

## Delivery Constraints

- POS stays out of scope.
- No mandatory migration of every screen.
- No global theme replacement for i1.
- No raw Figma variable names in public Android API.
- No parallel Kotlin/Compose and XML resource definitions for the same token primitive values.
- No global design-system XML/View style application in PR 2.
- Retained XML/View bridge work is opt-in per screen and must not silently restyle sibling screens.
- The retained XML/View pilot target is selected during that PR's planning, using the low-traffic but
  complex-enough criteria above.
- Agents must ask before migrating heavy screens.
- Agents may proceed on assigned screens that pass the candidate checklist.
- Agents must include a short candidate assessment in migration or adoption output.
- No production screen should consume preview-only components.
- Preview-only components should not be exposed as reusable production-screen APIs.
- Migrated screens should not keep permanent duplicate legacy/design-system implementations.
- Temporary full-screen fallbacks are allowed only for genuinely high-risk migrations and require an
  expiry/removal plan.
- New design-system previews should use `androidx.compose.ui.tooling.preview.PreviewLightDark`.
- Design-system component previews should wrap content in `WooDesignSystemTheme`, not
  `WooThemeWithBackground`.
- Migrated screen previews should cover both `WooThemeWithBackground` legacy-compatible foundation
  and `WooDesignSystemThemeWithBackground` design-system foundation.
- Screenshot verification is required for pilot screens and high-risk components, not for every small primitive component.
- Production components need accessibility review before product-screen adoption.
- Pilot screens need an accessibility regression check against the original screen.
- Each PR should be reviewable independently.
