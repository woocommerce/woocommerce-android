# Woo Mobile Design System Integration Checkpoint

This checkpoint preserves the planning context for the Store Management App design-system integration. It is not the final implementation plan.

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

- Use an Android **Design System Adapter**, not a global app rewrite.
- Keep the adapter inside the existing Store app UI/theme/resource layer for i1.
- Add new design-system foundations and components as opt-in APIs.
- Do not globally remap existing `Woo*`, `WC*`, XML styles, or resource names yet.
- Figma is the design-intent source of truth; Android owns the runtime API contract.
- Manually define stable i1 Kotlin/Compose runtime tokens first.
- Structure tokens and docs so a future Figma generation pipeline can update adapter internals later.
- Do not expose raw Figma variable names as public Android APIs.
- Keep a strict token map with Android API/token name, source shorthand or Figma variable/name/ID
  when useful, light and dark values, Material 3 role mapping, status, and notes.
- Public `WooTheme.colors` exposes source-backed PR 2 color tokens used by the core foundation and
  inspected i1 component nodes, grouped shallowly by source intent.
- Do not limit `WooTheme.colors` to a small curated Material 3-like subset.
- Material 3 color roles remain internal interop projection aliases, not the Store authoring surface.
- Do not expose generated Material aliases such as fixed roles or surface-container aliases unless
  they are real source-backed tokens.
- Do not add `WooTheme.semanticColors`; supported status, alert, overlay, and palette colors live as
  grouped fields under `WooTheme.colors`.
- Do not expose manual `Semantic/*.tokens.json` groups in PR 2 unless a concrete Figma component node
  is confirmed to bind to that token group.
- `outline` and `outlineVariant` are source-backed and public under `WooTheme.colors`.
- i1 foundations start Kotlin/Compose-only; do not create Android XML resources for design-system tokens until XML/View needs them.
- When XML/View needs a design-system token, move that token's primitive value to Android resources and update Compose to read from the same resource.
- Do not keep parallel Kotlin/Compose and XML resource definitions for the same token primitive values.
- Design-system XML/View styles should not be globally applied in PR 2; add targeted XML/View style usage only when a non-migrated XML/View screen needs design-system styling.
- Code should publicly expose only production-ready tokens/components.
- In-progress i1 areas can be documented, tracked, or preview-only until stable.
- Preview-only components should not be exposed as reusable product-screen APIs.
- Preview-only implementations should stay private/internal to catalog or preview files under `designsystem.preview`.
- New design-system components are Compose-first and Material 3-only.
- Existing Material 2 usage can remain until touched.
- Use Material 3 wrappers as the default implementation strategy; build custom components only when Material 3 is too different.
- Component PR scope is full i1 catalog with previews, but production APIs only for pilot-needed components and low-risk primitives.
- Unsettled components stay private/internal preview catalog implementations.
- Initial production subset covers top/navigation bar, page title/body/link text styles or wrappers, primary button, settings cell/row, section header, switch, icon button, divider, progress indicator, and the tokens they depend on.
- Progress indicator is not listed as an i1 Figma component, but should still be wrapped as a thin Material 3 adapter for future custom design replacement.
- Do not add more thin Material 3 wrappers beyond the initial production subset unless a later design-system decision explicitly expands the catalog.
- Component names use the `Woo` prefix inside the design-system package.
- Do not use `WooDs*` or `WC*` for new design-system components.
- Package root: `com.woocommerce.android.ui.compose.designsystem`.
- Suggested subpackages: `foundation`, `component`, and `preview`.
- Use a separate opt-in `WooDesignSystemTheme`, Material 3-only.
- `WooDesignSystemTheme` is the migration-era wrapper name while the legacy
  `com.woocommerce.android.ui.compose.theme.WooTheme` wrapper exists. Do not introduce `WooNewTheme`.
  Future consolidation can merge wrapper/accessor naming after the legacy wrapper is removed.
- `WooDesignSystemTheme` installs the Store design-system runtime; `WooTheme.*` is the
  component-facing accessor for theme-scoped foundation values.
- The new `WooTheme` accessor lives under the design-system package. This intentionally accepts temporary
  simple-name overlap with the legacy `com.woocommerce.android.ui.compose.theme.WooTheme` wrapper until the
  legacy wrapper is removed; new design-system code should not import the legacy wrapper.
- `WooTheme.colors` is the canonical Store authoring surface for source-backed PR 2 color tokens used
  by the core foundation and inspected i1 component nodes.
