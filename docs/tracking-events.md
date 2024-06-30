# Tracking Events

To add a new event, the event name has to be added as an `enum` in the [AnalyticsEvent](../WooCommerce/src/main/kotlin/com/woocommerce/android/analytics/AnalyticsEvent.kt).

Tracking the event looks like this:

```kotlin
class OrderDetailViewModel {
    fun onRefreshRequested() {
        AnalyticsTracker.track(Stat.ORDER_DETAIL_PULLED_TO_REFRESH)
    }
}
```

Having the values in the `AnalyticsTracker` enum helps us with comparing the events being tracked in WooCommerce iOS.

## Custom Properties

If the event has custom properties, New key and value constant for the property has to be added as a constant in the [`AnalyticsTracker`](../WooCommerce/src/main/kotlin/com/woocommerce/android/analytics/AnalyticsTracker.kt). Tracking the event would now look like this:

```kotlin
class OrderDetailViewModel {
    fun onRefreshRequested() {
        AnalyticsTracker.track(
            PRODUCT_IMAGE_UPLOAD_FAILED,
                 mapOf(
                      AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                      AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                      AnalyticsTracker.KEY_ERROR_DESC to event.error?.message
                 )
            )
    }
}
```

### Naming Conventions
| Name Token | Associated Property                                                                              | Event Type | Description |
| --- |--------------------------------------------------------------------------------------------------| --- | --- |
| _action | type                                                                                             | Action | When there are multiple actions |
| _add | *                                                                                                | Action | User request to add something new such as an order note |
| _change | "from: to:"                                                                                      | Action | When a value changes |
| _confirmation_dialog_result | result="positive                                                                                 |negative" | View | Result of a confirmation dialog |
| _date | range:                                                                                           | Action | For a date range switcher in a graph or list |
| _failed | "errorContext:$initiated-classname<br>errorType:$errorType<br>errorDescription:$errorDescription" | Data | Errors for user initiated requests |
| _filter | *                                                                                                | Action | A list has been filtered or searched |
| _loaded | *                                                                                                | Data | The data to populate a view has loaded |
| _open | *                                                                                                | Action | An item from a list was opened by the user |
| _pulled_to_refresh | type                                                                                             | View | User gesture to do a manual refresh of the view |
| _reselected | *                                                                                                | View | The user reselected a bottom bar, tab, or dropdown/list option, option. Typically preceded by _tab_ or _bar_ |
| _selected | type                                                                                             | View | The user selected a bottom bar, tab, or dropdown/list option, option, Typically preceded by _tab_ or _bar_ |
| _show | name:                                                                                            | Action | Tracks views shown to the user |
| _success | *                                                                                                | Data | A successful user initiated request |
| _tapped | *                                                                                                | View | The user clicked on a clickable view, typically preceeded by _button_, _link_, _menu |
| _toggled | state="on                                                                                        |off" | View | User toggled an option |
| _undo |                                                                                                  | Action | The user chose to undo a previous action or change |
