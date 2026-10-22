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

## Checkout timing

The events `card_present_collect_payment_success`, `card_present_collect_payment_failed`,
and `cash_collect_payment_success` include the available checkout
timing properties. Interac payments use the generic timed card-success event after payment is recorded on the store.
The supplemental `card_present_collect_interac_payment_success` processing event is outside this timing contract;
it does not consume card-confirmation timing. `card_present_collect_payment_cancelled` keeps its existing
properties, without the new timing fields or `sync_strategy`.
`sync_strategy` is the active products source at checkout: `local_catalog`, `remote`, or `unknown`.
Older loaded and outdated-item events use `local_catalog_file` for the same local source; map that value
to `local_catalog` when comparing event families.

| Optional integer milliseconds | Start boundary |
| --- | --- |
| `pos_time_since_checkout_started_ms` | Order-draft creation entry, before async work |
| `pos_time_since_order_sync_completed_ms` | Successful order creation for this checkout |
| `pos_time_since_payment_confirmed_ms` | Native/Tap to Pay capture-state entry after local receipt lookup and before backend capture, including confirmed-intent retry |

Durations use `SystemClock.elapsedRealtime()`, including sleep. Zero is valid; missing/negative values are omitted.
The Android Tracks SDK serializes custom properties as strings. These durations are integer-valued milliseconds;
cast them to numbers for analysis or comparison with iOS raw payloads.
`pos_time_since_reader_ready_ms` is omitted: the SDK input callback has no operation identity, and processing entry does not prove readiness.
Automatic product/coupon removal re-sync also restarts checkout timing; this measures a synchronization attempt, not a physical press.
Three shared timestamps live in `WooPosAnalyticsTrackingDataKeeper`; there are no per-attempt timing contexts.
Checkout entry resets stage timestamps, and an order-sync generation guard ignores results from an earlier checkout.
Success snapshots follow success-property lookup. Card outcomes clear confirmation; cancellation clears confirmation without emitting it.
Cash reads checkout/sync timing without clearing card confirmation, so overlapping card completion retains its own start boundary.
The existing cart reset clears all timestamps. Native retries retain checkout/sync timing; no unique attempt or journey identity is emitted.
Native and Tap to Pay use the existing `CapturingPayment` status after successful local receipt lookup and before
backend capture. A missing receipt fails before this status and omits confirmation timing. iOS starts this field
earlier, when the confirmed intent is available before receipt lookup; this boundary difference applies when
comparing platforms. Remote readers start it for `requires_capture`/`succeeded` before backend capture;
simulation omits confirmation.
Existing wall-clock properties, units, checkout counter and reset behavior remain unchanged. These metrics do not measure first display or card touch.

For timing coverage, select Android and iOS cohorts separately with each platform's app-version/build metadata.
Use the first released version/build that contains these fields as that platform's lower bound; the release
boundaries can differ. No runtime timing-version property is emitted. Within each cohort, count the eligible
terminal events and the events with each timing property. A missing property means its start boundary is
unavailable or its duration is invalid; it does not mean zero. Keep valid zero durations in timing distributions.
