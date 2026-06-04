# Material 3 Reference for Store Design System Agents

This reference helps agents map Woo Mobile Design System i1 foundations and components to Material 3
defaults and Compose semantics. It is for the Store Management App design-system adapter only. POS is
out of scope.

Use this as a semantic baseline, not as a public API mandate. The i1 adapter still owns stable
`Woo` APIs under `com.woocommerce.android.ui.compose.designsystem`.

## Official Sources

- [Material Design 3 color roles](https://m3.material.io/styles/color/roles)
- [Material Design 3 typography](https://m3.material.io/styles/typography/type-scale-tokens)
- [Material Design 3 shape](https://m3.material.io/styles/shape/overview)
- [Material Design 3 elevation](https://m3.material.io/styles/elevation/overview)
- [Material Design 3 states](https://m3.material.io/foundations/interaction/states/state-layers)
- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose Material 3 API reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)
- [AndroidX Material 3 source tokens](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/)
- [Compose accessibility API defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [Compose accessibility semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)

Numeric defaults below are from the current public AndroidX Material 3 token source. Before
implementation, confirm API availability against the repo's pinned Material 3 dependency.

## Color Roles

Map i1 colors to Material 3 roles when the role describes the UI meaning. Add Woo-specific semantic
tokens only when Material 3 does not express the i1 intent.

| Role group | Use for i1 mapping |
| --- | --- |
| `primary`, `onPrimary` | Highest-emphasis brand actions and selected interactive surfaces, such as primary buttons. |
| `primaryContainer`, `onPrimaryContainer` | Prominent but less forceful brand containers, selected rows, or highlighted surfaces. |
| `secondary`, `onSecondary` | Supporting accents and controls where primary would overstate hierarchy. |
| `secondaryContainer`, `onSecondaryContainer` | Supporting selected or filled containers. |
| `tertiary`, `onTertiary` | Complementary accent moments. Use sparingly and only when i1 has a distinct third accent meaning. |
| `error`, `onError`, `errorContainer`, `onErrorContainer` | Destructive actions, validation errors, and error containers. |
| `surface`, `onSurface` | Default app surfaces and primary text/icons on neutral surfaces. |
| `surfaceVariant`, `onSurfaceVariant` | Lower-emphasis neutral containers, secondary text/icons, and content adjacent to dividers. |
| `surfaceContainerLowest` through `surfaceContainerHighest` | Nested neutral containers. Prefer this scale over inventing ad hoc neutral fills. |
| `surfaceDim`, `surfaceBright` | Light/dark-aware surface extremes. Use only if i1 needs that explicit semantic. |
| `background`, `onBackground` | Root/background areas when distinct from `surface`. In M3, prefer `surface` for most containers. |
| `outline`, `outlineVariant` | Borders, dividers, and low-emphasis strokes. |
| `inverseSurface`, `inverseOnSurface`, `inversePrimary` | Inverse surfaces such as snackbar-like UI. |
| `scrim` | Modal overlays and obscuring layers. |
| `primaryFixed` and other fixed roles | Dynamic-color-stable accent roles. Do not use unless a later design decision needs fixed dynamic-color behavior. |

Always pair `on*` colors with their matching container/background role. Do not put
`onPrimaryContainer` on `primary`, or `primaryContainer` on `tertiaryContainer`, unless contrast has
been explicitly checked and the mismatch is intentional.

## Typography Scale

Compose Material 3 exposes a 15-role type scale through `MaterialTheme.typography`. Use the role by
semantic purpose first, then adjust i1 values in `WooDesignSystemTheme` if the design differs.

| Role | Default size/line | Weight | Typical use |
| --- | --- | --- | --- |
| `displayLarge` | 57sp/64sp | Regular | Rare, largest marketing-scale display. |
| `displayMedium` | 45sp/52sp | Regular | Large display. |
| `displaySmall` | 36sp/44sp | Regular | Smaller display. |
| `headlineLarge` | 32sp/40sp | Regular | Screen-level headline. |
| `headlineMedium` | 28sp/36sp | Regular | Major section headline. |
| `headlineSmall` | 24sp/32sp | Regular | Section headline. |
| `titleLarge` | 22sp/28sp | Regular | App bars, page titles, prominent titles. |
| `titleMedium` | 16sp/24sp | Medium | Card/list titles and medium-emphasis labels. |
| `titleSmall` | 14sp/20sp | Medium | Compact titles. |
| `bodyLarge` | 16sp/24sp | Regular | Primary body copy. |
| `bodyMedium` | 14sp/20sp | Regular | Default compact body copy. |
| `bodySmall` | 12sp/16sp | Regular | Supporting copy. |
| `labelLarge` | 14sp/20sp | Medium | Buttons and large labels. |
| `labelMedium` | 12sp/16sp | Medium | Compact labels. |
| `labelSmall` | 11sp/16sp | Medium | Small labels and metadata. |

Material 3 currently also has emphasized token variants in AndroidX source. Treat those as internal
Material implementation details until the i1 adapter deliberately exposes a Woo semantic for them.

## Shape Roles

Compose Material 3 `Shapes` defaults are:

| Role | Default | Common Material use |
| --- | --- | --- |
| `extraSmall` | 4dp | Menus, snackbars, text fields. |
| `small` | 8dp | Chips. |
| `medium` | 12dp | Cards, small FABs. |
| `large` | 16dp | FABs, extended FABs, navigation drawers. |
| `extraLarge` | 28dp | Large FABs. |
| `largeIncreased` | 20dp | Expressive larger large role. |
| `extraLargeIncreased` | 32dp | Expressive larger extra-large role. |
| `extraExtraLarge` | 48dp | XXL containers. |
| `RectangleShape` | 0dp | Square containers. |
| `CircleShape` / full | Full | Pills, circular icon containers, full-round buttons. |

Map i1 radius tokens to the nearest role if the semantic matches. If i1 defines a radius with a
product-specific meaning, keep a Woo shape token and document the closest Material role in
`token-map.md`.

## Elevation And Surfaces

Material 3 primarily differentiates surfaces with tonal elevation, backed by optional shadow
elevation. Compose `Surface` supports both:

- `tonalElevation`: changes the tonal surface color when the surface color is `ColorScheme.surface`.
- `shadowElevation`: draws a physical shadow. Use only when i1 needs visual separation that tonal
  surfaces cannot provide.
- `ColorScheme.surfaceColorAtElevation(elevation)` computes elevated surface color.
- AndroidX elevation token levels are `0dp`, `1dp`, `3dp`, `6dp`, `8dp`, and `12dp`.

For i1, prefer explicit `surfaceContainer*` roles for stable neutral container hierarchy. Use
`Surface` and component defaults where the Material behavior matches. Avoid creating one-off neutral
fills when a surface role already describes the hierarchy.

## State Layers And Interaction States

Material state layers communicate interaction without changing component identity. AndroidX
Material 3 state layer opacity tokens are:

| State | Opacity |
| --- | --- |
| Hovered | `0.08f` |
| Focused | `0.10f` |
| Pressed | `0.10f` |
| Dragged | `0.16f` |

Material 3 `ripple()` is the default `LocalIndication` inside `MaterialTheme`. It draws ripple
animations for press interactions and fixed state layers for other interactions. For custom Woo
components, prefer Material components or `Surface`/`Modifier.clickable` with the default indication
before building custom state drawing.

Use `MutableInteractionSource` only when a component needs to expose or preview state. Keep disabled
behavior aligned with Material defaults unless i1 explicitly defines different disabled opacity or
color tokens.

## Component Mapping Guidance

- Start with Material 3 components when i1 intent, layout, state, accessibility, and shape are close.
- Wrap them in `Woo` components so product screens consume adapter APIs, not raw Material defaults.
- Use component defaults objects, such as `ButtonDefaults`, `CardDefaults`, `SwitchDefaults`, and
  `ListItemDefaults`, to override colors, shapes, and elevation behind the Woo API.
- Build custom Compose components only when i1 materially differs in layout, behavior, state model,
  or semantics.
- Use `ListItem` as a candidate for i1 cell/row semantics, but verify density, leading/trailing
  content, dividers, and click/toggle semantics before marking a Woo cell production-ready.
- Use Material emphasis variants intentionally: filled button for high emphasis, outlined/text for
  lower emphasis, icon buttons for compact actions, and progress indicators as thin wrappers.
- Do not expand the adapter into a general Material 3 wrapper library. Follow the production subset
  and preview-only boundaries in `component-catalog.md`.

## Accessibility Notes

- Material components include many accessibility defaults, including semantics and minimum touch
  target behavior for actionable controls. Custom components must provide equivalent semantics.
- Actionable targets should preserve at least 48dp touch size unless there is a documented exception.
- Icon-only actions need meaningful labels. Decorative icons should use `contentDescription = null`.
- Use `heading()`, `stateDescription`, `error`, `progressBarRangeInfo`, `paneTitle`, or custom
  accessibility actions when a custom component needs those semantics.
- Do not communicate state by color alone. Include labels, icons, semantics, or layout affordances
  where needed.
- Check contrast whenever i1 overrides Material color pairings. The matching `on*` role is the safe
  default, but custom token values still need review in light and dark themes.
- Add font-scale and RTL previews when the component or screen is sensitive to text growth or
  directionality.

## Compose API Pointers

- Theme: `MaterialTheme(colorScheme, typography, shapes)`.
- Color schemes: `lightColorScheme(...)`, `darkColorScheme(...)`, `ColorScheme`.
- Color helpers: `contentColorFor(...)`, `ColorScheme.surfaceColorAtElevation(...)`.
- Typography: `Typography(...)`, `MaterialTheme.typography.<role>`.
- Shapes: `Shapes(...)`, `MaterialTheme.shapes.<role>`.
- Surfaces: `Surface(color, contentColor, tonalElevation, shadowElevation, shape)`.
- Interactions: `ripple()`, `MutableInteractionSource`, `collectIsPressedAsState()`,
  `collectIsFocusedAsState()`, `collectIsHoveredAsState()`, `collectIsDraggedAsState()`.
- Components: use `androidx.compose.material3` components and their `*Defaults` objects internally.
- Previews: use `androidx.compose.ui.tooling.preview.PreviewLightDark` for design-system previews.

## Token Ownership And XML/View Promotion

The existing adapter decision still stands:

- i1 token primitive values start Kotlin/Compose-owned in the design-system foundation layer.
- Material 3 `ColorScheme`, `Typography`, `Shapes`, and component defaults should be built from those
  Kotlin/Compose-owned primitives.
- Do not create Android resources for every Material role just because `ColorScheme` has that role.
- Promote a primitive to Android resources only when a real non-migrated XML/View screen needs that
  token.
- When promotion is needed, move the primitive value to resources and update Compose to read the same
  value. Do not keep parallel Kotlin and XML primitive definitions.
- Record every production token and its closest Material 3 role in `token-map.md`.
