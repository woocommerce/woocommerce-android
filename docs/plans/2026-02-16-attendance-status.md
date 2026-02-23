# Attendance Status Change Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Wire the attendance toggle to the network API with optimistic updates and fix the UI to match Figma.

**Architecture:** The network stack already exists (BookingsRepository -> BookingsStore -> BookingsRestClient). We add a new small toggle button component, restructure the attendance section layout to match Figma, inject BookingsRepository into the ViewModel, and implement optimistic state updates with rollback on failure.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt DI, Coroutines, mockito-kotlin, AssertJ

---

### Task 1: Add WooPosToggleButtonSmall component

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/composeui/component/WooPosButtons.kt:194` (after WooPosToggleButton)

**Step 1: Add the composable after line 194**

Insert `WooPosToggleButtonSmall` — identical to `WooPosToggleButton` but with 40dp height, 20dp loading indicator, and `BodySmall` text:

```kotlin
@Composable
fun WooPosToggleButtonSmall(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    state: WooPosButtonState = WooPosButtonState.ENABLED,
    onClick: () -> Unit,
) {
    val borderColor = when {
        state == WooPosButtonState.DISABLED -> WooPosTheme.colors.disabledContainer
        else -> MaterialTheme.colorScheme.inverseSurface
    }
    Button(
        modifier = modifier,
        height = 40.dp,
        loadingIndicatorSize = 20.dp,
        textStyle = WooPosTypography.BodySmall,
        text = text,
        border = BorderStroke(2.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.inverseSurface
            } else {
                WooPosTheme.colors.transparent
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.inverseOnSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            disabledContainerColor = if (isSelected) {
                WooPosTheme.colors.disabledContainer
            } else {
                WooPosTheme.colors.transparent
            },
            disabledContentColor = WooPosTheme.colors.onDisabledContainer,
        ),
        state = state,
        onClick = onClick,
    )
}
```

**Step 2: Add preview entries in `WooPosSmallButtonsPreview`**

Add at the end of the preview Column (before the closing `}`):

```kotlin
Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

WooPosToggleButtonSmall(
    text = "Toggle Small Selected",
    isSelected = true,
    onClick = {}
)

Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

WooPosToggleButtonSmall(
    text = "Toggle Small Unselected",
    isSelected = false,
    onClick = {}
)
```

**Step 3: Add to git**

```bash
git add WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/composeui/component/WooPosButtons.kt
```

---

### Task 2: Restructure BookingAttendanceSection to match Figma

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/details/WooPosBookingDetails.kt:327-371`

The Figma shows:
- NO card wrapper
- Single Row: "Attendance status" title (BodyLarge, bold) on left, two small pill toggle buttons on right
- Hint text below the row

**Step 1: Replace the `BookingAttendanceSection` composable (lines 327-371)**

Replace the entire function with:

```kotlin
@Composable
private fun BookingAttendanceSection(
    attendanceSection: WooPosBookingsState.AttendanceSection,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_bookings_details_attendance_title),
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)
            ) {
                WooPosToggleButtonSmall(
                    text = stringResource(R.string.woopos_bookings_details_attendance_attended),
                    isSelected = attendanceSection.selection == WooPosBookingsState.AttendanceState.ATTENDED,
                    onClick = { onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(true)) },
                )

                WooPosToggleButtonSmall(
                    text = stringResource(R.string.woopos_bookings_details_attendance_unattended),
                    isSelected = attendanceSection.selection == WooPosBookingsState.AttendanceState.UNATTENDED,
                    onClick = { onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(false)) },
                )
            }
        }

        Spacer(Modifier.height(WooPosSpacing.Small.value))

        WooPosText(
            text = stringResource(R.string.woopos_bookings_details_attendance_hint),
            style = WooPosTypography.BodySmall,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )
    }
}
```

**Step 2: Add the `WooPosToggleButtonSmall` import at the top of the file**

Add import: `import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToggleButtonSmall`

Remove unused imports: `WooPosToggleButton`, `WooPosCard`, `ShadowType` (only if no other usages in the file — WooPosCard and ShadowType are used elsewhere so keep them).

