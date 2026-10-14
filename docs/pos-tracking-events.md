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

## Checkout timing version 2

Card success, failure and cancellation, and cash success include integer `pos_timing_version: 2`.
`sync_strategy` is the active products source at checkout: `local_catalog`, `remote`, or `unknown`.

| Optional integer milliseconds | Start boundary |
| --- | --- |
| `pos_time_since_checkout_started_ms` | Order-draft creation entry, before async work |
| `pos_time_since_order_sync_completed_ms` | Successful order creation for this checkout |
| `pos_time_since_payment_confirmed_ms` | Confirmed intent available before receipt lookup/backend capture, including reuse |

Durations use `SystemClock.elapsedRealtime()`, including sleep. Zero is valid; missing/negative values are omitted.
`pos_time_since_reader_ready_ms` is omitted: the SDK input callback has no operation identity, and processing entry does not prove readiness.
Automatic product/coupon removal re-sync also starts a context; this measures a synchronization attempt, not a physical press.
Contexts are captured before async work; snapshots follow success-property lookup. Late callbacks retain their own references.
Failure/cancel clear captured confirmation; success closes only its captured collection. New collections after success omit old checkout timing.
Customer reset replaces active pointers. Native retries can reuse open collections; duplicate/late-confirmed signals do not establish attempt identity.
Cash uses its own collection and omits confirmation. Remote readers confirm only `requires_capture`/`succeeded`; simulation omits confirmation.
Existing wall-clock properties, units, checkout counter and reset behavior remain unchanged. These metrics do not measure first display or card touch.

## Ordinary remote product first page

`items_fetched` is a new Android POS event for each successful ordinary remote product page 1 result, including an empty result. It excludes cached-only collection, canceled or failed requests, prepopulation, local DB reads, pagination and search. iOS already emits this event for its own remote first-page population; these populations and query filters are not identical.

- `milliseconds_since_request_sent`: integer monotonic milliseconds around the remote store await, including request/response work and coroutine scheduling; excludes later POS mapping, cache/index work and rendering. Zero is retained; negative deltas omit the field.
- `returned_items_count`: integer product response length before POS mapping/cache/index.
- `item_type`: `product`; `sync_strategy`: `remote`.

Android omits `total_items` because this API does not supply a store total. iOS retains its existing string store-total property from response headers. This event has no checkout timing version tag and does not measure startup or first visible content.
