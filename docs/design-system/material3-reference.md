# Material 3 Reference for Store Design System Agents

This reference helps agents map Woo Mobile Design System i1 foundations and components to Material 3
defaults and Compose semantics. It is for the Store Management App design-system adapter only. POS is
out of scope.

Use this as a semantic baseline, not as a public API mandate. The i1 adapter still owns stable
`WooTheme` and component APIs under `com.woocommerce.android.ui.compose.designsystem`.

Store design-system APIs should expose source-backed authoring roles through `WooTheme`.
`MaterialTheme` is still populated from source values so Material 3 components, defaults, and
helpers work normally. Product-screen and design-system component code should read Store foundations
through `WooTheme` and use `MaterialTheme` when a Material API requires the interop projection.

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

Use Material 3 color roles as interop semantics, not as the public Store color API. PR 2
`WooTheme.colors` should expose source-backed Store authoring roles from normal `Light` / `Dark`
values in non-`Semantic` `Woo theme` sources. `MaterialTheme.colorScheme` receives projections for
Material 3 components, defaults, and helpers. Roles without approved Store semantics may
intentionally use `lightColorScheme(...)` / `darkColorScheme(...)` builder defaults.

Do not use top-level `Semantic` or high-contrast modes for normal Material color projections.
Container roles are first-class Store roles and can project to Material container roles. The fuller
surface role set is first-class Store data, including `surfaceDim`, `surfaceBright`,
`surfaceContainerHighest`, `onVariantLowest`, `inverted`, and `onInverted`. These roles should
project from their Store source-backed roles, not older internal aliases. Background, overlay, and
palette roles remain export-backed even when they are not validated by the Color roles frame.

Do not generate public `WooTheme.colors` entries for Material fixed roles or source-missing aliases.
`outline` and `outlineVariant` are source-backed and public under `WooTheme.colors`.

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
| `surfaceContainerLowest` through `surfaceContainerHighest` | Nested neutral containers for projection. `surfaceContainerHighest` is a promoted Store role; the rest stay internal unless source-backed names are promoted. |
| `surfaceDim`, `surfaceBright` | Source-backed Store surface roles. |
| `background`, `onBackground` | Root/background areas when distinct from `surface`. In M3, prefer `surface` for most containers. |
| `outline`, `outlineVariant` | Borders, dividers, and low-emphasis strokes. |
| `inverseSurface`, `inverseOnSurface`, `inversePrimary` | Inverse surfaces such as snackbar-like UI. |
| `scrim` | Modal overlays and obscuring layers. |
| `primaryFixed` and other fixed roles | Dynamic-color-stable accent roles. Keep internal unless source-backed names exist. |

Always pair `on*` colors with their matching container/background role. Do not put
`onPrimaryContainer` on `primary`, or `primaryContainer` on `tertiaryContainer`, unless the component
intentionally owns that semantic mismatch.

Public palette/ramp and alert tokens do not automatically approve foreground/background pairing. Treat
them as source colors unless a component owns a specific semantic mapping.

The Store source `Woo theme/Alerts/Error-Container` /
`Woo theme/Alerts/On-Error-Container` pair is an error container/background pair. It should project
to `errorContainer` / `onErrorContainer`, not to the foreground/control `error` role. Leave
`error` / `onError` on Material defaults until a source-backed Store foreground error pair is
approved.

## Typography Scale

Compose Material 3 exposes a 15-role type scale through `MaterialTheme.typography`. The Store
design-system authoring surface is `WooTheme.text`, which keeps the regular, emphasized, and strong
variants. `MaterialTheme.typography` receives the regular projection for Material 3 interop.

All 15 Android Store roles are source-backed by `figma-export.json` / `Typescale`; use Android mode
values. Export role names are hyphenated while Android doc/API names are camelCase. Source
`Typescale/<Role>/Font` resolves through `Font theme/Font/Plain`, whose Android value is `Roboto`;
Android default font is the accepted runtime equivalent.

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

Material 3 currently also has emphasized token variants in AndroidX source. Treat those as Material
implementation details; the Store design-system text variants are the ones exposed through
`WooTheme.text`.

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

