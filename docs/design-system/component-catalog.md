# Store Design System Component Catalog

This catalog tracks Woo Mobile Design System i1 components for Android.

The goal is to implement the i1 foundation and component catalog with previews, while exposing a broad
Figma-aligned public Compose API where the component contract can stay small, stateless, and source-backed.

The component PR should provide visual coverage for the full i1 catalog. Preview-only is the exception:
components should stay private/internal only when a public API would encode unresolved design state, screen behavior,
app-shell ownership, or complex data semantics.

## Production API Scope

Before the two pilots begin, production-ready APIs should exist for the original baseline:

- Top/navigation bar.
- Page title/body/link text styles or wrappers.
- Primary button.
- Settings cell/row.
- Section header.
- Switch as a pilot-needed Material 3 wrapper, not an inspected i1 Figma component.
- Icon button.
- Divider.
- Progress indicator.
- Spacing, radius, color, and typography tokens used by those components.

PR3 also expands the public component surface for source-backed, low-risk catalog items whose contracts can remain
controlled and close to Material 3: badges, secondary/tertiary/small buttons, generic cell/cell content,
checkbox, radio button, filter chip, icon container, static notice banner, page header composition, search field,
and top tabs.

Production components should read approved foundation values through `WooTheme.*`. `MaterialTheme` remains available
inside wrappers when Material 3 components or defaults require interop projection values.

Production components must render correctly under both the real design-system foundation and the
legacy-compatible foundation provided by `WooThemeWithBackground`. Do not rely on static fallback
defaults such as `LightWooColors`.

Do not add thin wrappers that are not source-backed by the i1 catalog or a known Store migration need. This prevents
the adapter from becoming an unlimited Material 3 wrapper library.

## Preview Standard

Use `androidx.compose.ui.tooling.preview.PreviewLightDark` for design-system component previews.

Older Store Compose screens may use the project `LightDarkThemePreviews` annotation. New design-system foundations, components, preview catalog entries, and pilot updates should use `@PreviewLightDark`.

Design-system component previews should wrap content in `WooDesignSystemTheme`, not
`WooThemeWithBackground`. Migrated screen previews should cover both the legacy-compatible foundation
and the real design-system foundation.

Preview coverage is required for every component. Screenshot verification is required for pilot screens and high-risk components, but not for every small primitive component.

## Status Values

- `production`: Stable API, visual states, accessibility behavior, and previews.
- `production_initial`: Public API intended for product-screen adoption, but still early enough to refine during
  pilot feedback. These APIs should stay stateless and close to Material 3 where possible.
- `preview_only`: Implemented for catalog or design review, but not exposed as a reusable production-screen API.
- `needs_design`: Design intent or required states are not signed off.
- `needs_android_mapping`: Android API or Material 3 mapping is not resolved.

## Catalog

