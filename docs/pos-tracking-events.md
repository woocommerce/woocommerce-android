# POS — Tracking Events

Do NOT use store app patterns (`AnalyticsEvent` enum, `AnalyticsTrackerWrapper`, `KEY_`/`VALUE_` constants) in POS code.

## Adding an Event

1. **Add a sealed class entry** to [`WooPosAnalyticsEvent.kt`](../WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/util/analytics/WooPosAnalyticsEvent.kt). Use `data object` for simple events and `data class` for events with properties.
   ```kotlin
   // Simple event (no properties)
   data object MyActionTapped : Event() {
       override val name: String = "my_action_tapped"
   }

   // Event with properties
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

   For error events, use the `Error` sealed class:
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

2. **Add typed constants** (if needed) to [`WooPosAnalyticsEventConstant.kt`](../WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/util/analytics/WooPosAnalyticsEventConstant.kt). POS uses typed enums instead of `KEY_`/`VALUE_` string constants:
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

3. **Track the event** via injected `WooPosAnalyticsTracker`. `track()` is a **suspend function** that dispatches to `Dispatchers.IO` and automatically adds common POS properties via `WooPosAnalyticsCommonPropertiesProvider`.
   ```kotlin
   // In ViewModel constructor:
   private val analyticsTracker: WooPosAnalyticsTracker,

   // Simple event
   analyticsTracker.track(WooPosAnalyticsEvent.Event.MyActionTapped)

   // Event with properties (passed via constructor)
   analyticsTracker.track(
       WooPosAnalyticsEvent.Event.MyFeatureLoaded(
           itemCount = items.size,
           source = ItemsListSource.PRODUCT
       )
   )
   ```

4. **Write tests** verifying the track call:
   ```kotlin
   private val analyticsTracker: WooPosAnalyticsTracker = mock()

   @Test
   fun `when action performed, then event is tracked`() = runTest {
       viewModel.onActionPerformed()
       verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.MyActionTapped)
   }
   ```

## Dedicated Tracker Classes

When a feature has many tracking calls, extract a dedicated tracker (e.g., `WooPosBarcodeEventTracker`, `WooPosPaymentStateAnalyticsTracker`) that wraps `WooPosAnalyticsTracker`.

## Naming Conventions

| Suffix | Description |
|--------|-------------|
| `_tapped` | User clicked a view |
| `_loaded` | Data populated a view |
| `_failed` | Request error |
| `_success` | Request success |
| `_selected` | User selected an option |
| `_shown` | View shown to user |
