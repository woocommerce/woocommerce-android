# POS Cancel Booking Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a "Cancel booking" menu item to the WooPos booking details overflow menu with a confirmation dialog and API call.

**Architecture:** Extend existing `BookingAction` sealed interface with `CancelBooking`, add `CancelConfirmation` dialog state, inject `BookingsRepository` into ViewModel for the cancel API call. The booking list auto-refreshes via Room observer after successful cancel.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Hilt DI, Coroutines, existing `BookingsRepository.cancelBooking()` and `BookingEntity.isCancellable`.

---

### Task 1: Add string resources

**Files:**
- Modify: `WooCommerce/src/main/res/values/strings.xml`

**Step 1: Add new POS-specific cancel booking strings**

Add these strings near the other `woopos_bookings_` strings:

```xml
<string name="woopos_bookings_cancel_menu_item">Cancel booking</string>
<string name="woopos_bookings_cancel_dialog_title">Cancel this booking?</string>
<string name="woopos_bookings_cancel_dialog_message">Booking #%1$s for %2$s on %3$s at %4$s for %5$s will be canceled.</string>
<string name="woopos_bookings_cancel_dialog_email_notice">The customer will be notified via email.</string>
<string name="woopos_bookings_cancel_dialog_confirm">Yes, cancel booking</string>
<string name="woopos_bookings_cancel_dialog_keep">No, keep it</string>
<string name="woopos_bookings_cancel_dialog_background">Cancel booking dialog background</string>
<string name="woopos_bookings_cancel_error">Failed to cancel booking. Please try again.</string>
```

**Step 2: Commit**

```bash
git add WooCommerce/src/main/res/values/strings.xml
git commit -m "Add POS cancel booking string resources"
```

---

### Task 2: Add CancelBooking action and CancelConfirmation dialog state

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsState.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsUIEvent.kt`

**Step 1: Add CancelBooking to BookingAction sealed interface**

In `WooPosBookingsState.kt`, inside `BookingAction`, add after `EmailReceipt`:

```kotlin
@Immutable
data class CancelBooking(val bookingId: Long, override val orderId: Long) : BookingAction
```

**Step 2: Add CancelConfirmation to DialogState**

In `WooPosBookingsState.kt`, inside `Content.DialogState`, add after `IssueRefund`:

```kotlin
data class CancelConfirmation(
    val bookingId: Long,
    val message: String,
) : DialogState()
```

**Step 3: Add cancel booking UI events**

In `WooPosBookingsUIEvent.kt`, add:

```kotlin
data object CancelBookingConfirmed : WooPosBookingsUIEvent
data object CancelBookingDismissed : WooPosBookingsUIEvent
```

**Step 4: Commit**

```bash
git add WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsState.kt \
       WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsUIEvent.kt
git commit -m "Add CancelBooking action and dialog state types"
```

---

### Task 3: Update mapper to conditionally include CancelBooking action

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingViewStateMapper.kt`
- Test: `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingViewStateMapperTest.kt`

**Step 1: Write the failing tests**

In `WooPosBookingViewStateMapperTest.kt`, add:

```kotlin
@Test
fun `given cancellable booking, when mapped to details, then actions include CancelBooking`() {
    // GIVEN
    val booking = sampleBooking(status = BookingEntity.Status.Unpaid)

    // WHEN
    val result = mapper.mapToDetailsViewState(booking)

    // THEN
    val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
    assertThat(actions).anyMatch { it is WooPosBookingsState.BookingAction.CancelBooking }
}

@Test
fun `given cancelled booking, when mapped to details, then actions do not include CancelBooking`() {
    // GIVEN
    val booking = sampleBooking(status = BookingEntity.Status.Cancelled)

    // WHEN
    val result = mapper.mapToDetailsViewState(booking)

    // THEN
    val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
    assertThat(actions).noneMatch { it is WooPosBookingsState.BookingAction.CancelBooking }
}

@Test
fun `given complete booking, when mapped to details, then actions do not include CancelBooking`() {
    // GIVEN
    val booking = sampleBooking(status = BookingEntity.Status.Complete)

    // WHEN
    val result = mapper.mapToDetailsViewState(booking)

    // THEN
    val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
    assertThat(actions).noneMatch { it is WooPosBookingsState.BookingAction.CancelBooking }
}
```

Note: The existing `sampleBooking()` function may need a `status` parameter. Check the existing test helper and add the parameter if needed.