| Component | Android API | Strategy | Status | Required previews | Notes |
| --- | --- | --- | --- | --- | --- |
| Badges | `WooBadge`, `WooBadgeTone`; optional `leadingIcon` slot | Figma-backed custom label badge | production_initial | Light/dark, seven tone variants including `NeutralOutlined`, long label, large font, leading icon | Public scope is compact label badges, not Material count badges. Figma Badges page node `1164:7609`, group node `199:19563`, and error node `199:19562` use 24dp minimum height, `corner-radius/medium`, `bodySmall.regular`, 14dp optional icons, and status container/on-container tone pairs. Tone naming follows source: `Caution` maps to warning tokens and `Warning` maps to caution tokens. Count/badged-box semantics remain out of PR3. Accessibility contract: visible text remains the label, optional icons are decorative unless no text equivalent exists, and tone is not the only state signal. |
| Buttons | `WooPrimaryButton`, `WooSecondaryButton`, `WooTertiaryButton`, `WooButtonSize`; optional `leadingIcon` slot | Figma-backed custom button surface with source-controlled styles | production_initial | Light/dark, enabled, disabled, long text, large font, primary/secondary/tertiary, medium/small, leading icon | Primary is the baseline production action. Secondary is the source-backed filled secondary style, and Tertiary is the source-backed secondary outline style; loading/destructive variants remain private until source-backed state rules are approved. Figma Button nodes `1653:5498` / `1653:5502` / `1654:5531` use `corner-radius/large`, `primary/onPrimary`, `secondary/onSecondary`, `stroke/weight/medium` for tertiary, 18dp medium icons, and 14dp small icons. Small node `1653:5503` uses a 32dp visual with `labelMedium`. Accessibility contract: button role is preserved, disabled blocks click, visible text remains the accessible label, optional leading icons are decorative unless the caller supplies meaningful icon semantics, and actionable small visuals keep a 48dp touch target. |
| Cell | `WooCell`, `WooSettingsRow`, `WooSwitchSettingsRow` | Custom slot row; Material 3 `ListItem` remains only an internal comparison | production_initial | Light/dark, leading/trailing, switch row, disabled-ready API, long text, large font, RTL | `WooCell` is the public row primitive: title, optional description, optional leading/trailing slots, enabled state, and one row action. The layout follows the Cell and Cell Content evidence from Figma nodes `1186:13285`, `1181:12446`, and `1164:9190`: content-driven height, 24dp horizontal row padding, 12dp vertical row padding, 16dp slot/content gaps, and a 48dp minimum touch target. Grouped cell containers should add 12dp vertical content padding so row padding and container padding combine to the 24dp section-edge-to-content rhythm shown in multi-cell Figma examples. `WooSettingsRow` is a Store settings convenience wrapper over the same cell layout. `WooSwitchSettingsRow` keeps row-owned switch semantics. Accessibility contract: one row-level action only, decorative slots do not add duplicate semantics, and text wraps at large font scales. |
| Cell Content | `WooCellContent`; title/description content inside row APIs | Custom text composition inside rows | production_initial | Light/dark, title/description density, long text, large font | Public as the reusable text/content nucleus for cells and settings rows. It accepts an enabled flag so parent rows can apply the same source-backed disabled colors without adding separate click semantics. Title and description use a tight 2dp vertical gap matching the Cell Content examples; content text wraps by default and does not expose an independent action. |
| Check Box | `WooCheckbox` | Material 3 `Checkbox` wrapper | production_initial | Light/dark, checked, unchecked, disabled | Public scope is the controlled checkbox primitive only. Checkbox rows/groups remain future work because label ownership and form grouping should be validated in a real form screen. Accessibility contract: caller owns checked state and label association; disabled state follows Material behavior. |
| Chip | `WooFilterChip` | Material 3 `FilterChip` wrapper | production_initial | Light/dark, selected, unselected, disabled, optional leading icon | Public scope is the selected/unselected filter-chip variant currently represented in the catalog. Other chip families remain future work until variants are mapped. Accessibility contract: selected state is semantic, not color-only, and caller owns selection state. |
| Divider | `WooDivider`, `WooVerticalDivider` | Thin Material 3 wrapper | production | Light/dark, horizontal, vertical | Figma Divider node `347:3189` uses `outline/outline-variant` and `stroke/weight/extra-thin`; inset/full/spacer variants remain future/preview-only. Accessibility review result: no semantic role by default and dividers must not be the sole required state indicator. |
| Icons | `WooIconButton`, `WooOutlinedIconButton`, `WooIconButtonEmphasis`; private catalog sample for icon catalog | Material 3 `IconButton` wrappers with content-slot and ImageVector convenience overloads; outlined variant uses the Figma-backed 40dp visual container inside a 48dp target | production | Light/dark, neutral/primary emphasis, disabled-ready API, plain and outlined states, icon catalog preview-only | Production scope is icon-only actions, not icon naming policy. Plain `WooIconButton` has no border and uses the shared emphasis policy. Figma navigation-button node `1656:5112` maps to `WooOutlinedIconButton` with a 40dp large-radius outline and 18dp icon. Accessibility review result: non-blank content description is required and owned by button semantics; child icon semantics are decorative; 48dp Material touch target preserved. |
| Icon Container | `WooIconContainer`, `WooIconContainerTone` | Figma-backed custom icon box | production_initial | Light/dark, seven palette tone variants, decorative/labeled usage | Public scope is the source-backed 44dp icon box with restricted palette tone options: Purple, Sandstone, Blue, Green, Orange, Pink, and DarkPurple. Figma icon-box node `1447:7243` uses `corner-radius/medium` and an 18dp icon. It should not become an open palette utility. Accessibility contract: icons are decorative by default unless the caller supplies a label, and tone cannot be the only information carrier. |
| Navigation Bar | `WooTopAppBar`; private catalog sample for exploratory variants | Material 3 `CenterAlignedTopAppBar` wrapper with slot API and String/ImageVector convenience overload | production | Light/dark, navigation/action states, long title with two actions, large font, RTL | Production scope is the centered 64dp top navigation bar from Figma node `1657:961`, including surface background, bottom divider, centered `bodyLarge.strong` title, and 40dp navigation action affordances inside 48dp targets. The slot API lets multiple actions be measured by Material 3 instead of overlaid with fixed title padding. Accessibility review result: navigation icon convenience overload requires non-blank label and click handler; title remains visible text; existing auto-mirror guard is preserved for RTL. |
| Notice Banner | `WooNoticeBanner`, `WooNoticeBannerTone` | Figma-backed custom static banner | production_initial | Light/dark, seven tone variants including `NeutralOutlined`, title/description, long text, large font | Public scope is a static title/description banner. `NeutralOutlined` restores parity with the recorded seven-tone notice-banner source and uses an outlined neutral treatment. Dismissible, actionable, transient, and live-region banners remain future work until a screen needs those behaviors. Accessibility contract: visible text carries the message; optional leading icon is decorative unless no text equivalent exists. |
| Page Header | `WooPageHeader`; text primitives `WooPageTitle`, `WooBodyText`, `WooLinkedBodyText`, `WooLinkText` | Figma-backed custom page-header bar plus standalone text/link primitives | production_initial | Light/dark, title, optional right actions, divider, long title, large font | `WooPageHeader` maps to Figma Page Header page node `1168:11407` and component node `1168:11650`: a 64dp surface bar with left-aligned `headlineSmall.strong` title, 24dp horizontal inset, optional 40dp right action area, and bottom divider. Text wrappers remain production primitives for page content intros, but PR3 no longer exposes a separate title/body/link composition under the Page Header name. Accessibility contract: page-header title is a heading; links are underlined and use link annotations/listeners instead of color-only styling. |
| Radio Button | `WooRadioButton` | Material 3 `RadioButton` wrapper | production_initial | Light/dark, selected, unselected, disabled | Public scope is the controlled radio primitive only. Radio rows/groups remain future work because label association and group semantics should be validated in a real form screen. Accessibility contract: caller owns selected state and label/group association. |
| Search | `WooSearchField` | Material 3 `OutlinedTextField` wrapper | production_initial | Light/dark, query, placeholder, clear action, disabled clear state, focused/unfocused, large font, RTL | Public scope is a controlled search field only. Search layout, results, filters, and focus orchestration remain screen-owned. Accessibility contract: caller supplies placeholder/labels, clear action has a label, value remains caller-owned, and the clear action follows the field enabled state. |
| Segment Control | Private catalog sample under `designsystem.preview` | Preview-only custom sample until design is signed off | preview_only | `WooDesignSystemComponentCatalogPreview` light/dark, selected state | Kept preview-only because the i1 source marks Segment Control in progress. A public API would get ahead of the signed-off selected, disabled, focus, and sizing model. |
| Sheets | Private catalog sample under `designsystem.preview` | Preview-only visual sample | preview_only | `WooDesignSystemComponentCatalogPreview` light/dark, content sample | Kept preview-only because a production sheet API must define modal state, dismissal choreography, navigation/event ownership, pane/title semantics, and content insets. Those decisions need a real screen adoption target, not only the visual catalog. |
| Tab Bar | Private catalog sample under `designsystem.preview` | Figma-backed custom bottom navigation preview-only sample | preview_only | `WooDesignSystemComponentCatalogPreview` light/dark, selected/unselected items | Kept preview-only because bottom Tab Bar is app-shell navigation. A public API before an app-shell migration would need back-stack ownership, selected destination semantics, labels, badges, and system inset behavior. Represents Figma Tab Bar page node `919:10678`, tab-bar `1392:40868`, active item `1388:40618`. |
| Tabs | `WooTabRow`, `WooTab` | Material 3 `PrimaryTabRow` and `Tab` wrappers | needs_android_mapping | Light/dark, selected/unselected, long labels, large font | This API covers Store-style text top tabs, but it is not visual parity for the Figma Tab Bar source. Figma Tab Bar page node `919:10678`, tab-bar `1392:40868`, and active tab item `1388:40618` describe a bottom/icon tab bar, which remains preview-only app-shell scope in PR3. Keep `WooTabRow` out of pilot adoption until a source-backed top-tabs design or explicit Store migration need is approved. Accessibility contract: caller owns selected index/value; selected state is semantic; labels remain visible with predictable overflow. |
| Table | Private catalog sample under `designsystem.preview` | Preview-only custom sample | preview_only | `WooDesignSystemComponentCatalogPreview` light/dark, row density sample | Kept preview-only because a production table API needs data model, column sizing, scrolling rules, sorting/selection behavior, and table accessibility semantics. The current source only supports a static visual sample. |
| Progress Indicator | `WooLinearProgressIndicator`, `WooCircularProgressIndicator` | Thin Material 3 wrapper | production | Light/dark, indeterminate, determinate | Not listed as an i1 Figma component. Accessibility review result: Material progress semantics preserved; determinate progress uses Material 3 lambda overloads and coerces values into `0f..1f`. |

