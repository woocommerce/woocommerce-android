# Store Design System

## Icon resources

This module owns design-system icon drawables for store UI components.

- Figma source: `Mobile-Design-System` (`50XIH5MmOf4xUYEkM6fAm6`), node `920:6066` (`Icons`)
- Resource naming: `woo_ds_ic_<style>_<name>_24dp`
- Current style buckets: `light`, `regular`, `solid`
- Current import scope: source-backed 24dp icons from the Figma `Icons` node: `29` light,
  `75` regular, and `66` solid drawables
- Duplicate Figma group names that render identically are imported once under the unsuffixed name.

Use these drawables from later component branches instead of copying app-module icons into
`libs/store-design-system`.

Compose code should prefer the `WooIcons` catalog instead of referencing drawable IDs directly:

```kotlin
Icon(
    imageVector = WooIcons.Regular.Store,
    contentDescription = null,
)
```
