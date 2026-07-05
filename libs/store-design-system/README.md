# Store Design System

## Icon resources

This module owns design-system icon drawables for store UI components.

- Figma source: `Mobile-Design-System` (`50XIH5MmOf4xUYEkM6fAm6`), node `920:6066` (`Icons`)
- Resource naming: `woo_ds_ic_<style>_<name>_24dp`
- Current style buckets: `light`, `regular`, `solid`
- Current import scope: all source-backed 24dp icons from the Figma `Icons` node: `27` light,
  `73` regular, and `64` solid drawables
- Duplicate Figma group names that render identically are imported once under the unsuffixed name.

Use these drawables from later component branches instead of copying app-module icons into
`libs/store-design-system`.

Compose code should prefer the `WooDsIcons` catalog instead of referencing drawable IDs directly:

```kotlin
Icon(
    imageVector = WooDsIcons.Regular.Store,
    contentDescription = null,
)
```
