# POS Bookings Screen - Design Document

## Overview

Add a bookings screen to the POS (Point of Sale) that lets merchants view, manage, and collect payment for bookings. The screen follows the same visual approach as `WooPosOrdersScreen` (split-pane layout) and is accessible from the POS floating toolbar menu.

This is a POC branch - no feature flag needed.

## Architecture

### Package Structure

New package: `com.woocommerce.android.ui.woopos.bookings`

```
woopos/bookings/
  WooPosBookingsScreen.kt          # Main screen (split-pane)
  WooPosBookingsViewModel.kt       # State management
  WooPosBookingsState.kt           # UI state sealed classes
  WooPosBookingsDataSource.kt      # API calls via BookingsRestClient
  WooPosBookingsNavigation.kt      # Route + NavGraph registration
  WooPosBookingListItemMapper.kt   # BookingDto -> UI model
  WooPosBookingDetailMapper.kt     # BookingDto -> detail UI model
  payment/
    WooPosBookingCardPaymentScreen.kt     # Card payment (self-contained)
    WooPosBookingCardPaymentViewModel.kt  # Card payment state
    WooPosBookingCardPaymentNavigation.kt # Route
```

### Data Layer

**`WooPosBookingsDataSource`** calls `BookingsRestClient` directly. No BookingsStore, no DAO, no local caching.

```kotlin
class WooPosBookingsDataSource @Inject constructor(
    private val restClient: BookingsRestClient,
    private val selectedSite: SelectedSite,
) {
    suspend fun fetchBookings(tab: BookingTab, page: Int): Result<FetchResult>
    suspend fun fetchBooking(bookingId: Long): Result<BookingDto>
    suspend fun updateBookingStatus(bookingId: Long, status: String): Result<BookingDto>
    suspend fun updateAttendanceStatus(bookingId: Long, status: String): Result<BookingDto>
}

data class FetchResult(
    val bookings: List<BookingDto>,
    val hasMorePages: Boolean,
)
```

**Tab-to-filter mapping:**

| Tab       | API params                                                            |
|-----------|-----------------------------------------------------------------------|
| Today     | `start_date_after` = start of day, `start_date_before` = end of day, `order` = ASC |
| Upcoming  | `start_date_after` = end of today, `order` = ASC                     |
| Canceled  | Fetch with status filter (verify API support, fallback to client-side filter) |
| All       | No date/status filter, `order` = DESC                                |

No caching at all - each tab switch or refresh fetches from API.

### UI State

```kotlin
sealed class WooPosBookingsState {
    data object Loading : WooPosBookingsState()
    data object Error : WooPosBookingsState()
    data object Empty : WooPosBookingsState()
    data class Content(
        val selectedTab: BookingTab,
        val items: List<WooPosBookingListItem>,
        val selectedDetail: WooPosBookingDetail?,
        val paginationState: WooPosPaginationState,
        val pullToRefreshState: WooPosPullToRefreshState,
        val dialogState: DialogState,
    ) : WooPosBookingsState()
}

enum class BookingTab { Today, Upcoming, Canceled, All }

sealed class DialogState {
    data object Hidden : DialogState()
    data class CancelConfirmation(val bookingId: Long) : DialogState()
}
```

### ViewModel

**`WooPosBookingsViewModel`** responsibilities:
- Fetch bookings on tab change (clear list, fetch fresh)
- Pagination: load more at 5 items from bottom (same pattern as orders)
- Select booking -> populate detail pane
- Update attendance status -> API call -> refresh single booking
- Cancel booking -> confirmation dialog (using `WooPosDialogWrapper` without close button) -> API call -> refresh list
- Trigger payment flow -> navigate to card/cash screen
- View order -> send navigation event with order number

### Screen Layout

Split-pane layout matching `WooPosOrdersScreen`:
- **Left pane (30%):** Tab chips (Today, Upcoming, Canceled, All) + booking list with pull-to-refresh
- **Right pane (70%):** Selected booking detail or empty state placeholder

**List item shows:**
- Customer name
- Service/product name
- Start time
- Amount
- Booking status badge (payment status)
- Attendance status badge

**Detail pane shows:**
- Booking summary (customer, service, date/time)
- Status badge (payment status)
- Attendance status with inline chips (Booked, Checked-In, No-Show, Cancelled) - directly selectable, no bottom sheet
- Payment actions (Pay by Card / Pay by Cash) - only for unpaid bookings with a linked order
- Cancel button - only for cancellable bookings
- View Order button - only when orderId is present

## Payment

### Cash Payment

