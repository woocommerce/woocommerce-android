# Store App — Tracking Events

## Adding an Event

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

## Dedicated Tracker Classes

When a feature has many tracking calls with shared property logic, extract a dedicated tracker class (e.g., `BarcodeScanningTracker`) that wraps `AnalyticsTrackerWrapper`. Inject this tracker into ViewModels instead of `AnalyticsTrackerWrapper` directly.

## Naming Conventions

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
| `_action` | Action | When there are multiple actions (property: `type`) |
| `_confirmation_dialog_result` | View | Result of a confirmation dialog (property: `result="positive\|negative"`) |
| `_date` | Action | Date range switcher (property: `range:`) |
| `_reselected` | View | User reselected a tab or option |
