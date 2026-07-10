# Figma Navigation For Agents

This guide defines how agents should inspect the Woo Mobile Design System Figma file for Store
Design System work.

Figma remains the design-intent source of truth. Android owns the runtime API contract. Use Figma
inspection to verify source geometry, typography, color bindings, variants, and screenshots; do not
replace Figma values with Android-side guesses because a value looks wrong.

## Source Shorthands

Use the approved repo shorthand when writing docs:

- Figma file: `Woo Mobile Design System` (`50XIH5MmOf4xUYEkM6fAm6-fi`).
- Figma tool `fileKey`: `50XIH5MmOf4xUYEkM6fAm6`.

Do not expand the shorthand into raw Figma URLs in public repo docs. When using Figma tools, strip
the `-fi` suffix and pass only the file key.

## Discovery Flow

Use live Figma discovery as the source of page and node IDs. Do not depend on local work notes as the
authority for Figma nodes.

1. Start from the approved file key:

   ```text
   fileKey = 50XIH5MmOf4xUYEkM6fAm6
   ```

2. Resolve the current `Mobile Design System` library from the file:

   ```text
   get_libraries(fileKey)
   ```

   Use the returned `Mobile Design System` library key to scope component searches. This avoids
   matching similarly named components from WPDS, WordPress, Automattic, or deprecated libraries.

3. Search by component family name before choosing a node:

   ```text
   search_design_system(
     fileKey = 50XIH5MmOf4xUYEkM6fAm6,
     query = "Button",
     includeLibraryKeys = [Mobile Design System library key],
     includeComponents = true,
     includeVariables = false,
     includeStyles = false
   )
   ```

   Search results identify canonical component names, `componentKey`, `filePath`, and update time.
   A `componentKey` is useful evidence, but it is not directly screenshotable.

4. Use Figma metadata when it is available:

   ```text
   get_metadata(fileKey = 50XIH5MmOf4xUYEkM6fAm6, nodeId = "0:0")
   ```

   The `0:0` node is intentionally not a real design node. In MCP sessions where `get_metadata` is
   available, the tool may report it as invalid while also returning the top-level pages of the
   document. Use that page list to find pages such as `Badges`, `Buttons`, `Cell`, `Navigation`,
   `Notice Banner`, `Search`, and `Tabs`.

   Treat metadata as a useful navigation shortcut, not a blocker. Do not stop if `get_metadata` is
   unavailable, fails, or returns only `COVER` / top-level noise.

5. If metadata is unavailable or incomplete, use the whole-file Code Connect component graph:

   ```text
   list_file_components_for_code_connect(fileKey = 50XIH5MmOf4xUYEkM6fAm6)
   ```

   Use the returned component graph to find canonical component node IDs, page names, component-set
   names, and variant property definitions. This is the fallback path when metadata traversal cannot
   reliably locate the promoted source component.

6. Inspect the relevant page or canonical component node:

   ```text
   get_metadata(fileKey = 50XIH5MmOf4xUYEkM6fAm6, nodeId = "<page id from live page list>")
   get_context_for_code_connect(fileKey = 50XIH5MmOf4xUYEkM6fAm6, nodeId = "<canonical node id>")
   get_design_context(fileKey = 50XIH5MmOf4xUYEkM6fAm6, nodeId = "<canonical node id>")
   get_variable_defs(fileKey = 50XIH5MmOf4xUYEkM6fAm6, nodeId = "<canonical node id>")
   ```

   Prefer top-level component frames, component sets, or representative page frames for screenshots.
   Avoid sublayers inside instances because those IDs may not persist. Use node-level context,
   variable definitions, screenshots, and assets to confirm source geometry, variables, and variants.

7. Capture Figma evidence from existing nodes only:

   ```text
   get_screenshot(
     fileKey = 50XIH5MmOf4xUYEkM6fAm6,
     nodeId = "<frame or component-set id discovered from live Figma evidence>"
   )
   ```

   Increase `maxDimension` when fine detail matters. If implementation work is needed, call
   `get_design_context` on the canonical node after discovery; metadata and component graph results
   are navigation evidence only.

   For visual primitives where generated context returns image assets, download and inspect the asset
   SVG when context and search evidence do not expose the needed fill, stroke, or opacity details.

## Component Search Terms

Use Android API names to choose likely Figma search terms, then verify against live Figma evidence.