**Step 3: Add to git**

```bash
git add WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/details/WooPosBookingDetails.kt
```

---

### Task 3: Write failing tests for attendance toggle in ViewModel

**Files:**
- Modify: `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModelTest.kt`

We need to add `BookingsRepository` mock and update `createViewModel()`. Then add 3 tests.

**Step 1: Add the BookingsRepository mock field and update createViewModel**

Add import:
```kotlin
import com.woocommerce.android.ui.bookings.BookingsRepository
```

Add field after existing mocks:
```kotlin
private val bookingsRepository: BookingsRepository = mock()
```

Update `createViewModel()`:
```kotlin
private fun createViewModel(): WooPosBookingsViewModel {
    return WooPosBookingsViewModel(
        bookingListHandler = bookingListHandler,
        dateTimeProvider = dateTimeProvider,
        mapper = WooPosBookingViewStateMapper(dateFormatter, resourceProvider),
        bookingsRepository = bookingsRepository,
    )
}
```

**Step 2: Add test — optimistic update on attended toggle**

```kotlin
@Test
fun `given content state, when attendance toggled to attended, then selection updates optimistically`() = runTest {
    // GIVEN
    whenever(bookingsRepository.updateAttendanceStatus(any(), any()))
        .doSuspendableAnswer { delay(Long.MAX_VALUE); Result.success(Unit) }
    viewModel = createViewModel()
    advanceUntilIdle()

    // WHEN
    viewModel.onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(true))
    advanceUntilIdle()

    // THEN
    val content = viewModel.state.value as WooPosBookingsState.Content
    assertThat(content.selectedDetails?.attendanceSection?.selection)
        .isEqualTo(WooPosBookingsState.AttendanceState.ATTENDED)
    assertThat(content.selectedDetails?.attendanceBadge)
        .isEqualTo(WooPosBookingsState.AttendanceState.ATTENDED)
}
```

**Step 3: Add test — optimistic update on unattended toggle**

```kotlin
@Test
fun `given content state, when attendance toggled to unattended, then selection updates optimistically`() = runTest {
    // GIVEN
    whenever(bookingsRepository.updateAttendanceStatus(any(), any()))
        .doSuspendableAnswer { delay(Long.MAX_VALUE); Result.success(Unit) }
    viewModel = createViewModel()
    advanceUntilIdle()

    // WHEN
    viewModel.onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(false))
    advanceUntilIdle()

    // THEN
    val content = viewModel.state.value as WooPosBookingsState.Content
    assertThat(content.selectedDetails?.attendanceSection?.selection)
        .isEqualTo(WooPosBookingsState.AttendanceState.UNATTENDED)
    assertThat(content.selectedDetails?.attendanceBadge)
        .isEqualTo(WooPosBookingsState.AttendanceState.UNATTENDED)
}
```

**Step 4: Add test — rollback on API failure**

```kotlin
@Test
fun `given content state, when attendance toggle API fails, then selection reverts to previous value`() = runTest {
    // GIVEN
    whenever(bookingsRepository.updateAttendanceStatus(any(), any()))
        .thenReturn(Result.failure(RuntimeException("network error")))
    viewModel = createViewModel()
    advanceUntilIdle()

    val contentBefore = viewModel.state.value as WooPosBookingsState.Content
    val previousSelection = contentBefore.selectedDetails?.attendanceSection?.selection

    // WHEN
    viewModel.onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(false))
    advanceUntilIdle()

    // THEN
    val content = viewModel.state.value as WooPosBookingsState.Content
    assertThat(content.selectedDetails?.attendanceSection?.selection).isEqualTo(previousSelection)
}
```

**Step 5: Add test — correct repository call**

```kotlin
@Test
fun `given content state, when attendance toggled to attended, then repository called with correct params`() = runTest {
    // GIVEN
    whenever(bookingsRepository.updateAttendanceStatus(any(), any()))
        .thenReturn(Result.success(Unit))
    viewModel = createViewModel()
    advanceUntilIdle()

    // WHEN
    viewModel.onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(true))
    advanceUntilIdle()

    // THEN
    verify(bookingsRepository).updateAttendanceStatus(
        bookingId = 1L,
        attendanceStatus = BookingEntity.AttendanceStatus.Attended
    )
}
```