- Group colors shallowly by source intent, such as core, background, surface, outline, status,
  overlay, alert, and palette.
- `MaterialTheme.colorScheme` remains populated from source values as an internal interop projection
  for Material 3 components, defaults, and helpers.
- `WooTheme.text` exposes the Store text-role model, including regular, emphasized, and strong variants.
  `MaterialTheme.typography` is the regular Material 3 projection.
- Public palette/ramp and alert tokens do not automatically approve foreground/background pairing;
  component-specific contrast is still required before using them for text, essential icons, or
  required state communication.
- PR 2 foundation scope includes color, typography, spacing, radius, elevation, icon sizing, and interaction/state tokens.
- Add an explicit theme selector to `composeView`, defaulting to the legacy app theme.
- New design-system foundations, components, preview catalog entries, and pilot updates should use `androidx.compose.ui.tooling.preview.PreviewLightDark` for light/dark previews.
- Design-system previews should wrap content in `WooDesignSystemTheme`, not `WooThemeWithBackground`.
- Pilot previews should use the same theme that the screen opts into at runtime.
- Preview coverage is required for every component; screenshot verification is required for pilot screens and high-risk components.
- Production components need accessibility review before product-screen adoption.
- Pilot screens need an accessibility regression check against the original screen.
- Migrated design-system screens opt into the design-system theme at the Fragment Compose root.
- Fragment-hosted Compose layout migration means replacing XML/View layout content with Compose while keeping Fragments, XML nav graphs, SafeArgs, ViewModels, navigation, and Store app event ownership.
- Compose layout migration is optional per screen, not required for all screens.
- Some screens require substantial work and should stay XML/View while receiving targeted token/style updates when needed.
- Agents should assess and recommend, but must ask the user before migrating heavy screens.
- Agents may proceed on assigned screens that pass the candidate checklist; if high-risk signals appear during exploration, they stop and ask before editing.
- Agents must include a short candidate assessment in migration or adoption output.
- Pilot docs should record post-implementation findings only when there is non-trivial learning to preserve.
- First existing-Compose pilot: update `PrivacySettingsFragment` to consume the opt-in design-system layer without changing its hosting model or product behavior.
- First XML/View pilot: migrate `FeedbackCompletedFragment` from XML/View layout to Fragment-hosted Compose and consume the new opt-in design-system layer.
- The pilots should become the AI-agent templates for the two likely outcomes.
- Pilot order: run `PrivacySettingsFragment` first, then `FeedbackCompletedFragment`, after foundations/components/previews and the `composeView` theme selector are available.

## Considered Approaches

These were rejected for i1:

- Global replacement of existing `Woo*`, `WC*`, XML styles, theme resources, and resource names.
- A standalone design-system module before proving the Android mapping in the Store app.
- A generated-token pipeline before manual i1 Android tokens exist.
- Mandatory Compose layout migration for every Store screen.

## Screen Migration Candidate Criteria

A good candidate:

- Is a low-risk product surface.
- Has visible design-system value such as toolbar, text hierarchy, cells, buttons, banners, empty/loading states, or forms.
- Has bounded state and navigation.
- Can be verified with previews and screenshots in light and dark mode.
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

## AI-Agent Documentation Needed

Planned docs folder:

- `docs/design-system/android-adapter.md`
- `docs/design-system/token-map.md`
- `docs/design-system/component-catalog.md`
- `docs/design-system/material3-reference.md`
- `docs/design-system/screen-migration-playbook.md`
- `docs/design-system/implementation-plan.md`
- `docs/design-system/pilot-feedback-completed.md`
- `docs/design-system/pilot-privacy-settings.md`

Docs should start with candidate assessment before migration steps so agents do not treat Compose layout migration as mandatory.

## Resolved Open Question

The `docs/design-system/` skeleton was created before implementation so agents have stable constraints to follow.

Created docs:

- `docs/design-system/android-adapter.md`
- `docs/design-system/token-map.md`
- `docs/design-system/component-catalog.md`
- `docs/design-system/material3-reference.md`
- `docs/design-system/screen-migration-playbook.md`
- `docs/design-system/implementation-plan.md`
- `docs/design-system/pilot-feedback-completed.md`
- `docs/design-system/pilot-privacy-settings.md`

These files are intentionally concise. Component names, token values, screenshots, and code examples should be filled during foundation implementation and the pilot.
