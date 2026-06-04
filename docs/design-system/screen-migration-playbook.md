# Store Design System Screen Migration Playbook

This playbook is for AI-assisted Store Management App screen migrations.

Migration is optional per screen. Do not assume every XML/View screen should be migrated to Compose.

There are two supported adoption outcomes:

- XML/View layout to Fragment-hosted Compose layout migration.
- Existing Compose screen adopting the design-system theme, tokens, and components.

## Candidate Assessment

Assess the screen before editing code.

A good candidate:

- Is a low-risk product surface.
- Has visible design-system value: toolbar, text hierarchy, cells, buttons, banners, empty/loading states, or forms.
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

If the screen is high-risk, stop and ask whether it should be migrated, partially updated with View/XML styles, or deferred.

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
- Replace legacy/current Compose styling with production-ready design-system theme, tokens, and components.
- Preserve or improve existing preview coverage.

It does not mean:

- Rewriting the screen's state model.
- Moving navigation into Compose.
- Migrating unrelated legacy components.
- Changing product copy or behavior without an explicit product decision.

## Workflow

1. Capture the baseline: current layout, Fragment/dialog host, navigation, analytics, strings, images, and accessibility.
2. Decide whether the screen is a good candidate and which adoption outcome applies.
3. Keep the existing host. For XML/View migration, replace the layout with `composeView`; for existing Compose adoption, keep the current Compose root.
4. Opt into the design-system theme only at the Compose root for the adopted screen.
5. Build a stateless screen composable and a VM-aware overload only if the screen needs ViewModel state.
6. Use design-system components and tokens where they are production-ready.
7. Do not use preview-only components in production screens. Preview-only implementations should not have public APIs intended for product-screen imports.
8. Preserve existing string resources and analytics event names unless the migration explicitly requires a product copy or tracking change.
9. Add light and dark previews with `@PreviewLightDark`. Pilot previews should use the same theme that the screen opts into at runtime. Add font scale, RTL, or orientation previews when the screen is sensitive to those dimensions.
10. Verify pilot screens with screenshot review, targeted tests when behavior changes, and an accessibility regression check against the original screen.

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
- What design-system tokens/components were used.
- What stayed in XML/View or legacy Compose and why.
- Preview coverage added.
- Verification performed.
- Accessibility checks performed.
- Any follow-up needed before broader migration.