**Step 6: Run tests to verify they fail**

```bash
cd /Users/andrey/StudioProjects/woocommerce-android && ./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosBookingsViewModelTest"
```

Expected: compilation error (ViewModel doesn't accept bookingsRepository yet)

**Step 7: Add to git**

```bash
git add WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModelTest.kt
```

---

### Task 4: Implement attendance toggle in ViewModel

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModel.kt`

**Step 1: Add BookingsRepository import and constructor parameter**

Add import:
```kotlin
import com.woocommerce.android.ui.bookings.BookingsRepository
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
```

Add constructor parameter:
```kotlin
private val bookingsRepository: BookingsRepository,
```

**Step 2: Replace the empty AttendanceToggled handler (line 218)**

Replace:
```kotlin
is WooPosBookingsUIEvent.AttendanceToggled -> { }
```

With:
```kotlin
is WooPosBookingsUIEvent.AttendanceToggled -> handleAttendanceToggle(event.attended)
```

**Step 3: Add the handleAttendanceToggle method**

```kotlin
private fun handleAttendanceToggle(attended: Boolean) {
    val currentState = _state.value as? WooPosBookingsState.Content ?: return
    val details = currentState.selectedDetails ?: return
    val attendanceSection = details.attendanceSection ?: return

    val newAttendanceState = if (attended) {
        WooPosBookingsState.AttendanceState.ATTENDED
    } else {
        WooPosBookingsState.AttendanceState.UNATTENDED
    }

    val previousSelection = attendanceSection.selection
    val previousBadge = details.attendanceBadge

    val updatedDetails = details.copy(
        attendanceSection = attendanceSection.copy(selection = newAttendanceState),
        attendanceBadge = newAttendanceState,
    )
    _state.value = currentState.copy(
        selectedDetails = updatedDetails,
        items = updateItemsWithDetails(currentState.items, updatedDetails),
    )

    viewModelScope.launch {
        val entityStatus = if (attended) {
            BookingEntity.AttendanceStatus.Attended
        } else {
            BookingEntity.AttendanceStatus.Unattended
        }
        bookingsRepository.updateAttendanceStatus(
            bookingId = details.id,
            attendanceStatus = entityStatus,
        ).onFailure {
            val rollbackState = _state.value as? WooPosBookingsState.Content ?: return@onFailure
            val rollbackDetails = rollbackState.selectedDetails ?: return@onFailure
            if (rollbackDetails.id != details.id) return@onFailure
            val reverted = rollbackDetails.copy(
                attendanceSection = rollbackDetails.attendanceSection?.copy(selection = previousSelection),
                attendanceBadge = previousBadge,
            )
            _state.value = rollbackState.copy(
                selectedDetails = reverted,
                items = updateItemsWithDetails(rollbackState.items, reverted),
            )
        }
    }
}

private fun updateItemsWithDetails(
    items: WooPosBookingsState.Content.Items,
    updatedDetails: WooPosBookingsState.BookingDetailsViewState
): WooPosBookingsState.Content.Items {
    val loaded = items as? WooPosBookingsState.Content.Items.Loaded ?: return items
    val updatedMap = loaded.items.map { (item, details) ->
        if (item.id == updatedDetails.id) {
            item to updatedDetails
        } else {
            item to details
        }
    }.toMap()
    return WooPosBookingsState.Content.Items.Loaded(updatedMap)
}
```

**Step 4: Run tests to verify they pass**

```bash
cd /Users/andrey/StudioProjects/woocommerce-android && ./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosBookingsViewModelTest"
```

Expected: all tests pass

**Step 5: Run detekt**

```bash
cd /Users/andrey/StudioProjects/woocommerce-android && ./gradlew detektAll --auto-correct
```

**Step 6: Add to git**

```bash
git add WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModel.kt
```
