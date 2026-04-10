# Tracking Events

This project has two analytics systems — one for the **store management app** and one for **POS**. Use the correct one based on which part of the codebase you're working in.

## Store App Analytics

### Adding an Event

1. **Add an enum constant** to [`AnalyticsEvent.kt`](../WooCommerce/src/main/kotlin/com/woocommerce/android/analytics/AnalyticsEvent.kt)
   - Use `UPPER_SNAKE_CASE` (e.g., `PRODUCT_DETAIL_LOADED`)
   - Place under the appropriate `// -- Section Name` comment block
   - For siteless events (login/signup): `MY_EVENT(siteless = true),`

2. **Add property key/value constants** (if the event carries custom properties) to [`AnalyticsTracker.kt`](../WooCommerce/src/main/kotlin/com/woocommerce/android/analytics/AnalyticsTracker.kt) companion object. Check existing constants first to avoid duplicates.
   ```kotlin
   const val KEY_MY_PROPERTY = "my_property"
   const val VALUE_MY_OPTION = "my_option"
   ```

3. **Track the event** via injected `AnalyticsTrackerWrapper` — never use `AnalyticsTracker` singleton directly.
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

4. **Write tests** verifying the track call:
   ```kotlin
   private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

   @Test
   fun `when action performed, then event is tracked`() = testBlocking {
       viewModel.onActionPerformed()
       verify(analyticsTrackerWrapper).track(AnalyticsEvent.MY_EVENT)
   }
   ```

### Dedicated Tracker Classes

When a feature has many tracking calls with shared property logic, extract a dedicated tracker class (e.g., `BarcodeScanningTracker`) that wraps `AnalyticsTrackerWrapper`. Inject this tracker into ViewModels instead of `AnalyticsTrackerWrapper` directly.

## POS Analytics

POS uses its own analytics system. Do NOT use store app patterns (`AnalyticsEvent` enum, `AnalyticsTrackerWrapper`, `KEY_`/`VALUE_` constants) in POS code.

### Adding an Event

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

3. **Track the event** via injected `WooPosAnalyticsTracker`. `track()` is a **suspend function** that dispatches to `Dispatchers.IO` and automatically adds common POS properties.
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

### Dedicated Tracker Classes

Same pattern as store app — when a feature has many tracking calls, extract a dedicated tracker (e.g., `WooPosBarcodeEventTracker`, `WooPosPaymentStateAnalyticsTracker`) that wraps `WooPosAnalyticsTracker`.

## Key Differences: Store vs POS

| Aspect | Store App | POS |
|--------|-----------|-----|
| Event definition | `AnalyticsEvent` enum | `WooPosAnalyticsEvent` sealed class |
| Event naming | `UPPER_SNAKE_CASE` enum | `snake_case` string |
| Property constants | `KEY_`/`VALUE_` strings | Typed enums |
| Tracker class | `AnalyticsTrackerWrapper` | `WooPosAnalyticsTracker` |
| Track function | Regular function | Suspend function (dispatches to IO) |
| Common properties | None | Auto-added via `WooPosAnalyticsCommonPropertiesProvider` |

## Naming Conventions

Both systems follow the same naming suffix conventions:

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
| `_show` / `_shown` | Action | View shown to user |
| `_undo` | Action | User undid a previous action |
| `_action` | Action | When there are multiple actions (property: `type`) |
| `_confirmation_dialog_result` | View | Result of a confirmation dialog (property: `result="positive\|negative"`) |
| `_date` | Action | Date range switcher (property: `range:`) |
| `_reselected` | View | User reselected a tab or option |