**Reuse existing `WooPosCashPaymentScreen`** - navigate to `home/cash_payment/{orderId}` with the booking's linked orderId. No changes to the existing cash payment code.

After cash payment completes and navigates back, the bookings screen:
1. Calls `BookingsRestClient.updateBooking(bookingId, status=paid)` to update booking status
2. Refreshes the booking in the list

### Card Payment

**New self-contained screen: `WooPosBookingCardPaymentScreen`**

Uses `CardReaderPaymentControllerFactory` directly with the booking's orderId. Manages its own payment states independently from the totals screen. Reuses existing composables for visual states:
- `WooPosTotalsPaymentInProgressScreen` (processing animation)
- `WooPosPaymentSuccessScreen` (success checkmark)
- `WooPosTotalsPaymentFailedScreen` (error with retry)

After card payment succeeds, also updates booking status to paid via BookingsRestClient.

**Zero changes to existing POS checkout code. All additive.**

### Payment States

```
Ready -> "Tap, insert, or swipe card" + amount + Cancel
Processing -> animation + "Processing payment..."
Success -> checkmark + amount + "Done"
Error -> error message + "Try Again" + "Cancel"
```

## Navigation

### Menu Integration

Add "Bookings" menu item in `WooPosHomeFloatingToolbarViewModel`, placed before Orders:
1. Bookings
2. Orders
3. Settings
4. Exit

### Routes

| Route                                  | Screen                          |
|----------------------------------------|---------------------------------|
| `home/bookings`                        | WooPosBookingsScreen            |
| `home/bookings/card_payment/{orderId}` | WooPosBookingCardPaymentScreen  |

Cash payment reuses existing `home/cash_payment/{orderId}` route.

### Navigation Events

New events in `WooPosNavigationEvent`:
- `OpenBookings` - from menu
- `OpenBookingCardPayment(orderId, amount)` - from booking detail
- `OpenHomeFromBookingCardPayment` - back after card payment

New child-to-parent events:
- `ChildToParentEvent.NavigationEvent.ToBookings`
- `ChildToParentEvent.NavigationEvent.ToOrderWithSearch(orderNumber)` - for "View Order"

### View Order Flow

"View Order" button navigates to the orders screen with the order number pre-filled in search. This bypasses the `createdVia=pos-rest-api` filter for that search.

Flow: booking detail -> `ToOrderWithSearch(orderNumber)` -> parent -> `OpenOrdersWithSearch(orderNumber)` -> orders screen with search pre-filled.

### Cash Payment Return Flow

After cash payment completes:
1. Cash payment screen navigates back (existing behavior)
2. Bookings screen detects return (via `SavedStateHandle` or lifecycle)
3. Updates booking status to paid via API
4. Refreshes the list

## Confirmation Dialog

Cancel booking uses `WooPosDialogWrapper` without the close button (no `onCloseClick`). Just confirm and cancel action buttons.

## Key Decisions

- **Split-pane layout** matching orders screen (30/70 split)
- **4 tabs:** Today, Upcoming, Canceled, All
- **Inline attendance chips** in detail pane (no bottom sheet)
- **No feature flag** - POC branch only
- **Reuse BookingsRestClient** but not BookingsStore/DAO
- **No local caching** - fetch from API on every interaction
- **Cash payment reuse** - existing screen, no modifications
- **Card payment** - new self-contained screen, no modifications to checkout

---

## POC Findings

These are architectural decisions and trade-offs made for the POC that should be discussed for the production implementation.

### 1. Data Layer Isolation

POS bookings bypass `BookingsStore` and `BookingsDao` entirely, calling `BookingsRestClient` directly. This avoids polluting the main app's locally stored booking data. The main app uses `observeBookings()` flows from the DAO, and POS writes to the same tables would cause interference.

**For production:** Consider whether a shared data layer with proper site/context isolation is needed, or if the direct REST client approach is the right long-term pattern for POS.

### 2. No Local Caching

POS bookings use no caching - neither database nor in-memory. Each tab switch or screen open fetches fresh from the API. This keeps the implementation simple and avoids cache invalidation complexity.

**For production:** Evaluate whether in-memory caching improves UX, especially for frequent tab switching. The orders screen uses an `AtomicReference`-based in-memory cache for comparison.

### 3. Canceled Tab

POS adds a dedicated "Canceled" tab that is not present in the main app (which has Today, Upcoming, All). The wider tablet layout gives room for 4 tab chips. Need to verify whether the bookings API supports server-side `status=cancelled` filtering or if client-side filtering is needed.

**For production:** Consider whether the main app should adopt this tab for consistency.

### 4. View Order Workaround