## Production Checklist

Before a component is marked `production`:

- It uses `Woo` naming inside `com.woocommerce.android.ui.compose.designsystem`.
- It is wrapped by `WooDesignSystemTheme` in design-system previews.
- It reads production foundation values through `WooTheme.*`, except where a Material 3 API requires `MaterialTheme`
  interop values.
- It renders under `WooThemeWithBackground` without falling back to hardcoded light defaults.
- It has light and dark previews through `@PreviewLightDark`.
- Required states are covered in previews.
- Screenshot verification is completed if the component is high-risk.
- Accessibility is reviewed: semantic role where applicable, label/contentDescription rules, disabled state behavior, minimum touch target, and font-scale resilience.
- Its tokens are mapped in `docs/design-system/token-map.md`.
- It does not depend on POS APIs or POS design-system concepts.

## Preview-Only Boundary

Preview-only components may exist only as private/internal catalog or preview implementations, preferably under `designsystem.preview`.
Under the revised PR3 scope, preview-only is limited to components with concrete blockers: Segment Control,
Sheets, bottom Tab Bar, and Table. PR3 keeps only group-level catalog/screenshot entry points module-visible for
previews; individual unsettled samples are file-private.

Do not expose preview-only components as reusable public APIs. This keeps product-screen migrations and AI agents
from importing components whose API would currently encode unstable design, navigation, or data semantics.
