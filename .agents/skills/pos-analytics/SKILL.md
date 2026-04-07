---
name: pos-analytics
description: POS analytics tracking patterns (WooPosAnalyticsEvent sealed class, WooPosAnalyticsTracker, WooPosAnalyticsEventConstant). Use when writing, editing, exploring, debugging, or reviewing analytics tracking in POS (WooPos*) code. NOT for main store app — use the `store-analytics` skill instead.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# POS Analytics Tracking

POS uses its own analytics system. Do NOT use store app patterns (`AnalyticsEvent` enum, `AnalyticsTrackerWrapper` directly, `KEY_`/`VALUE_` constants) in POS code.

## Workflow

Follow these steps when adding analytics tracking:

1. **Add sealed class entry** to `WooPosAnalyticsEvent.kt`
2. **Add typed constants** to `WooPosAnalyticsEventConstant` if needed
3. **Track the event** via injected `WooPosAnalyticsTracker` (suspend function)
4. **Write tests** verifying the track call with `verify(analyticsTracker).track(...)`

## Step 1: Add the Event

File: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/util/analytics/WooPosAnalyticsEvent.kt`

Events are entries in the `WooPosAnalyticsEvent` sealed class. Use `data object` for simple events and `data class` for events with properties.

```kotlin
// Simple event (no properties)
data object MyActionTapped : Event() {
    override val name: String = "my_action_tapped"
}

// Event with properties — add them in the init block
data class MyFeatureLoaded(val itemCount: Int, val source: ItemsListSource) : Event() {
    override val name: String = "my_feature_loaded"

    init {
        addProperties(
            mapOf(
                "item_count" to itemCount.toString(),
                ItemsListSource.SOURCE to source.value
            )
        )
    }
}
```

For error events, add entries under the `Error` sealed class:

```kotlin
data class MyFeatureError(
    override val errorContext: KClass<out Any>,
    override val errorType: String?,
    override val errorDescription: String?,
) : Error() {
    override val name: String = "my_feature_failed"
}
```

Event names use `snake_case` strings (not `UPPER_SNAKE_CASE` enums like the store app).

## Step 2: Add Property Constants (if needed)

File: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/util/analytics/WooPosAnalyticsEventConstant.kt`

POS uses typed enums for property values instead of `KEY_`/`VALUE_` string constants:

```kotlin
object WooPosAnalyticsEventConstant {
    enum class MyFeatureType(val value: String) {
        OPTION_A("option_a"),
        OPTION_B("option_b");

        override fun toString(): String = value

        companion object {
            const val FEATURE_TYPE = "feature_type"
        }
    }
}
```

Only add constants for properties shared across multiple events. For one-off properties, use inline string keys in the event's `addProperties` call.

## Step 3: Track the Event

Inject `WooPosAnalyticsTracker` — never use `AnalyticsTrackerWrapper` directly in POS.

`track()` is a **suspend function** that dispatches to `Dispatchers.IO` and automatically adds common POS properties (via `WooPosAnalyticsCommonPropertiesProvider`).

```kotlin
// In ViewModel constructor:
private val analyticsTracker: WooPosAnalyticsTracker,

// Simple event
analyticsTracker.track(WooPosAnalyticsEvent.Event.MyActionTapped)

// Event with properties (properties are passed via constructor)
analyticsTracker.track(
    WooPosAnalyticsEvent.Event.MyFeatureLoaded(
        itemCount = items.size,
        source = ItemsListSource.PRODUCT
    )
)

// Error event
analyticsTracker.track(
    WooPosAnalyticsEvent.Error.MyFeatureError(
        errorContext = this::class,
        errorType = error.type.toString(),
        errorDescription = error.message
    )
)
```

**Dedicated tracker classes:** When a feature has many tracking calls, extract a dedicated tracker class (e.g., `WooPosBarcodeEventTracker`, `WooPosPaymentStateAnalyticsTracker`) that wraps `WooPosAnalyticsTracker`. Inject this tracker into ViewModels instead.

## Step 4: Write Tests

Mock `WooPosAnalyticsTracker` and verify the call:

```kotlin
private val analyticsTracker: WooPosAnalyticsTracker = mock()

@Test
fun `when action performed, then event is tracked`() = runTest {
    // WHEN
    viewModel.onActionPerformed()

    // THEN
    verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.MyActionTapped)
}

@Test
fun `when action with data performed, then event with properties is tracked`() = runTest {
    // WHEN
    viewModel.onFeatureLoaded()

    // THEN
    verify(analyticsTracker).track(
        WooPosAnalyticsEvent.Event.MyFeatureLoaded(
            itemCount = 5,
            source = ItemsListSource.PRODUCT
        )
    )
}
```

## Naming Suffix Reference

| Suffix | Description |
|--------|-------------|
| `_tapped` | User clicked a view |
| `_loaded` | Data populated a view |
| `_failed` | Request error |
| `_success` | Request success |
| `_selected` | User selected an option |
| `_shown` | View shown to user |