"View Order" navigates to the orders screen with the order number pre-filled in search. This is needed because the orders screen filters by `createdVia=pos-rest-api`, but booking orders are created by customers (not POS). The search bypasses this filter.

**For production:** Consider adding a dedicated "view single order by ID" mode to the orders screen, or a lightweight standalone order detail component.

### 5. Payment Flow - Cash

The existing `WooPosCashPaymentScreen` is generic enough to reuse for bookings. It takes an `orderId`, marks the order as completed with COD payment method. The only addition is updating the booking status to "paid" after the order payment completes.

**For production:** This reuse pattern works well and could be the standard approach.

### 6. Payment Flow - Card

Card payment is tightly coupled to `WooPosTotalsViewModel` in the existing checkout flow. The POC creates a separate `WooPosBookingCardPaymentScreen` that independently uses `CardReaderPaymentControllerFactory`. This avoids any modifications to the existing checkout code.

**For production:** Consider extracting a shared, reusable payment collection screen that both the checkout flow and bookings can use. This should be done carefully to avoid regressions in the checkout flow.

### 7. No Modifications to Existing Code (Principle)

The POC deliberately avoids modifying existing POS checkout/orders code to minimize side effects. All booking functionality is additive. The only changes to existing files are:
- Menu items list (add "Bookings" entry)
- Navigation graph (add bookings routes)
- Navigation events (add booking-related events)
- Child-to-parent events (add booking navigation events)
- Orders screen (accept optional search parameter for "View Order")

### 8. Payment UI Not Reusable

Existing payment composables (`WooPosTotalsPaymentInProgressScreen`, `WooPosPaymentSuccessScreen`, `WooPosTotalsPaymentFailedScreen`) are tightly coupled to `WooPosTotalsViewState` and `WooPosTotalsUIEvent`. They cannot be called directly from the bookings payment screen. The POC copies the visual patterns (Lottie animation, success checkmark, error layout) into a standalone `WooPosBookingCardPaymentScreen`.

**For production:** Extract shared, reusable payment composables that accept simple parameters (amount label, callbacks) instead of being tied to the totals ViewModel state.

### 9. Card Reader Connection Check Required

The original design did not mention checking whether a card reader is connected before starting payment. The POC adds a `ReaderNotConnected` state to the card payment screen that shows a "Connect Reader" button and automatically starts payment once a reader connects (same pattern as the main POS checkout flow via `WooPosCardReaderFacade`).

**For production:** This is essential for any card payment flow. Consider extracting a shared "ensure reader connected" step.

### 10. Cash Payment Returns to Home, Not Bookings

After cash payment completes, the existing `WooPosCashPaymentScreen` navigates via `OpenHomeFromCashPaymentAfterSuccessfulPayment`, which pops the entire backstack to the home screen. The bookings screen is not in the backstack after this. The booking status is not automatically updated to "paid" after cash payment.

**For production:** Either modify the cash payment return flow to support returning to the previous screen (not just home), or add a listener that updates the booking status when the bookings screen resumes.

### 11. View Order Search Pre-fill Not Functional

The "View Order" button navigates to the orders screen and sets `searchQuery` on `savedStateHandle`, but the orders screen ViewModel does not read this value on init. The order number is passed but ignored. The user sees the orders screen without any search pre-filled.

**For production:** The orders ViewModel needs to check `savedStateHandle` for a `searchQuery` key on init and trigger a search if present.

### 12. Cancel Dialog Uses AlertDialog

The design specified using `WooPosDialogWrapper` for the cancel confirmation dialog. The POC uses a standard `AlertDialog` instead because `WooPosDialogWrapper` requires an `onCloseClick` parameter (close button) which is not wanted for a simple confirm/cancel dialog.

**For production:** Either add a `WooPosDialogWrapper` variant that supports no close button, or stick with `AlertDialog` styled with POS theme colors.

### 13. UiStringParser for Payment Errors

`PaymentFlowError.message` returns a `UiString` type (not `@StringRes Int`). The card payment ViewModel uses `UiStringParser.asString()` to resolve error messages, not `ResourceProvider.getString()`. This is a pattern worth noting for any code that handles card reader error messages.

### 14. WooPosSpacing Gap at 56dp

The toolbar height spacer uses 56dp, but `WooPosSpacing` enum does not have a 56dp value (jumps from 48dp `XXXLarge` to 80dp `Huge`). The POC suppresses the `WooPosDesignSystemSpacingUsageRule` detekt rule for these spacers.

**For production:** Consider adding a 56dp spacing value to `WooPosSpacing` or using a different approach for toolbar height compensation.