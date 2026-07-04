# Store Design System

## Icon resources

This module owns design-system icon drawables for store UI components.

- Figma source: `Mobile-Design-System` (`50XIH5MmOf4xUYEkM6fAm6`), node `920:6066` (`Icons`)
- Resource naming: `woo_ds_ic_<style>_<name>_24dp`
- Current style buckets: `light`, `regular`, `solid`
- Current import scope: all source-backed 24dp icons from the Figma `Icons` node: `27` light,
  `74` regular, and `66` solid drawables
- Duplicate Figma group names keep Figma's `_2` suffix before `_24dp`, for example
  `woo_ds_ic_solid_plus_2_24dp`

Use these drawables from later component branches instead of copying app-module icons into
`libs/store-design-system`.
