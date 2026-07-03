# Store Design System Rollout Direction

Status: current direction as of June 23, 2026.

This document is the canonical source for Store Management App design-system rollout scope. It
supersedes the earlier pilot-first and open-strategy framing in the design-system docs. Branches
created before this direction predate the current plan and should not be treated as scope authority.

## Direction

The rollout is a UI migration, not a full app rewrite.

The first coherent wave migrates:

- Dashboard tab surface.
- Products tab surface.
- Orders tab surface.
- More tab surface.
- Top Product Detail surface.
- Top Order Detail surface.

Each migrated screen should have one design-system UI implementation. Do not keep permanent duplicate
legacy and design-system screen trees, and do not use the XML bridge explored in earlier branches as
the migration strategy.

For heavy XML screens, full migration can still keep an XML shell when the shell is needed for
existing app compatibility. In that case, use `ComposeView` for migrated sections, rows, or content
areas, and keep XML only for compatibility ownership such as `SearchView`, `ActionMode`, collapsing
toolbar behavior, or existing Fragment integration.

## Theme Root Rollout

Screen migration is explicit: migrated screens opt into the design-system root, and non-migrated
screens stay on the legacy root. The current `composeView {}` implementation is legacy-rooted; no
runtime root switching exists yet, so this is the root contract to build rather than a change to
shipped behavior.

- During migration work, design-system screens should use an explicit DS-specific builder, such as
  `designSystemComposeView {}`.
- Existing legacy screens continue using the current legacy Compose root.
- Do not add root-selection indirection to existing `composeView {}` calls. A screen is migrated by
  changing its call site to the DS root builder.

Before the final merge of the migration branch, restore the ergonomic default API through a
controlled rename boundary:

- Rename the current legacy `composeView` to `legacyComposeView`.
- Rename the current `WooThemeWithBackground` to `LegacyWooThemeWithBackground`.
- Rename the DS-specific builder to `composeView`.
- Audit every remaining `composeView` call as intentionally migrated.
- Move every non-migrated screen to `legacyComposeView`.

Verify the rename boundary with strict `rg` audits for legacy root usage, DS root usage, and
remaining ambiguous `composeView` call sites. Do not document a legacy-compatible design-system
foundation bridge as required for this rollout path unless a future implementation explicitly
chooses that bridge.

## Detail Scope

### Product Detail

Product Detail is expected to migrate most of its top surface because the scope is smaller than Order
Detail. The top surface includes the entry detail screen, visible sections, loading/error states, and
toolbar chrome.

Launched child and edit flows are out of scope for this wave. Examples include product creation, AI
product creation, image management, categories/tags/selectors, pricing, inventory, shipping,
linked/grouped/bundle/composite editors, variations, downloads, add-ons, quantity rules, subscription
editors, reviews, custom fields, and share/webview flows.

### Order Detail

Order Detail is larger. The top surface is in scope, including the initial rendered detail hub:
status, product list, totals/custom amounts, shipping lines, refunds summary, shipping label
summary/cards, payment summary, customer summary, subscriptions, gift cards, tracking summary,
attribution, notes summary, trash action, loading/empty states, and toolbar chrome such as
previous/next/edit actions.

Launched child flows are out of scope for this wave. Examples include shipping label
creation/refund/print/customs/Woo Shipping flows, refund creation, order editing/status/address
flows, payment and card-reader flows, add note, shipment tracking add/provider/barcode flows,
receipt/printing, fulfillment, custom fields, AI thank-you-note, and product detail launched from
order items.

## Legacy Theme Convergence

After the first-wave screens, converge existing XML and legacy Compose foundations toward the
design-system look for safe tokens only. This is broader visual convergence for screens that remain
legacy; it is separate from explicit screen migration and does not require changing the root used by
existing `composeView {}` calls.

Allowed convergence areas:

- Background and surface colors.
- Toolbar and app chrome colors.
- Primary/accent colors when contrast is verified.
- Divider and outline colors.
- Shallow radius only when centralized and low-risk.

Explicitly out of scope for the convergence wave:

- Global typography replacement.
- Global spacing or padding replacement.
- Global status, alert, or semantic color rewrites.
- App-wide card radius changes.
- Behavior, navigation, or state-model rewrites.

Colors are now the approved XML-safe exception: Store design-system color primitives may live in
module-local Android resources so Compose and targeted XML/View usage read the same values through
`WooTheme.colors`.

Non-color foundations remain Kotlin/Compose-owned. Do not promote typography, spacing, padding,
radius, icon size, or stroke primitives to XML resources as part of this convergence direction.

When XML/View needs a design-system color token, use the shared module-local color resource and keep
Compose reading that same resource. Do not keep parallel Kotlin/Compose and XML primitive values.

## Toolbar Direction

The toolbar goal is a unified design-system visual look, not one mandatory implementation.

- Compose-owned screens use `WooTopAppBar`.
- The module `WooTopAppBar` is design-system-only and lives in `:libs:store-design-system`.
- Heavy XML screens may keep XML toolbar ownership if the toolbar matches the design-system look.
- `Widget.Woo.DesignSystem.Toolbar` is a style scaffold for colors, centered title, and insets; it is
  not enough on its own for parity with the Compose top app bar.
- XML-heavy screens that need visual parity can use `WooDesignSystemToolbar` from
  `:libs:store-design-system` for automatic design-system chrome. The library also owns
  `Widget.Woo.DesignSystem.Toolbar` and `ThemeOverlay.Woo.DesignSystem.Toolbar` for XML opt-ins.
  Visible inflated icon actions are decorated in place; text actions stay borderless, and
  expanded/custom action views remain screen-owned.
- Existing behavior must be preserved for `SearchView`, `ActionMode`, collapsing app bars,
  menu/action ownership, navigation ownership, and insets.
- A DS-looking XML toolbar is an acceptable migration tool for legacy-heavy screens. PR3 adds
  component/XML toolbar infrastructure and uses Product List as the first XML toolbar opt-in;
  additional product-screen adoption remains migration work.
- If a legacy-themed Compose root still needs toolbar compatibility, implement that compatibility in
  app code rather than adding app theme/resource coupling to `:libs:store-design-system`.

This keeps the migration focused on UI consistency without forcing a full app rewrite.

## Decision Table

| Surface | Scope | Action |
| --- | --- | --- |
| Dashboard tab | In | Migrate the tab surface to design-system UI. |
| Products tab | In | Migrate the tab surface to design-system UI. |
| Orders tab | In | Migrate the tab surface to design-system UI. |
| More tab | In | Migrate the tab surface to design-system UI. |
| Product Detail top surface | In | Migrate the top detail surface and chrome. Keep launched child/edit flows out. |
| Order Detail top surface | In | Migrate the top detail hub and chrome. Keep launched child flows out. |
| Product creation and AI creation | Out | Leave legacy for this wave unless separately assigned later. |
| Product editors and selectors | Out | Leave pricing, inventory, shipping, taxonomy, linked product, variation, add-on, subscription, review, and custom-field flows legacy for this wave. |
| Order shipping label flows | Out | Leave creation, refund, print, customs, and Woo Shipping child flows legacy for this wave. |
| Order refund creation | Out | Leave legacy for this wave. |
| Order editing/status/address flows | Out | Leave legacy for this wave. |
| Payment and card-reader flows | Out | Leave legacy for this wave. |
| Shipment tracking add/provider/barcode flows | Out | Leave legacy for this wave. |
| Receipt, printing, and fulfillment flows | Out | Leave legacy for this wave. |
| Other unlisted screens | Unassigned | Assess separately. Do not infer migration scope from this wave. |
