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

4. Get the live page list from Figma metadata:

   ```text
   get_metadata(fileKey = 50XIH5MmOf4xUYEkM6fAm6, nodeId = "0:0")
   ```

   The `0:0` node is intentionally not a real design node. The tool reports it as invalid, but also
   returns the top-level pages of the document. Use that page list to find pages such as `Badges`,
   `Buttons`, `Cell`, `Navigation`, `Notice Banner`, `Search`, and `Tabs`.

   Do not stop after a no-`nodeId` metadata call if it only shows a cover page. Use the page-list
   call above before deciding a component cannot be found.

5. Inspect the relevant page by the page ID returned from live metadata:

   ```text
   get_metadata(fileKey = 50XIH5MmOf4xUYEkM6fAm6, nodeId = "<page id from live page list>")
   ```

   Prefer top-level component frames, component sets, or representative page frames for screenshots.
   Avoid sublayers inside instances because those IDs may not persist.

6. Capture Figma evidence from existing nodes only:

   ```text
   get_screenshot(
     fileKey = 50XIH5MmOf4xUYEkM6fAm6,
     nodeId = "<frame or component-set id discovered from live metadata>"
   )
   ```

   Increase `maxDimension` when fine detail matters. If implementation work is needed, call
   `get_design_context` after metadata; metadata alone is only structure.

## Component Search Terms

Use Android API names to choose likely Figma search terms, then verify against live page metadata.

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
| `WooRadioButton` | `Radio Button`, `radio-button` |
| `WooSearchField` | `Search` |
| `WooSwitch` | Material 3 / token adapter; no canonical `Switch` component is currently found in Mobile Design System library search |
| `WooSwitchSettingsRow` | Adapter composition over `Cell` / `Cell Content` plus `WooSwitch` |
| `WooTabRow`, `WooTab` | `Tabs`, `tab-item` |
| `WooTopAppBar`, `WooTopAppBarAction`, `WooDesignSystemToolbar` | `Navigation`, `Top Navigation Bar`, `navigation-button` |
| Preview-only families | `Segment Control`, `Sheets`, `Tab Bar`, `Table` |

`WooLinearProgressIndicator` and `WooCircularProgressIndicator` are thin Material 3 adapters. They
are not currently treated as i1 Figma component pages.

## Evidence Rules

- For reviews, record the exact Figma tool calls or search terms used.
- Record `libraryName`, `libraryKey`, `componentKey`, `filePath`, and `updatedAt` from
  `search_design_system` when available.
- Record page and node IDs discovered from live metadata in review artifacts. Treat those IDs as
  navigation aids that may need refresh if Figma components are recreated.
- Do not create scratch Figma files, temporary pages, or imported instances for read-only fidelity
  review unless the user explicitly approves that heavier workflow.
- Do not use local work notes as proof of Figma structure. They can be historical hints only when the
  task explicitly allows them.
- Do not report dark-theme contrast problems as component bugs when the component is faithfully using
  Figma-provided variables. Those fixes belong in foundations/design source.

## Troubleshooting

- If metadata without a `nodeId` only returns `COVER`, use `nodeId = "0:0"` to get the top-level page
  list.
- If a node ID is rejected, re-run the page-list and page metadata steps. The component may have been
  recreated, or the ID may point to a non-persistent sublayer of an instance.
- If search returns decoy libraries, scope the search to the current `Mobile Design System` library
  key returned by `get_libraries`.
- If a family has no dedicated component page, document the search terms tried and compare against
  the nearest source-backed Figma instance, token map, or intentional Android-only wrapper.