**Step 2: Run tests to verify they fail**

Run: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosBookingViewStateMapperTest"`
Expected: FAIL (CancelBooking not yet in the actions list)

**Step 3: Update the mapper**

In `WooPosBookingViewStateMapper.kt`:
- Add import: `import org.wordpress.android.fluxc.persistence.entity.isCancellable`
- In `mapToDetailsViewState()`, replace the hardcoded actions list:

```kotlin
actionsState = WooPosBookingsState.BookingActionsState.Loaded(
    buildList {
        add(WooPosBookingsState.BookingAction.EmailReceipt(booking.orderId))
        if (booking.isCancellable) {
            add(
                WooPosBookingsState.BookingAction.CancelBooking(
                    bookingId = booking.id.value,
                    orderId = booking.orderId
                )
            )
        }
    }
),
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosBookingViewStateMapperTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingViewStateMapper.kt \
       WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingViewStateMapperTest.kt
git commit -m "Add CancelBooking action to mapper based on isCancellable"
```

---

### Task 4: Add cancel booking menu item to overflow menu (red text)

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/details/WooPosBookingDetails.kt`

**Step 1: Update BookingOverflowMenu**

In `BookingOverflowMenu` composable, update the `when` block inside `DropdownMenuItem`:

```kotlin
actions.forEach { action ->
    DropdownMenuItem(
        text = {
            val (text, textColor) = when (action) {
                is WooPosBookingsState.BookingAction.EmailReceipt -> {
                    stringResource(R.string.woopos_orders_email_receipt) to
                        MaterialTheme.colorScheme.onSurface
                }
                is WooPosBookingsState.BookingAction.CancelBooking -> {
                    stringResource(R.string.woopos_bookings_cancel_menu_item) to
                        MaterialTheme.colorScheme.error
                }
            }
            WooPosText(
                text = text,
                style = WooPosTypography.BodyMedium,
                color = textColor
            )
        },
        onClick = {
            showMenu = false
            onClick(action)
        }
    )
}
```

**Step 2: Update the preview**

In `WooPosBookingDetailsPreview`, update `actionsState` to include `CancelBooking`:

```kotlin
actionsState = WooPosBookingsState.BookingActionsState.Loaded(
    listOf(
        WooPosBookingsState.BookingAction.EmailReceipt(3330L),
        WooPosBookingsState.BookingAction.CancelBooking(bookingId = 333L, orderId = 3330L)
    )
),
```

**Step 3: Commit**

```bash
git add WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/details/WooPosBookingDetails.kt
git commit -m "Add cancel booking menu item with red text in overflow menu"
```

---

### Task 5: Create cancel booking confirmation dialog

**Files:**
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/details/WooPosCancelBookingDialog.kt`

**Step 1: Create the dialog composable**

```kotlin
package com.woocommerce.android.ui.woopos.bookings.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosCancelBookingDialog(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WooPosDialogWrapper(
        modifier = modifier,
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_bookings_cancel_dialog_background
        ),
        onCloseClick = onDismiss,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_bookings_cancel_dialog_title),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosText(
                text = message,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            WooPosText(
                text = stringResource(R.string.woopos_bookings_cancel_dialog_email_notice),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.XXXLarge.value))

            WooPosButton(
                text = stringResource(R.string.woopos_bookings_cancel_dialog_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButton(
                text = stringResource(R.string.woopos_bookings_cancel_dialog_keep),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosCancelBookingDialogPreview() {
    WooPosTheme {
        WooPosCancelBookingDialog(
            isVisible = true,
            message = "Booking #333 for Women's Haircut on Monday, 05 July 2025 at 10:30 AM for Margarita Nikolaevna will be canceled.",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
```

**Step 2: Commit**

```bash
git add WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/details/WooPosCancelBookingDialog.kt
git commit -m "Create WooPosCancelBookingDialog composable"
```

---

