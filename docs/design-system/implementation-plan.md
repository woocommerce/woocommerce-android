# Store Design System Implementation Plan

This plan sequences the Woo Mobile Design System i1 adapter work for trunk-based delivery.

The goal is to avoid a long-lived branch, avoid global app changes, and keep product-screen adoption behind opt-in APIs and pilot validation.

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
- Manual i1 Kotlin/Compose runtime tokens.
- Foundation groups for color, typography, spacing, radius, elevation, icon sizing, and interaction/state tokens.
- Curated production color roles through `WooTheme.colors`, projected into `MaterialTheme.colorScheme`.
- Full text roles through `WooTheme.text`, with the regular projection installed in `MaterialTheme.typography`.
- Spacing and padding through `WooTheme.spacing` and `WooTheme.padding`.
- Woo-specific semantic colors added to `WooTheme.colors` only when necessary and approved.
- Internal adapter tokens for Figma variables with no clean Material 3 role, with public exposure only when needed by production components or pilots.
- Kotlin/Compose-owned token primitive values for i1 foundations.
- No Android XML resources for design-system tokens until a real XML/View use case needs them.
- Promotion rule: when XML/View needs a token, move that token's primitive value to Android resources and update Compose to read from the same resource.
- No parallel Kotlin/Compose and XML resource definitions for the same token primitive values.
- Preview wrappers.
- Token map entries for implemented foundations.
- Light and dark previews using `@PreviewLightDark`.
- Design-system previews wrapped in `WooDesignSystemTheme`.

Keep this PR opt-in only. Do not globally remap existing app theme resources.

Do not globally apply design-system XML/View styles in PR 2. Add targeted XML/View style usage only when a non-migrated XML/View screen needs design-system styling.

### 3. Components and Preview Catalog

Implement the full i1 component catalog with previews.

Expected output:

- Compose-first, Material 3-only design-system components under `com.woocommerce.android.ui.compose.designsystem`.
- `Woo` component naming.
- Material 3 wrappers where the mapping is close.
- Custom components only where Material 3 is materially different.
- Component catalog status updates.
- Light and dark previews for every component using `@PreviewLightDark`.
- Production APIs for the subset needed by the two pilots and low-risk primitives.
- Private/internal preview catalog implementations for unsettled components.

Production screens should consume only production-ready components. In-progress components can remain preview-only.

Preview-only implementations should stay private/internal to the catalog or preview files, preferably under `designsystem.preview`, so migration agents do not import unsettled APIs into product screens.

The initial production subset should cover top/navigation bar, page title/body/link text styles or wrappers, primary button, settings cell/row, section header, switch, icon button, divider, progress indicator, and the spacing/radius/color/typography tokens they depend on.

Production components should read approved foundations through `WooTheme.*`. Material 3 defaults may use
`MaterialTheme` internally as an interop projection.

Progress indicator is not listed as an i1 Figma component. Include it as a thin Material 3 wrapper so future custom loading/progress design can replace the implementation behind the adapter.

Do not add more thin Material 3 wrappers beyond the initial production subset unless a later design-system decision explicitly expands the catalog.

### 4. `composeView` Theme Selector

Add explicit theme selection to the Store app `composeView` helper.

Expected output:

- Existing calls continue to use the legacy app theme by default.
- Design-system screens can opt in at the Fragment Compose root.
- No behavior change for existing screens.

### 5. Existing-Compose Pilot: Privacy Settings

Update `PrivacySettingsFragment` to consume the opt-in design-system layer.

Expected output:

- Existing Fragment hosting remains.
- Existing ViewModel, navigation, events, analytics, strings, and behavior remain.
- Existing previews are preserved or improved.
- Real findings are added to the screen migration playbook.

### 6. XML/View Pilot: Feedback Completed

Migrate `FeedbackCompletedFragment` from XML/View layout to Fragment-hosted Compose.

Expected output:

- Existing Fragment, nav graph destinations, SafeArgs, analytics, strings, and behavior remain.
- The screen opts into the design-system theme at the Compose root.
- The XML layout is removed only after the Compose replacement is verified.
- Real findings are added to the screen migration playbook.

### 7. AI Migration Playbook Updates

Update docs based on the two pilots before asking AI agents to migrate additional screens.

Expected output:

- Concrete examples from both adoption outcomes.
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
- Agents must ask before migrating heavy screens.
- Agents may proceed on assigned screens that pass the candidate checklist.
- Agents must include a short candidate assessment in migration or adoption output.
- No production screen should consume preview-only components.
- Preview-only components should not be exposed as reusable production-screen APIs.
- New design-system previews should use `androidx.compose.ui.tooling.preview.PreviewLightDark`.
- New design-system previews should wrap content in `WooDesignSystemTheme`, not `WooThemeWithBackground`.
- Screenshot verification is required for pilot screens and high-risk components, not for every small primitive component.
- Production components need accessibility review before product-screen adoption.
- Pilot screens need an accessibility regression check against the original screen.
- Each PR should be reviewable independently.
