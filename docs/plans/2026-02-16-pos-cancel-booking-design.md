# Cancel Booking - WooPos Booking Details

## Overview

Add a "Cancel booking" menu item to the overflow menu in WooPos booking details. When tapped, show a confirmation dialog. On confirm, cancel the booking via existing `BookingsRepository.cancelBooking()`.

## Current State

- Overflow menu has only `EmailReceipt` action
- `BookingAction` sealed interface has one case: `EmailReceipt`
- `DialogState` has `Hidden` and `IssueRefund`
- ViewModel depends on `BookingListHandler` (no direct access to `BookingsRepository`)
- Non-POS booking details already has full cancel flow via `BookingsRepository.cancelBooking()`
- `BookingEntity.isCancellable` extension exists (false for Cancelled, InCart, Complete)

## Design

### 1. State: `BookingAction.CancelBooking`

Add to `WooPosBookingsState.BookingAction`:
```kotlin
data class CancelBooking(val bookingId: Long, override val orderId: Long) : BookingAction
```

### 2. State: `DialogState.CancelConfirmation`

Add to `WooPosBookingsState.Content.DialogState`:
```kotlin
data class CancelConfirmation(
    val bookingId: Long,
    val message: String,
) : DialogState()
```

The message is pre-formatted by the ViewModel (e.g. "Booking #333 for Women's Haircut on Monday, 05 July 2025 at 10:30 AM for Margarita Nikolaevna will be canceled.").

### 3. Mapper: Conditionally add CancelBooking

In `WooPosBookingViewStateMapper.mapToDetailsViewState()`, use `BookingEntity.isCancellable` to conditionally include `CancelBooking` in the actions list alongside `EmailReceipt`.

### 4. UI: Overflow Menu - Red text

In `BookingOverflowMenu`, add a `CancelBooking` branch with `MaterialTheme.colorScheme.error` text color.

### 5. UI: Confirmation Dialog

New composable `WooPosCancelBookingDialog` using `WooPosDialogWrapper`:
- X close button (via `onCloseClick`)
- Title: "Cancel this booking?"
- Body: pre-formatted message + "\n\nThe customer will be notified via email."
- Primary button (filled): "Yes, cancel booking"
- Secondary button (outlined): "No, keep it"

### 6. ViewModel

- Inject `BookingsRepository` (for `cancelBooking()`)
- `CancelBooking` action -> build message string, set `DialogState.CancelConfirmation`
- New UI events: `CancelBookingConfirmed`, `CancelBookingDismissed`
- On confirm: call `bookingsRepository.cancelBooking(bookingId)`, booking list auto-refreshes via Room observer
- On failure: show snackbar error
- On dismiss: set `DialogState.Hidden`

### 7. New POS-specific strings

- `woopos_bookings_cancel_dialog_title`: "Cancel this booking?"
- `woopos_bookings_cancel_dialog_message`: "Booking #%1$s for %2$s on %3$s at %4$s for %5$s will be canceled."
- `woopos_bookings_cancel_dialog_email_notice`: "The customer will be notified via email."
- `woopos_bookings_cancel_dialog_confirm`: "Yes, cancel booking"
- `woopos_bookings_cancel_dialog_keep`: "No, keep it"
- `woopos_bookings_cancel_menu_item`: "Cancel booking"

### 8. Files to Change

| File | Change |
|------|--------|
| `WooPosBookingsState.kt` | Add `CancelBooking` action, `CancelConfirmation` dialog state |
| `WooPosBookingsUIEvent.kt` | Add `CancelBookingConfirmed` and `CancelBookingDismissed` events |
| `WooPosBookingViewStateMapper.kt` | Conditionally add `CancelBooking` based on `isCancellable` |
| `WooPosBookingsViewModel.kt` | Inject `BookingsRepository`, handle cancel flow |
| `WooPosBookingDetails.kt` | Red text for cancel in overflow menu |
| New: `WooPosCancelBookingDialog.kt` | Confirmation dialog composable |
| `WooPosBookingsScreen.kt` | Render cancel dialog based on `dialogState` |
| `strings.xml` | Add new POS-specific cancel strings |