| Android family | Figma search terms / likely page names |
| --- | --- |
| `WooBadge`, `WooBadgeTone` | `Badge`, `Badges` |
| `WooFilledButton`, `WooFilledTonalButton`, `WooOutlinedButton`, `WooButtonSize` | `Button`, `Buttons`; source treatments are labeled `Fill`, `Tonal`, and `Outline` |
| `WooCell`, `WooSettingsRow` | `Cell`, `cell`; `Cell Content` is an internal text/content subcomponent mapping |
| `WooCheckbox` | `Check Box`, `checkbox`, `check-box-cell-content` |
| `WooFilterChip` | `Chip`, `Chips`, `Chip - Filter` |
| `WooDivider`, `WooVerticalDivider` | `Divider` |
| `WooIconButton`, `WooOutlinedIconButton` | Material 3 / token adapters; inspect `navigation-button` only for the Figma outlined navigation treatment |
| `WooIconContainer`, `WooIconContainerTone` | `Icon Container`, `icon-box` |
| `WooNoticeBanner`, `WooNoticeBannerTone` | `Notice Banner`, `notice-banner` |
| `WooPageHeader` | `Page Header`, `page-header` |
| `WooRadioButton` | `Radio Button`, `radio-button`; canonical node `1208:7478` in current Figma evidence |
| `WooSearchField` | `Search` |
| `WooSwitch` | Material 3 / token adapter; no canonical `Switch` component is currently found in Mobile Design System library search |
| `WooSwitchSettingsRow` | Adapter composition over `Cell` / `Cell Content` plus `WooSwitch` |
| `WooTabRow`, `WooTab` | `Tabs`, `tab-item` |
| `WooTopAppBar`, `WooTopAppBarAction`, `WooDesignSystemToolbar` | `Navigation`, `Top Navigation Bar`, `navigation-button` |
| Section header | `section-header` may appear in search, but its master lives in `Components Playground`, not on a promoted component page. Treat it as not public and ignore it for Android production API work for now |
| Preview-only families | `Segment Control`, `Sheets`, `Tab Bar`, `Table` |

`WooLinearProgressIndicator` and `WooCircularProgressIndicator` are thin Material 3 adapters. They
are not currently treated as i1 Figma component pages.

## Evidence Rules

- For reviews, record the exact Figma tool calls or search terms used.
- Record `libraryName`, `libraryKey`, `componentKey`, `filePath`, and `updatedAt` from
  `search_design_system` when available.
- Record page and node IDs discovered from live metadata or the Code Connect component graph in
  review artifacts. Treat those IDs as navigation aids that may need refresh if Figma components are
  recreated.
- Do not create scratch Figma files, temporary pages, or imported instances for read-only fidelity
  review unless the user explicitly approves that heavier workflow.
- Do not use local work notes as proof of Figma structure. They can be historical hints only when the
  task explicitly allows them.
- Do not report dark-theme contrast problems as component bugs when the component is faithfully using
  Figma-provided variables. Those fixes belong in foundations/design source.
- Do not promote top-level `Semantic` variables to runtime/public mappings unless a concrete
  component audit updates the token contract with approved evidence.
- Do not invent component tokens from source gaps. For example, `WooSearchField` currently uses
  `WooTheme.colors.surface.surfaceDim` for the field container; do not document an unconfirmed
  `search.fieldContainer` token.
- For visual primitives, search results are not enough. Confirm fills, strokes, state layers, and
  variant matrices with node-level context, variable definitions, screenshots, or asset SVGs.

## Radio Button Evidence Example

The radio button audit used the Code Connect fallback path because search-level evidence did not
expose all visual state details:

- `search_design_system` scoped to `Mobile Design System` confirmed the `radio-button` component
  identity.
- `list_file_components_for_code_connect(fileKey = 50XIH5MmOf4xUYEkM6fAm6)` identified canonical
  node `1208:7478` on page `Radio Button`.
- The variant matrix is `Type = Selected | Unselected` and `State = Enabled | Disabled`. There are
  no radio error or indeterminate variants.
- Node-level variable and SVG asset evidence showed selected uses a primary fill with an on-primary
  dot, unselected uses transparent fill with `Stroke/Weight/Medium = 1.5`, and disabled uses
  `State Layers/On Surface/Opacity-16`.

## Troubleshooting

- If metadata without a `nodeId` only returns `COVER`, use `nodeId = "0:0"` to get the top-level page
  list when `get_metadata` is available.
- If `get_metadata` is not available, fails, or still returns only `COVER` / top-level noise, use
  `list_file_components_for_code_connect(fileKey = 50XIH5MmOf4xUYEkM6fAm6)` and drill into the
  canonical node with `get_context_for_code_connect`, `get_design_context`, and `get_variable_defs`.
- If a node ID is rejected, refresh discovery through metadata or the Code Connect component graph.
  The component may have been recreated, or the ID may point to a non-persistent sublayer of an
  instance.
- If search returns decoy libraries, scope the search to the current `Mobile Design System` library
  key returned by `get_libraries`.
- If a family has no dedicated component page, document the search terms tried and compare against
  the nearest source-backed Figma instance, token map, or intentional Android-only wrapper.