### Task 6: Wire up ViewModel cancel flow

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModel.kt`
- Test: `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModelTest.kt`

**Step 1: Write failing tests**

In `WooPosBookingsViewModelTest.kt`:

Add `BookingsRepository` mock to the test class fields:
```kotlin
private val bookingsRepository: BookingsRepository = mock()
```

Update the import:
```kotlin
import com.woocommerce.android.ui.bookings.BookingsRepository
```

Update `createViewModel()`:
```kotlin
private fun createViewModel(): WooPosBookingsViewModel {
    return WooPosBookingsViewModel(
        bookingListHandler = bookingListHandler,
        dateTimeProvider = dateTimeProvider,
        mapper = WooPosBookingViewStateMapper(dateFormatter, resourceProvider),
        bookingsRepository = bookingsRepository,
        resourceProvider = resourceProvider,
    )
}
```

Add these tests:

```kotlin
@Test
fun `given cancel action clicked, when handling event, then dialog state is CancelConfirmation`() = runTest {
    // GIVEN
    viewModel = createViewModel()
    advanceUntilIdle()
    val content = viewModel.state.value as WooPosBookingsState.Content
    val bookingId = content.selectedDetails!!.id

    // WHEN
    viewModel.onUIEvent(
        WooPosBookingsUIEvent.BookingActionClicked(
            WooPosBookingsState.BookingAction.CancelBooking(
                bookingId = bookingId,
                orderId = bookingId * 10
            )
        )
    )
    advanceUntilIdle()

    // THEN
    val updatedContent = viewModel.state.value as WooPosBookingsState.Content
    assertThat(updatedContent.dialogState)
        .isInstanceOf(WooPosBookingsState.Content.DialogState.CancelConfirmation::class.java)
}

@Test
fun `given cancel confirmed, when handling event, then calls cancelBooking and hides dialog`() = runTest {
    // GIVEN
    whenever(bookingsRepository.cancelBooking(any())).thenReturn(Result.success(Unit))
    viewModel = createViewModel()
    advanceUntilIdle()
    val content = viewModel.state.value as WooPosBookingsState.Content
    val bookingId = content.selectedDetails!!.id

    viewModel.onUIEvent(
        WooPosBookingsUIEvent.BookingActionClicked(
            WooPosBookingsState.BookingAction.CancelBooking(
                bookingId = bookingId,
                orderId = bookingId * 10
            )
        )
    )
    advanceUntilIdle()

    // WHEN
    viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingConfirmed)
    advanceUntilIdle()

    // THEN
    verify(bookingsRepository).cancelBooking(bookingId)
    val updatedContent = viewModel.state.value as WooPosBookingsState.Content
    assertThat(updatedContent.dialogState)
        .isInstanceOf(WooPosBookingsState.Content.DialogState.Hidden::class.java)
}

@Test
fun `given cancel dismissed, when handling event, then hides dialog`() = runTest {
    // GIVEN
    viewModel = createViewModel()
    advanceUntilIdle()
    val content = viewModel.state.value as WooPosBookingsState.Content
    val bookingId = content.selectedDetails!!.id

    viewModel.onUIEvent(
        WooPosBookingsUIEvent.BookingActionClicked(
            WooPosBookingsState.BookingAction.CancelBooking(
                bookingId = bookingId,
                orderId = bookingId * 10
            )
        )
    )
    advanceUntilIdle()

    // WHEN
    viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingDismissed)
    advanceUntilIdle()

    // THEN
    val updatedContent = viewModel.state.value as WooPosBookingsState.Content
    assertThat(updatedContent.dialogState)
        .isInstanceOf(WooPosBookingsState.Content.DialogState.Hidden::class.java)
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosBookingsViewModelTest"`
Expected: FAIL (constructor mismatch and missing event handling)

**Step 3: Update the ViewModel**

In `WooPosBookingsViewModel.kt`:

Add imports:
```kotlin
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.viewmodel.ResourceProvider
```

Update constructor to inject `BookingsRepository` and `ResourceProvider`:
```kotlin
@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val bookingListHandler: BookingListHandler,
    private val dateTimeProvider: DateTimeProvider,
    private val mapper: WooPosBookingViewStateMapper,
    private val bookingsRepository: BookingsRepository,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {
```

Update `onUIEvent()` to handle new events:
```kotlin
fun onUIEvent(event: WooPosBookingsUIEvent) {
    when (event) {
        is WooPosBookingsUIEvent.BookingActionClicked -> handleBookingAction(event.action)
        is WooPosBookingsUIEvent.AttendanceToggled -> { }
        is WooPosBookingsUIEvent.PayByCardClicked -> handlePayByCard()
        is WooPosBookingsUIEvent.PayByCashClicked -> handlePayByCash()
        is WooPosBookingsUIEvent.AddBookingNoteClicked -> { }
        is WooPosBookingsUIEvent.CopyEmailClicked -> { }
        is WooPosBookingsUIEvent.CancelBookingConfirmed -> handleCancelConfirmed()
        is WooPosBookingsUIEvent.CancelBookingDismissed -> handleCancelDismissed()
    }
}
```