Store radius tokens project to Material 3 shapes as:

| Material role | Store source-backed radius |
| --- | ---: |
| `extraSmall` | `2dp` |
| `small` | `4dp` |
| `medium` | `8dp` |
| `large` | `12dp` |
| `extraLarge` | `16dp` |

`WooTheme.radius.none = 0dp` and `WooTheme.radius.full = 999dp` are Woo-only tokens with no direct
Material role. The `large` and `extraLarge` projection changes need visual review because the current
legacy projection uses `8dp` for both roles.

## Elevation And Surfaces

Material 3 primarily differentiates surfaces with tonal elevation, backed by optional shadow
elevation. Compose `Surface` supports both:

- `tonalElevation`: changes the tonal surface color when the surface color is `ColorScheme.surface`.
- `shadowElevation`: draws a physical shadow. Use only when i1 needs visual separation that tonal
  surfaces cannot provide.
- `ColorScheme.surfaceColorAtElevation(elevation)` computes elevated surface color.
- AndroidX elevation token levels are `0dp`, `1dp`, `3dp`, `6dp`, `8dp`, and `12dp`.

For Material 3 interop, `surfaceContainer*` roles can help model stable neutral container hierarchy.
Keep aliases internal unless the source export has matching promoted Store roles. Product code should
prefer the source-backed `WooTheme.colors` surface/background tokens.

Elevation remains unresolved for Store foundations. No non-`Semantic` elevation, shadow, effect,
z-depth, or tonal-elevation source exists in `figma-export.json`, so do not create a public Store
elevation token or infer one from legacy Android resources.

## State Layers And Interaction States

Material state layers communicate interaction without changing component identity. AndroidX
Material 3 state layer opacity tokens are:

| State | Opacity |
| --- | --- |
| Hovered | `0.08f` |
| Focused | `0.10f` |
| Pressed | `0.10f` |
| Dragged | `0.16f` |

The Store export contains `Woo theme/State-Layers/On-Surface/Opacity-08`, `Opacity-10`, and
`Opacity-16`. Treat them as mode-aware color tokens, not public raw alpha floats. Keep state-layer
implementation internal-first until design confirms whether they map to hovered, focused/pressed,
and dragged states, and whether the dark and high-contrast solid values are intentional.

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
- Use matching `on*` roles whenever i1 overrides Material color pairings. Document intentional role
  mismatches in the component contract.
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

## Token Ownership And XML/View Usage

The current adapter decision is:

- Store design-system APIs remain Compose-first at the component and authoring layer.
- Store design-system color primitives are the XML-safe exception and may live in module-local
  Android resources so Compose and targeted XML/View usage share the same values.
- Non-color primitive foundations remain Kotlin/Compose-owned unless a later approved plan says
  otherwise.
- Material 3 `ColorScheme`, `Typography`, `Shapes`, and component defaults should be built from
  `WooTheme` foundation values, whether those values originate from Kotlin/Compose primitives or
  module-local color resources.
- Do not create Android resources for every Material role just because `ColorScheme` has that role.
- Expose source-backed Store authoring roles through `WooTheme.colors`, grouped shallowly by source
  intent.
- Do not create a parallel semantic-colors carrier in PR 2. Supported status, alert, overlay, and
  palette colors live as grouped fields under `WooTheme.colors`.
- Do not expose top-level `Semantic` groups in PR 2 unless a concrete Figma component audit approves
  that token group.
- Keep Material 3-only projection aliases internal unless the alias is itself a source-backed token.
- Treat `surfaceDim`, `surfaceBright`, and `surfaceContainerHighest` as source-backed Store roles.
- Keep high-contrast modes out of normal `Light` / `Dark` runtime mapping until accessibility-mode
  scope is decided.
- Do not create a public `WooTheme.stateAlpha` float API from mode-aware state-layer color tokens.
- When XML/View needs a design-system color, consume the shared module-local color resource through
  targeted style usage and keep Compose reading the same value through `WooTheme.colors`.
- Do not keep parallel Kotlin and XML primitive definitions.
- Record every production token and its closest Material 3 role in `token-map.md`.
