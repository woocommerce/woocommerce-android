# POS Bookings POC - Findings and Discussion

## Background

I spent a day building a POC for POS Bookings ([PRD](https://ciabp2.wordpress.com/2026/02/05/prd-ciab-pos-and-bookings-in-person-payments-mlp/)) to go through every flow end-to-end and find potential pitfalls early so we can figure out how to deal with them. The goal was not to build production-ready UI but to stress-test the technical path and surface blockers.

The POC branch (`feature/pos-bookings-poc`) covers the full scope from Milestones 1 and 2 in the PRD, and partially covers Milestone 3 (View Order works, refunds are handled through the View Order flow using the existing order refund mechanism).

## What the POC Covers

The implementation follows the same approach as POS Historical Orders - split-pane layout, tab-based filtering, pagination, pull-to-refresh.

- Bookings screen accessible from the POS floating toolbar menu
- Split-pane list/detail layout (30/70) matching the orders screen
- 4 tabs: Today, Upcoming, Canceled, All
- Booking detail with customer name, service, date/time, amount, status badges
- Attendance status toggle (Attended / Unattended)
- Cancel booking with confirmation dialog
- Card payment - self-contained screen using `CardReaderPaymentControllerFactory`
- Cash payment - reuses existing `WooPosCashPaymentScreen` without changes
- View Order - navigates to the orders screen with search pre-filled
- Refunds - handled via the View Order flow (merchant opens the linked order and refunds from there)

## Technical Approach

The main principle was to keep this code **self-contained and disposable** with minimal side effects on existing POS code.

- Everything lives in `woopos/bookings/` package
- **No caching** - calls `BookingsRestClient` directly, bypasses `BookingsStore` and `BookingsDao` entirely. Each tab switch or refresh fetches from the API. This avoids interfering with main app's booking data that uses `observeBookings()` flows from the DAO
- **Cash payment** screen and **send receipt** screen are fully reused as-is. The only change needed is navigation wiring - cash payment needs to know it was opened from bookings (not checkout) so it returns to the right screen after completion
- **Card reader connection** flow is reused from the main app (same `WooPosCardReaderFacade` pattern as the home screen)
- **Card payment UI** could not be reused - existing composables (`WooPosTotalsPaymentInProgressScreen`, `WooPosPaymentSuccessScreen`, etc.) are coupled to `WooPosTotalsViewState`. The POC creates a standalone payment screen that copies the visual patterns but uses its own ViewModel and `CardReaderPaymentControllerFactory` directly. This feels like the right balance: we avoid touching the existing checkout flow (no risk of breaking it) while keeping the bookings payment code self-contained and easy to throw away or refactor later. The duplication is intentional and manageable
- Changes to existing files are minimal: menu items list, navigation graph, navigation events, and child-to-parent events

## Findings

These are the things I ran into. Ordered roughly by impact.

### CardReaderPaymentCollectibilityChecker Blocks Booking Orders

This is the biggest one. `CardReaderPaymentCollectibilityChecker.isCollectable()` rejects booking orders because it checks the payment method against a short allow-list (`""`, `cod`, `woocommerce_payments`, `stripe`). Booking orders created on the web use `woocommerce_bookings_gateway`, so the check fails. When it fails, the controller calls `exitWithSnackbar()` which emits an event but does NOT update `paymentState`. Since the card payment VM only listens to `paymentState`, the screen gets stuck on Loading forever with no error shown.

The same checker also validates order status (only `pending`, `processing`, `on-hold`, `auto-draft`, `failed`). Booking orders could fail this check too depending on how they were created.

**Question:** Do we bypass the checker for POS booking payments (since the bookings screen already validates payability), or do we extend it to support booking order payment methods?

### Email Receipt Does Not Work for Booking Orders

After a successful payment, the send receipt screen is shown (reused from the existing POS flow), but actually sending the email fails:

```json
{
  "code": "woocommerce_rest_invalid_email_template",
  "message": "customer_pos_completed_order is not a valid template for this order."
}
```

The POS email receipt endpoint uses the `customer_pos_completed_order` template, but booking orders don't support it. Fixing this requires backend work - either register a POS-compatible template for booking orders or fall back to a generic completion email.

**Question:** Do we actually need to send a receipt for booking payments in MLP? The customer already received a booking confirmation email when they booked online. If we do need it, this is a backend dependency and we should figure out timeline early.

### Attendance API Values Were Wrong

The WooCommerce Bookings plugin (v3.1.0) only accepts two attendance values: `"attended"` and `"unattended"`. The Android app was sending `"checked-in"`, `"booked"`, `"no-show"`, `"cancelled"` - all rejected by the API via `rest_not_in_enum` validation.

Reading was also broken: the API returned `"attended"` / `"unattended"` but `fromKey()` didn't recognize them, so attendance was never shown in the UI.

Fixed in the POC by adding `Attended` and `Unattended` to `BookingEntity.AttendanceStatus` and deprecating the old values. **The main app's booking detail screen still uses the old values and needs to be migrated.**

### Bookings Without Linked Orders

Some bookings created via wp-admin have no linked order (`orderId = 0`). These show up in the API response but can't be paid through POS since payment requires an orderId. The POC hides the payment buttons for these, but they still appear in the list.

**Question:** Should we hide them entirely from POS? Show them as read-only? Or create an order on-the-fly?

### View Order is a Workaround

The POS orders screen filters by `createdVia=pos-rest-api`, but booking orders are created by customers on the web. So "View Order" navigates to the orders screen with the order number pre-filled in search, which bypasses the `createdVia` filter. It works but the orders ViewModel needed changes to read a search query from `savedStateHandle` on init.

**Question:** The PRD lists this as Milestone 3. Do we need it for MLP? If yes, should we build a proper "view single order by ID" mode instead of hacking the search?

### Card Payment UI Duplication

As mentioned in the technical approach, the existing payment composables can't be reused directly. The POC duplicates the visual patterns into a standalone screen. We could extract shared reusable composables that both flows use, but that means modifying the existing checkout code and risking regressions in a flow that already works well.

**Question:** Are we ok with the duplication for MLP and extract shared components later, or do we want to invest in that refactor now?

### Other Notes

- **Card reader connection check** - not in the original design, but essential. Without it, "Pay by Card" with no reader connected would just hang. The POC adds a `ReaderNotConnected` state with auto-connect, same pattern as the main checkout flow.
- **Cash payment return flow** - after cash payment the user needs to return to the bookings screen, not the POS home. The POC adds a `source` parameter to cash payment so it knows where to navigate back. Small change to existing code.
- **No caching** - acceptable for now, but if frequent tab switching feels slow we might want in-memory caching later (the orders screen does this with `AtomicReference`).

The POC branch is available for anyone to look at: `feature/pos-bookings-poc`.