Update `handleBookingAction()`:
```kotlin
private fun handleBookingAction(action: WooPosBookingsState.BookingAction) {
    when (action) {
        is WooPosBookingsState.BookingAction.EmailReceipt -> {
            // TBD: handle email receipt
        }
        is WooPosBookingsState.BookingAction.CancelBooking -> {
            showCancelConfirmationDialog(action.bookingId)
        }
    }
}
```

Add new methods:
```kotlin
private fun showCancelConfirmationDialog(bookingId: Long) {
    val currentState = _state.value as? WooPosBookingsState.Content ?: return
    val details = currentState.selectedDetails ?: return

    val customerName = details.customerSection?.name
    val message = if (customerName != null) {
        resourceProvider.getString(
            R.string.woopos_bookings_cancel_dialog_message,
            details.number.removePrefix("#"),
            details.bookingName,
            details.appointmentDate,
            details.appointmentTime,
            customerName
        )
    } else {
        resourceProvider.getString(
            R.string.woopos_bookings_cancel_dialog_message,
            details.number.removePrefix("#"),
            details.bookingName,
            details.appointmentDate,
            details.appointmentTime,
            ""
        )
    }

    _state.value = currentState.copy(
        dialogState = WooPosBookingsState.Content.DialogState.CancelConfirmation(
            bookingId = bookingId,
            message = message,
        )
    )
}

private fun handleCancelConfirmed() {
    val currentState = _state.value as? WooPosBookingsState.Content ?: return
    val dialog = currentState.dialogState as? WooPosBookingsState.Content.DialogState.CancelConfirmation ?: return
    val bookingId = dialog.bookingId

    _state.value = currentState.copy(
        dialogState = WooPosBookingsState.Content.DialogState.Hidden
    )

    viewModelScope.launch {
        bookingsRepository.cancelBooking(bookingId)
    }
}

private fun handleCancelDismissed() {
    val currentState = _state.value as? WooPosBookingsState.Content ?: return
    _state.value = currentState.copy(
        dialogState = WooPosBookingsState.Content.DialogState.Hidden
    )
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosBookingsViewModelTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModel.kt \
       WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModelTest.kt
git commit -m "Wire up cancel booking flow in ViewModel"
```

---

### Task 7: Render cancel dialog in WooPosBookingsScreen

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsScreen.kt`

**Step 1: Add dialog rendering in WooPosBookingsContent**

In `WooPosBookingsContent`, after the `Row` block (which contains the list pane and detail pane), add the dialog. The dialog should be rendered as a sibling of the `Row`, inside the same parent composable. Wrap the `Row` and dialog in a `Box`:

```kotlin
@Composable
private fun WooPosBookingsContent(
    state: WooPosBookingsState.Content,
    scrollToTopEvent: SharedFlow<Unit>,
    onRefresh: () -> Unit,
    onBookingSelected: (Long) -> Unit,
    onEndOfBookingsListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ... existing list pane and detail pane code (unchanged) ...
        }

        val cancelDialog = state.dialogState as? WooPosBookingsState.Content.DialogState.CancelConfirmation
        WooPosCancelBookingDialog(
            isVisible = cancelDialog != null,
            message = cancelDialog?.message.orEmpty(),
            onConfirm = { onUIEvent(WooPosBookingsUIEvent.CancelBookingConfirmed) },
            onDismiss = { onUIEvent(WooPosBookingsUIEvent.CancelBookingDismissed) },
        )
    }
}
```

Add import:
```kotlin
import com.woocommerce.android.ui.woopos.bookings.details.WooPosCancelBookingDialog
```

**Step 2: Commit**

```bash
git add WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsScreen.kt
git commit -m "Render cancel booking dialog in bookings screen"
```

---

### Task 8: Run detekt and fix any issues

**Step 1: Run detekt**

Run: `./gradlew detektAll --auto-correct`

**Step 2: Fix any issues found**

**Step 3: Run all bookings tests**

Run: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosBookingsViewModelTest" && ./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosBookingViewStateMapperTest"`
Expected: PASS

**Step 4: Commit any fixes**

```bash
git add -A
git commit -m "Fix detekt issues in cancel booking feature"
```
