---
name: analytics
description: Analytics tracking patterns for both main app and POS. Use when writing, editing, exploring, debugging, or reviewing analytics tracking — AnalyticsEvent enum entries, AnalyticsTrackerWrapper calls, KEY_*/VALUE_* constants, or tests that verify tracking. Covers both main app (AnalyticsEvent) and POS (WooPosAnalyticsEvent) patterns.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# Analytics Tracking

## Workflow

Follow these steps when adding analytics tracking:

1. **Add enum constant** to `AnalyticsEvent.kt` (or `WooPosAnalyticsEvent.kt` for POS)
2. **Add property key/value constants** to `AnalyticsTracker.companion` if needed
3. **Track the event** via injected `AnalyticsTrackerWrapper` in ViewModels/repositories
4. **Write tests** verifying the track call with `verify(analyticsTrackerWrapper).track(...)`

## Step 1: Add the Event Enum Constant

File: `WooCommerce/src/main/kotlin/com/woocommerce/android/analytics/AnalyticsEvent.kt`

- Use `UPPER_SNAKE_CASE` (e.g., `PRODUCT_DETAIL_LOADED`)
- Place under the appropriate `// -- Section Name` comment block
- For siteless events (login/signup): `MY_EVENT(siteless = true),`
- For regular events: `MY_EVENT,`

Existing sections follow the pattern `// -- Feature Name`. Find the matching section or add a new one.

**POS exception:** POS events use `WooPosAnalyticsEvent` sealed class at:
`WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/util/analytics/WooPosAnalyticsEvent.kt`

## Step 2: Add Property Constants

File: `WooCommerce/src/main/kotlin/com/woocommerce/android/analytics/AnalyticsTracker.kt` (companion object)

Only add constants when the event carries custom properties. Check existing constants first to avoid duplicates.

```kotlin
// Keys: KEY_ prefix
const val KEY_MY_PROPERTY = "my_property"

// Values: VALUE_ prefix
const val VALUE_MY_OPTION = "my_option"
```

## Step 3: Track the Event

Inject `AnalyticsTrackerWrapper` -- never use `AnalyticsTracker` singleton directly.

```kotlin
// In ViewModel constructor:
private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,

// Simple event
analyticsTrackerWrapper.track(AnalyticsEvent.MY_EVENT)

// Event with properties
analyticsTrackerWrapper.track(
    stat = AnalyticsEvent.MY_EVENT,
    properties = mapOf(
        AnalyticsTracker.KEY_SOURCE to "some_source",
        AnalyticsTracker.KEY_TYPE to type.value
    )
)

// Error event (convenience method)
analyticsTrackerWrapper.track(
    stat = AnalyticsEvent.MY_EVENT_FAILED,
    errorContext = this::class.java.simpleName,
    errorType = error.type.toString(),
    errorDescription = error.message
)
```

**POS exception:** POS uses `WooPosAnalyticsTracker` which wraps `AnalyticsTrackerWrapper` and adds common POS properties automatically.

**Dedicated tracker classes:** When a feature has many tracking calls with shared property logic, extract a dedicated tracker class (e.g., `BarcodeScanningTracker`) that wraps `AnalyticsTrackerWrapper`. Inject this tracker into ViewModels instead of `AnalyticsTrackerWrapper` directly.

## Step 4: Write Tests

Mock `AnalyticsTrackerWrapper` and verify the call:

```kotlin
private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

@Test
fun `when action performed, then event is tracked`() = testBlocking {
    // WHEN
    viewModel.onActionPerformed()

    // THEN
    verify(analyticsTrackerWrapper).track(AnalyticsEvent.MY_EVENT)
}

@Test
fun `when action performed, then event with properties is tracked`() = testBlocking {
    // WHEN
    viewModel.onActionPerformed()

    // THEN
    verify(analyticsTrackerWrapper).track(
        stat = AnalyticsEvent.MY_EVENT,
        properties = mapOf(AnalyticsTracker.KEY_SOURCE to "expected_value")
    )
}
```

## Naming Suffix Reference

| Suffix | Type | Description |
|--------|------|-------------|
| `_tapped` | View | User clicked a view |
| `_loaded` | Data | Data populated a view |
| `_failed` | Data | User-initiated request error |
| `_success` | Data | User-initiated request success |
| `_selected` | View | User selected an option |
| `_toggled` | View | User toggled option (property: `state: "on"/"off"`) |
| `_open` | Action | Item opened from list |
| `_pulled_to_refresh` | View | Manual refresh gesture |
| `_add` | Action | User adding something new |
| `_change` | Action | Value changed (properties: `from:` / `to:`) |
| `_filter` | Action | List filtered or searched |
| `_show` | Action | View shown to user |
| `_undo` | Action | User undid a previous action |
