# POS Bookings Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a bookings screen to the WooPos that lets merchants view, manage, and collect payment for bookings.

**Architecture:** Split-pane screen (like orders) with tabs, data sourced directly from BookingsRestClient (no DB), card/cash payment collection, and navigation via the floating toolbar menu. Zero modifications to existing checkout code.

**Tech Stack:** Jetpack Compose (Material 3), Hilt DI, Kotlin Coroutines/Flow, Navigation Compose, BookingsRestClient (fluxc), CardReaderPaymentControllerFactory

**Design doc:** `docs/plans/2026-02-05-pos-bookings-design.md`

---

### Task 1: Navigation Wiring - Events and Routes

Add the bookings navigation events, child-to-parent events, route, and menu entry. This wires up the full navigation path so that tapping "Bookings" in the menu opens an empty placeholder screen.

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/WooPosHomeChildToParentCommunication.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/root/navigation/WooPosNavigationEvent.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/root/navigation/WooPosNavigationEventHandler.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/root/navigation/WooPosRootHost.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/root/navigation/WooPosMainFlowGraph.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/toolbar/WooPosHomeFloatingToolbarViewModel.kt`
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsNavigation.kt`
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsScreen.kt` (placeholder)

**Step 1: Add child-to-parent events**

In `WooPosHomeChildToParentCommunication.kt`, add to `NavigationEvent`:

```kotlin
data object ToBookings : NavigationEvent()
data class ToOrderWithSearch(val orderNumber: String) : NavigationEvent()
```

**Step 2: Add navigation events**

In `WooPosNavigationEvent.kt`, add:

```kotlin
data object OpenBookings : WooPosNavigationEvent()
data class OpenOrdersWithSearch(val orderNumber: String) : WooPosNavigationEvent()
data class OpenBookingCardPayment(val orderId: Long) : WooPosNavigationEvent()
```

**Step 3: Create bookings navigation file**

Create `WooPosBookingsNavigation.kt`:

```kotlin
package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val BOOKINGS_ROUTE = "$HOME_ROUTE/bookings"

fun NavController.navigateToBookingsScreen() {
    navigateOnce(BOOKINGS_ROUTE)
}

fun NavGraphBuilder.bookingsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = BOOKINGS_ROUTE,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
    ) {
        WooPosBookingsScreen(onNavigationEvent = onNavigationEvent)
    }
}
```

**Step 4: Create placeholder bookings screen**

Create `WooPosBookingsScreen.kt` with a simple placeholder:

```kotlin
package com.woocommerce.android.ui.woopos.bookings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosBookingsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    BackHandler { onNavigationEvent(WooPosNavigationEvent.GoBack) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Bookings - Coming Soon")
    }
}
```

**Step 5: Wire navigation event handler**

In `WooPosNavigationEventHandler.kt`, add imports and case:

```kotlin
import com.woocommerce.android.ui.woopos.bookings.navigateToBookingsScreen

// In when(event):
is WooPosNavigationEvent.OpenBookings -> navigateToBookingsScreen()

is WooPosNavigationEvent.OpenOrdersWithSearch -> {
    navigateToOrdersScreen()
    // Pass search query via savedStateHandle
    currentBackStackEntry?.savedStateHandle?.set("searchQuery", event.orderNumber)
}
```

**Step 6: Wire RootHost**

In `WooPosRootHost.kt`, add to the `when` block in `homeViewModel.navigationEvent.collect`:

```kotlin
NavigationEvent.ToBookings -> onNavigationEvent(WooPosNavigationEvent.OpenBookings)
is NavigationEvent.ToOrderWithSearch -> onNavigationEvent(
    WooPosNavigationEvent.OpenOrdersWithSearch(it.orderNumber)
)
```

Add imports for `OpenBookings`, `OpenOrdersWithSearch`.

**Step 7: Wire main flow graph**

In `WooPosMainFlowGraph.kt`, add:

```kotlin
import com.woocommerce.android.ui.woopos.bookings.bookingsScreen

// Inside navigation block:
bookingsScreen(onNavigationEvent = onNavigationEvent)
```

**Step 8: Add menu item**

In `WooPosHomeFloatingToolbarViewModel.kt`:

Add bookings menu item as first item in `toolbarMenuItems`:

```kotlin
WooPosHomeFloatingToolbarState.Menu.MenuItem(
    title = R.string.woopos_bookings_title,
    icon = R.drawable.ic_calendar_today_24dp,
),
```

Add handling in `handleMenuItemClicked`:

```kotlin
R.string.woopos_bookings_title -> {
    viewModelScope.launch {
        childrenToParentEventSender.sendToParent(ChildToParentEvent.NavigationEvent.ToBookings)
    }
}
```

Note: You'll need to add string resource `woopos_bookings_title` = "Bookings" and find/create an appropriate calendar icon drawable. Check existing drawables first with: `find . -name "ic_calendar*"`.

**Step 9: Build and verify**

Run: `./gradlew :WooCommerce:assembleVanillaDebug`

Verify: App launches POS, menu shows "Bookings" entry, tapping it shows the placeholder screen, back button returns to home.

**Step 10: Commit**

```bash
git add -A && git commit -m "Add bookings navigation wiring and placeholder screen"
```

---

### Task 2: Data Source - WooPosBookingsDataSource

Create the data source that calls BookingsRestClient directly with tab-based filtering. No caching.

**Files:**
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsDataSource.kt`

**Step 1: Create data source**

```kotlin
package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingUpdatePayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsOrderOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsRestClient
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class WooPosBookingsDataSource @Inject constructor(
    private val restClient: BookingsRestClient,
    private val selectedSite: SelectedSite,
) {
    companion object {
        const val PAGE_SIZE = 25
    }

    suspend fun fetchBookings(tab: BookingTab, page: Int): Result<FetchBookingsResult> {
        val (filters, order) = buildFiltersForTab(tab)
        val response = restClient.fetchBookings(
            site = selectedSite.get(),
            perPage = PAGE_SIZE,
            page = page,
            query = null,
            filters = filters,
            order = order,
        )
        return if (response.isError) {
            Result.failure(Throwable(response.error?.message ?: "Unknown error"))
        } else {
            val bookings = response.result?.toList() ?: emptyList()
            // For canceled tab, filter client-side if API doesn't support status filter
            val filtered = if (tab == BookingTab.Canceled) {
                bookings.filter { it.status == BookingEntity.Status.Cancelled.key }
            } else {
                bookings
            }
            Result.success(
                FetchBookingsResult(
                    bookings = filtered,
                    hasMorePages = bookings.size >= PAGE_SIZE,
                )
            )
        }
    }

    suspend fun fetchBooking(bookingId: Long): Result<BookingDto> {
        val response = restClient.fetchBooking(
            site = selectedSite.get(),
            bookingId = bookingId,
        )
        return if (response.isError) {
            Result.failure(Throwable(response.error?.message ?: "Unknown error"))
        } else {
            response.result?.let { Result.success(it) }
                ?: Result.failure(Throwable("Booking not found"))
        }
    }

    suspend fun cancelBooking(bookingId: Long): Result<BookingDto> {
        return updateBooking(
            bookingId,
            BookingUpdatePayload(status = BookingEntity.Status.Cancelled)
        )
    }

    suspend fun updateAttendanceStatus(
        bookingId: Long,
        status: BookingEntity.AttendanceStatus
    ): Result<BookingDto> {
        return updateBooking(
            bookingId,
            BookingUpdatePayload(attendanceStatus = status)
        )
    }

    suspend fun markAsPaid(bookingId: Long): Result<BookingDto> {
        return updateBooking(
            bookingId,
            BookingUpdatePayload(status = BookingEntity.Status.Paid)
        )
    }

    private suspend fun updateBooking(
        bookingId: Long,
        payload: BookingUpdatePayload
    ): Result<BookingDto> {
        val response = restClient.updateBooking(
            site = selectedSite.get(),
            bookingId = bookingId,
            payload = payload,
        )
        return if (response.isError) {
            Result.failure(Throwable(response.error?.message ?: "Unknown error"))
        } else {
            response.result?.let { Result.success(it) }
                ?: Result.failure(Throwable("Update failed"))
        }
    }

    private fun buildFiltersForTab(tab: BookingTab): Pair<BookingFilters?, BookingsOrderOption> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfDay = today.atStartOfDay(zone).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()

        return when (tab) {
            BookingTab.Today -> {
                val filters = BookingFilters(
                    dateRange = BookingsFilterOption.DateRange(
                        after = startOfDay,
                        before = endOfDay,
                    )
                )
                filters to BookingsOrderOption.ASC
            }
            BookingTab.Upcoming -> {
                val filters = BookingFilters(
                    dateRange = BookingsFilterOption.DateRange(
                        after = endOfDay,
                    )
                )
                filters to BookingsOrderOption.ASC
            }
            BookingTab.Canceled -> {
                // Fetch all, filter client-side by cancelled status
                null to BookingsOrderOption.DESC
            }
            BookingTab.All -> {
                null to BookingsOrderOption.DESC
            }
        }
    }
}

data class FetchBookingsResult(
    val bookings: List<BookingDto>,
    val hasMorePages: Boolean,
)

enum class BookingTab {
    Today, Upcoming, Canceled, All
}
```

**Step 2: Build and verify**

Run: `./gradlew :WooCommerce:assembleVanillaDebug`

**Step 3: Commit**

```bash
git add -A && git commit -m "Add WooPosBookingsDataSource with tab-based filtering"
```

---

### Task 3: UI State and Mappers

Define the UI state classes and mappers that convert BookingDto to UI models.

**Files:**
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsState.kt`
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingMapper.kt`

**Step 1: Create state classes**

Create `WooPosBookingsState.kt`:

```kotlin
package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.runtime.Immutable
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

@Immutable
sealed class WooPosBookingsState {
    data object Loading : WooPosBookingsState()

    @Immutable
    data class Error(val message: String) : WooPosBookingsState()

    data object Empty : WooPosBookingsState()

    @Immutable
    data class Content(
        val selectedTab: BookingTab,
        val items: List<BookingListItem>,
        val selectedDetail: BookingDetail?,
        val paginationState: WooPosPaginationState,
        val pullToRefreshState: WooPosPullToRefreshState,
        val dialogState: DialogState,
    ) : WooPosBookingsState()
}

@Immutable
data class BookingListItem(
    val id: Long,
    val orderId: Long,
    val customerName: String,
    val serviceName: String,
    val startTime: String,
    val amount: String,
    val bookingStatus: BookingStatusUi,
    val attendanceStatus: AttendanceStatusUi?,
    val isSelected: Boolean,
)

@Immutable
data class BookingDetail(
    val id: Long,
    val orderId: Long,
    val customerName: String,
    val serviceName: String,
    val startDate: String,
    val startTime: String,
    val endTime: String,
    val amount: String,
    val currency: String,
    val bookingStatus: BookingStatusUi,
    val attendanceStatus: AttendanceStatusUi?,
    val isCancellable: Boolean,
    val isAttendanceEditable: Boolean,
    val hasLinkedOrder: Boolean,
    val isPayable: Boolean,
    val attendanceUpdateInProgress: Boolean,
    val cancelInProgress: Boolean,
    val paymentUpdateInProgress: Boolean,
)

enum class BookingStatusUi(val label: String) {
    Unpaid("Unpaid"),
    PendingConfirmation("Pending"),
    Confirmed("Confirmed"),
    Paid("Paid"),
    Cancelled("Cancelled"),
    Complete("Complete"),
    InCart("In Cart"),
    Unknown("Unknown"),
}

enum class AttendanceStatusUi(val label: String) {
    Booked("Booked"),
    CheckedIn("Checked In"),
    NoShow("No Show"),
    Cancelled("Cancelled"),
}

sealed class DialogState {
    data object Hidden : DialogState()
    data class CancelConfirmation(val bookingId: Long) : DialogState()
}
```

**Step 2: Create mapper**

Create `WooPosBookingMapper.kt`:

```kotlin
package com.woocommerce.android.ui.woopos.bookings

import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingDto
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

class WooPosBookingMapper @Inject constructor() {

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    fun toListItem(
        dto: BookingDto,
        selectedId: Long?,
    ): BookingListItem {
        val zone = ZoneId.systemDefault()
        val startInstant = Instant.ofEpochSecond(dto.start)
        val zonedStart = startInstant.atZone(zone)

        return BookingListItem(
            id = dto.id,
            orderId = dto.orderId,
            customerName = "Customer #${dto.customerId}",
            serviceName = "Product #${dto.productId}",
            startTime = timeFormatter.format(zonedStart),
            amount = formatCost(dto.cost, dto.currency),
            bookingStatus = mapBookingStatus(dto.status),
            attendanceStatus = dto.attendanceStatus?.let { mapAttendanceStatus(it) },
            isSelected = dto.id == selectedId,
        )
    }

    fun toDetail(dto: BookingDto): BookingDetail {
        val zone = ZoneId.systemDefault()
        val startInstant = Instant.ofEpochSecond(dto.start)
        val endInstant = Instant.ofEpochSecond(dto.end)
        val zonedStart = startInstant.atZone(zone)
        val zonedEnd = endInstant.atZone(zone)

        val status = BookingEntity.Status.fromKey(dto.status)
        val isCancellable = status !is BookingEntity.Status.Cancelled &&
            status !is BookingEntity.Status.InCart &&
            status !is BookingEntity.Status.Complete

        val isPayable = (status is BookingEntity.Status.Unpaid ||
            status is BookingEntity.Status.PendingConfirmation ||
            status is BookingEntity.Status.Confirmed) &&
            dto.orderId != 0L

        return BookingDetail(
            id = dto.id,
            orderId = dto.orderId,
            customerName = "Customer #${dto.customerId}",
            serviceName = "Product #${dto.productId}",
            startDate = dateFormatter.format(zonedStart),
            startTime = timeFormatter.format(zonedStart),
            endTime = timeFormatter.format(zonedEnd),
            amount = formatCost(dto.cost, dto.currency),
            currency = dto.currency,
            bookingStatus = mapBookingStatus(dto.status),
            attendanceStatus = dto.attendanceStatus?.let { mapAttendanceStatus(it) },
            isCancellable = isCancellable,
            isAttendanceEditable = status !is BookingEntity.Status.Cancelled,
            hasLinkedOrder = dto.orderId != 0L,
            isPayable = isPayable,
            attendanceUpdateInProgress = false,
            cancelInProgress = false,
            paymentUpdateInProgress = false,
        )
    }

    private fun mapBookingStatus(statusKey: String): BookingStatusUi {
        return when (BookingEntity.Status.fromKey(statusKey)) {
            is BookingEntity.Status.Unpaid -> BookingStatusUi.Unpaid
            is BookingEntity.Status.PendingConfirmation -> BookingStatusUi.PendingConfirmation
            is BookingEntity.Status.Confirmed -> BookingStatusUi.Confirmed
            is BookingEntity.Status.Paid -> BookingStatusUi.Paid
            is BookingEntity.Status.Cancelled -> BookingStatusUi.Cancelled
            is BookingEntity.Status.Complete -> BookingStatusUi.Complete
            is BookingEntity.Status.InCart -> BookingStatusUi.InCart
            is BookingEntity.Status.Unknown -> BookingStatusUi.Unknown
        }
    }

    private fun mapAttendanceStatus(statusKey: String): AttendanceStatusUi? {
        return when (BookingEntity.AttendanceStatus.fromKey(statusKey)) {
            is BookingEntity.AttendanceStatus.Booked -> AttendanceStatusUi.Booked
            is BookingEntity.AttendanceStatus.CheckedIn -> AttendanceStatusUi.CheckedIn
            is BookingEntity.AttendanceStatus.NoShow -> AttendanceStatusUi.NoShow
            is BookingEntity.AttendanceStatus.Cancelled -> AttendanceStatusUi.Cancelled
            is BookingEntity.AttendanceStatus.Unknown -> null
        }
    }

    private fun formatCost(cost: String, currencyCode: String): String {
        return try {
            val amount = BigDecimal(cost)
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.currency = Currency.getInstance(currencyCode.uppercase())
            format.format(amount)
        } catch (e: Exception) {
            "$currencyCode $cost"
        }
    }
}
```

Note: `customerName` and `serviceName` use placeholder text (`Customer #id`, `Product #id`) because BookingDto doesn't include the customer name or product name directly. These would need additional API calls to resolve. For the POC this is acceptable - note this as a finding if needed.

**Step 3: Build and verify**

Run: `./gradlew :WooCommerce:assembleVanillaDebug`

**Step 4: Commit**

```bash
git add -A && git commit -m "Add bookings UI state classes and mapper"
```

---

### Task 4: ViewModel

Create the bookings ViewModel that manages tab state, fetching, selection, pagination, attendance updates, and cancellation.

**Files:**
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModel.kt`

**Step 1: Create ViewModel**

```kotlin
package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingDto
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import javax.inject.Inject

@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val dataSource: WooPosBookingsDataSource,
    private val mapper: WooPosBookingMapper,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosBookingsState>(WooPosBookingsState.Loading)
    val state: StateFlow<WooPosBookingsState> = _state

    private var currentTab = BookingTab.Today
    private var currentPage = 1
    private var bookings = mutableListOf<BookingDto>()
    private var selectedBookingId: Long? = null
    private var loadingJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        loadBookings()
    }

    fun onTabSelected(tab: BookingTab) {
        if (tab == currentTab) return
        currentTab = tab
        selectedBookingId = null
        loadBookings()
    }

    fun onRefresh() {
        val current = _state.value
        if (current is WooPosBookingsState.Content) {
            _state.value = current.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
        }
        loadBookings()
    }

    fun onBookingSelected(bookingId: Long) {
        selectedBookingId = bookingId
        val current = _state.value
        if (current is WooPosBookingsState.Content) {
            _state.value = current.copy(
                items = current.items.map { it.copy(isSelected = it.id == bookingId) },
                selectedDetail = bookings.find { it.id == bookingId }?.let {
                    mapper.toDetail(it)
                },
            )
        }
    }

    fun onEndOfListReached() {
        val current = _state.value
        if (current !is WooPosBookingsState.Content) return
        if (current.paginationState == WooPosPaginationState.Loading) return
        if (current.paginationState == WooPosPaginationState.None &&
            bookings.size < WooPosBookingsDataSource.PAGE_SIZE
        ) return

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _state.value = current.copy(paginationState = WooPosPaginationState.Loading)
            currentPage++
            dataSource.fetchBookings(currentTab, currentPage).fold(
                onSuccess = { result ->
                    bookings.addAll(result.bookings)
                    updateContentState(
                        paginationState = if (result.hasMorePages) {
                            WooPosPaginationState.None
                        } else {
                            WooPosPaginationState.None
                        }
                    )
                },
                onFailure = {
                    currentPage--
                    val currentState = _state.value
                    if (currentState is WooPosBookingsState.Content) {
                        _state.value = currentState.copy(
                            paginationState = WooPosPaginationState.Error
                        )
                    }
                }
            )
        }
    }

    fun onAttendanceStatusSelected(status: AttendanceStatusUi) {
        val bookingId = selectedBookingId ?: return
        val entityStatus = when (status) {
            AttendanceStatusUi.Booked -> BookingEntity.AttendanceStatus.Booked
            AttendanceStatusUi.CheckedIn -> BookingEntity.AttendanceStatus.CheckedIn
            AttendanceStatusUi.NoShow -> BookingEntity.AttendanceStatus.NoShow
            AttendanceStatusUi.Cancelled -> BookingEntity.AttendanceStatus.Cancelled
        }

        updateDetailLoadingState(attendanceUpdateInProgress = true)

        viewModelScope.launch {
            dataSource.updateAttendanceStatus(bookingId, entityStatus).fold(
                onSuccess = { updated ->
                    replaceBookingInList(updated)
                    updateContentState()
                },
                onFailure = {
                    updateDetailLoadingState(attendanceUpdateInProgress = false)
                }
            )
        }
    }

    fun onCancelBookingClicked() {
        val bookingId = selectedBookingId ?: return
        val current = _state.value
        if (current is WooPosBookingsState.Content) {
            _state.value = current.copy(
                dialogState = DialogState.CancelConfirmation(bookingId)
            )
        }
    }

    fun onCancelConfirmed() {
        val current = _state.value
        if (current !is WooPosBookingsState.Content) return
        val dialog = current.dialogState
        if (dialog !is DialogState.CancelConfirmation) return

        _state.value = current.copy(dialogState = DialogState.Hidden)
        updateDetailLoadingState(cancelInProgress = true)

        viewModelScope.launch {
            dataSource.cancelBooking(dialog.bookingId).fold(
                onSuccess = { updated ->
                    replaceBookingInList(updated)
                    updateContentState()
                },
                onFailure = {
                    updateDetailLoadingState(cancelInProgress = false)
                }
            )
        }
    }

    fun onCancelDialogDismissed() {
        val current = _state.value
        if (current is WooPosBookingsState.Content) {
            _state.value = current.copy(dialogState = DialogState.Hidden)
        }
    }

    fun onPayByCardClicked() {
        val detail = (_state.value as? WooPosBookingsState.Content)?.selectedDetail ?: return
        if (detail.orderId == 0L) return
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.NavigationEvent.ToCashPayment(detail.orderId)
                // Note: This will need to be changed to a card payment navigation event
                // once the card payment screen is built in Task 7
            )
        }
    }

    fun onPayByCashClicked() {
        val detail = (_state.value as? WooPosBookingsState.Content)?.selectedDetail ?: return
        if (detail.orderId == 0L) return
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.NavigationEvent.ToCashPayment(detail.orderId)
            )
        }
    }

    fun onViewOrderClicked() {
        val detail = (_state.value as? WooPosBookingsState.Content)?.selectedDetail ?: return
        if (detail.orderId == 0L) return
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.NavigationEvent.ToOrderWithSearch(detail.orderId.toString())
            )
        }
    }

    fun onRetryClicked() {
        loadBookings()
    }

    fun onPaginationRetryClicked() {
        onEndOfListReached()
    }

    /**
     * Called when returning from cash payment screen.
     * Updates the booking status to paid.
     */
    fun onReturnFromCashPayment(bookingId: Long) {
        viewModelScope.launch {
            dataSource.markAsPaid(bookingId).fold(
                onSuccess = { updated ->
                    replaceBookingInList(updated)
                    updateContentState()
                },
                onFailure = { /* Silently fail, booking will refresh on next load */ }
            )
        }
    }

    private fun loadBookings() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            currentPage = 1
            bookings.clear()

            if (_state.value !is WooPosBookingsState.Content) {
                _state.value = WooPosBookingsState.Loading
            }

            dataSource.fetchBookings(currentTab, currentPage).fold(
                onSuccess = { result ->
                    bookings.addAll(result.bookings)
                    if (bookings.isEmpty()) {
                        _state.value = WooPosBookingsState.Empty
                    } else {
                        // Auto-select first booking
                        if (selectedBookingId == null) {
                            selectedBookingId = bookings.first().id
                        }
                        updateContentState()
                    }
                },
                onFailure = {
                    _state.value = WooPosBookingsState.Error(
                        message = it.message ?: "Failed to load bookings"
                    )
                }
            )
        }
    }

    private fun updateContentState(
        paginationState: WooPosPaginationState = WooPosPaginationState.None,
    ) {
        _state.value = WooPosBookingsState.Content(
            selectedTab = currentTab,
            items = bookings.map { mapper.toListItem(it, selectedBookingId) },
            selectedDetail = bookings.find { it.id == selectedBookingId }?.let {
                mapper.toDetail(it)
            },
            paginationState = paginationState,
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            dialogState = DialogState.Hidden,
        )
    }

    private fun updateDetailLoadingState(
        attendanceUpdateInProgress: Boolean? = null,
        cancelInProgress: Boolean? = null,
        paymentUpdateInProgress: Boolean? = null,
    ) {
        val current = _state.value
        if (current !is WooPosBookingsState.Content) return
        val detail = current.selectedDetail ?: return
        _state.value = current.copy(
            selectedDetail = detail.copy(
                attendanceUpdateInProgress = attendanceUpdateInProgress
                    ?: detail.attendanceUpdateInProgress,
                cancelInProgress = cancelInProgress ?: detail.cancelInProgress,
                paymentUpdateInProgress = paymentUpdateInProgress
                    ?: detail.paymentUpdateInProgress,
            )
        )
    }

    private fun replaceBookingInList(updated: BookingDto) {
        val index = bookings.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            bookings[index] = updated
        }
    }
}
```

**Step 2: Build and verify**

Run: `./gradlew :WooCommerce:assembleVanillaDebug`

**Step 3: Commit**

```bash
git add -A && git commit -m "Add WooPosBookingsViewModel with tab, pagination, and action handling"
```

---

### Task 5: Bookings List UI (Left Pane)

Build the left pane with tab chips and booking list. Follow the same patterns as `WooPosOrdersScreen`.

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsScreen.kt` (replace placeholder)

**Step 1: Implement full bookings screen**

Replace the placeholder `WooPosBookingsScreen.kt` with the full implementation including:
- Split-pane layout (30/70) matching orders screen
- Tab row with 4 chips (Today, Upcoming, Canceled, All)
- Booking list items showing: customer name, service, time, amount, status badges
- Pull-to-refresh
- Pagination trigger at 5 items from bottom
- Empty, error, and loading states
- Toolbar with back button

Follow the exact same Compose patterns from `WooPosOrdersScreen.kt`:
- Use `WooPosCard` for list items
- Use `WooPosLazyColumn` for the list
- Use `WooPosToolbar` for the top bar
- Use `WooPosEmptyScreen` and `WooPosErrorScreen` for empty/error states
- Use `MaterialTheme.colorScheme.surfaceBright` for left pane background
- Use `MaterialTheme.colorScheme.surface` for right pane background
- Use `WooPosText` with `WooPosTypography` for text
- Add status badges using colored `Surface` composables (reference `WooPosOrdersStatusBadge` pattern)

For the tab chips, use a `Row` with `FilterChip` (Material 3) or `WooPosCard` styled as chips.

Connect all UI events to the ViewModel via `hiltViewModel()`.

**Step 2: Build and verify**

Run: `./gradlew :WooCommerce:assembleVanillaDebug`

Verify: Bookings screen shows tabs, list loads from API, pull-to-refresh works, pagination works, tab switching fetches new data.

**Step 3: Commit**

```bash
git add -A && git commit -m "Add bookings list UI with tabs, pagination, and pull-to-refresh"
```

---

### Task 6: Booking Detail UI (Right Pane)

Build the right pane showing booking details with inline attendance chips, cancel button, payment buttons, and view order button.

**Files:**
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingDetailPane.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsScreen.kt` (wire detail pane)

**Step 1: Create detail pane composable**

Create `WooPosBookingDetailPane.kt` with:
- Booking summary section (customer name large, service name, date + time range)
- Booking status badge (same style as list)
- Attendance status section with inline selectable chips (Booked, Checked-In, No-Show, Cancelled). Show loading indicator when `attendanceUpdateInProgress` is true. Chips disabled when `!isAttendanceEditable`.
- Divider
- Payment section (only when `isPayable`):
  - "Pay by Card" filled button
  - "Pay by Cash" outlined button
- "View Order" button (only when `hasLinkedOrder`)
- "Cancel Booking" button (only when `isCancellable`, text in error color). Show loading when `cancelInProgress`.

Follow the same visual style as `WooPosOrderDetails` (scrollable column, spacing, typography).

**Step 2: Wire cancel confirmation dialog**

In `WooPosBookingsScreen.kt`, add the cancel confirmation dialog using `WooPosDialogWrapper`:

```kotlin
if (state is WooPosBookingsState.Content) {
    val dialogState = state.dialogState
    if (dialogState is DialogState.CancelConfirmation) {
        WooPosDialogWrapper(
            isVisible = true,
            dialogBackgroundContentDescription = "Cancel booking dialog background",
            onCloseClick = null, // No close button
            onDismissRequest = { viewModel.onCancelDialogDismissed() },
        ) {
            // Dialog content: title, message, Cancel and Confirm buttons
            // Use WooPosText and Button composables
        }
    }
}
```

**Step 3: Build and verify**

Run: `./gradlew :WooCommerce:assembleVanillaDebug`

Verify: Selecting a booking shows detail pane, attendance chips are selectable, cancel shows dialog, view order navigates, payment buttons visible for unpaid bookings.

**Step 4: Commit**

```bash
git add -A && git commit -m "Add booking detail pane with attendance, cancel, payment, and view order"
```

---

### Task 7: Card Payment Screen

Build the self-contained card payment screen for bookings that uses `CardReaderPaymentControllerFactory`.

**Files:**
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/payment/WooPosBookingCardPaymentScreen.kt`
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/payment/WooPosBookingCardPaymentViewModel.kt`
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/payment/WooPosBookingCardPaymentNavigation.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/root/navigation/WooPosMainFlowGraph.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/root/navigation/WooPosNavigationEventHandler.kt`

**Step 1: Create navigation**

Create `WooPosBookingCardPaymentNavigation.kt`:

```kotlin
package com.woocommerce.android.ui.woopos.bookings.payment

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woocommerce.android.ui.woopos.bookings.BOOKINGS_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val BOOKING_CARD_PAYMENT_ORDER_ID_KEY = "orderId"
const val BOOKING_CARD_PAYMENT_ROUTE =
    "$BOOKINGS_ROUTE/card_payment/{$BOOKING_CARD_PAYMENT_ORDER_ID_KEY}"

fun NavController.navigateToBookingCardPayment(orderId: Long) {
    navigateOnce(
        BOOKING_CARD_PAYMENT_ROUTE.replace(
            "{$BOOKING_CARD_PAYMENT_ORDER_ID_KEY}",
            orderId.toString()
        )
    )
}

fun NavGraphBuilder.bookingCardPaymentScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = BOOKING_CARD_PAYMENT_ROUTE,
        arguments = listOf(
            navArgument(BOOKING_CARD_PAYMENT_ORDER_ID_KEY) { type = NavType.LongType }
        ),
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
    ) {
        WooPosBookingCardPaymentScreen(onNavigationEvent = onNavigationEvent)
    }
}
```

**Step 2: Create ViewModel**

Create `WooPosBookingCardPaymentViewModel.kt`. This ViewModel:
- Gets `orderId` from `SavedStateHandle`
- Injects `WooPosCardReaderPaymentControllerFactory` and `WooPosBookingsDataSource`
- Creates a `CardReaderPaymentController` with `PaymentType.WOO_POS` and the orderId
- Collects `paymentState` from the controller
- Maps `CardReaderPaymentState` to a simple UI state (Ready, Processing, Success, Failed)
- On success, calls `dataSource.markAsPaid(bookingId)` to update booking status
- Provides cancel, retry, and done actions

Reference how `WooPosTotalsViewModel` uses the factory (search for `createCardReaderPaymentController` and `listenToPaymentState` in that file).

**Step 3: Create Screen**

Create `WooPosBookingCardPaymentScreen.kt`. This screen renders based on ViewModel state:
- Ready: Show "Tap, insert, or swipe card" message + amount + Cancel button
- Processing: Reuse `WooPosTotalsPaymentInProgressScreen` composable pattern (Lottie animation + text)
- Success: Reuse `WooPosPaymentSuccessScreen` composable pattern (checkmark + amount + Done button)
- Failed: Reuse `WooPosTotalsPaymentFailedScreen` composable pattern (error + Try Again + Cancel)

Note: You may not be able to directly call these composables if they have internal visibility or tight coupling to `WooPosTotalsViewState`. In that case, copy the visual patterns (they're simple Compose layouts) into the bookings payment screen.

**Step 4: Wire navigation**

In `WooPosMainFlowGraph.kt`, add:
```kotlin
bookingCardPaymentScreen(onNavigationEvent = onNavigationEvent)
```

In `WooPosNavigationEventHandler.kt`, add:
```kotlin
is WooPosNavigationEvent.OpenBookingCardPayment ->
    navigateToBookingCardPayment(event.orderId)
```

**Step 5: Update bookings ViewModel**

In `WooPosBookingsViewModel.onPayByCardClicked()`, change the navigation to use the new card payment event:
```kotlin
childrenToParentEventSender.sendToParent(
    ChildToParentEvent.NavigationEvent.ToBookingCardPayment(detail.orderId)
)
```

This requires adding `ToBookingCardPayment(val orderId: Long)` to `ChildToParentEvent.NavigationEvent`, and handling it in `WooPosRootHost.kt`.

**Step 6: Build and verify**

Run: `./gradlew :WooCommerce:assembleVanillaDebug`

Verify: "Pay by Card" navigates to card payment screen, card reader states display correctly, success updates booking status.

**Step 7: Commit**

```bash
git add -A && git commit -m "Add self-contained card payment screen for bookings"
```

---

### Task 8: Cash Payment Integration and View Order

Wire the cash payment return flow (mark booking as paid after cash payment completes) and the "View Order" navigation with search pre-fill.

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsNavigation.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModel.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosOrdersNavigation.kt` (accept search param)
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosOrdersViewModel.kt` (read search param)

**Step 1: Cash payment return**

In `WooPosBookingsNavigation.kt`, listen for a result from cash payment via `SavedStateHandle`. When the bookings screen detects return from cash payment, trigger `viewModel.onReturnFromCashPayment()`.

This follows the same pattern as how `WooPosOrdersScreen` listens for `EMAIL_RECEIPT_SENT` or `REFUND_REASON_RESULT_KEY` from the backstack.

**Step 2: View Order search pre-fill**

In `WooPosOrdersNavigation.kt`, accept an optional `searchQuery` parameter. When present, pass it to the orders screen. The orders ViewModel should check for this parameter on init and pre-fill the search.

For the POC, the simplest approach: when navigating from bookings to orders with a search query, the orders screen opens with the search input pre-populated and automatically searches (bypassing the `createdVia` filter during search, which the existing search already does since `searchQuery` is a separate API param).

**Step 3: Build and verify**

Run: `./gradlew :WooCommerce:assembleVanillaDebug`

Verify: After cash payment, booking status updates. "View Order" opens orders screen with the order found in search.

**Step 4: Commit**

```bash
git add -A && git commit -m "Wire cash payment return flow and view order navigation"
```

---

### Task 9: Polish and Edge Cases

Handle remaining edge cases, add string resources, and clean up.

**Files:**
- Create/Modify: `WooCommerce/src/main/res/values/strings.xml` (add bookings strings)
- Modify: various files for edge cases

**Step 1: Add string resources**

Add strings for:
- `woopos_bookings_title` = "Bookings"
- `woopos_bookings_tab_today` = "Today"
- `woopos_bookings_tab_upcoming` = "Upcoming"
- `woopos_bookings_tab_canceled` = "Canceled"
- `woopos_bookings_tab_all` = "All"
- `woopos_bookings_empty_title` = "No bookings"
- `woopos_bookings_empty_message` = "Bookings will appear here"
- `woopos_bookings_error_title` = "Could not load bookings"
- `woopos_bookings_error_retry` = "Try again"
- `woopos_bookings_cancel_dialog_title` = "Cancel booking?"
- `woopos_bookings_cancel_dialog_message` = "This booking will be marked as cancelled."
- `woopos_bookings_cancel_dialog_confirm` = "Cancel booking"
- `woopos_bookings_cancel_dialog_dismiss` = "Go back"
- `woopos_bookings_pay_by_card` = "Pay by Card"
- `woopos_bookings_pay_by_cash` = "Pay by Cash"
- `woopos_bookings_view_order` = "View Order"
- `woopos_bookings_no_booking_selected` = "Select a booking to view details"

**Step 2: Handle edge cases**

- Empty detail placeholder when no booking is selected (use `WooPosEmptyScreen`)
- Loading state for detail pane while booking is being fetched after selection
- Handle the case where `orderId == 0` (no linked order) - hide payment and view order buttons
- Handle tab empty states with appropriate messages per tab (e.g., "No bookings today", "No upcoming bookings")

**Step 3: Build and verify**

Run: `./gradlew :WooCommerce:assembleVanillaDebug`

Full end-to-end test: Open POS -> Menu -> Bookings -> Browse tabs -> Select booking -> View details -> Update attendance -> Cancel booking -> Pay by cash -> Pay by card -> View order.

**Step 4: Commit**

```bash
git add -A && git commit -m "Add bookings string resources and handle edge cases"
```

---

## Task Dependencies

```
Task 1 (Navigation) ──> Task 2 (DataSource) ──> Task 3 (State/Mappers)
                                                        │
                                                        ▼
                                                  Task 4 (ViewModel)
                                                        │
                                                        ▼
                                              Task 5 (List UI) ──> Task 6 (Detail UI)
                                                                        │
                                                                        ▼
                                                              Task 7 (Card Payment)
                                                                        │
                                                                        ▼
                                                              Task 8 (Cash + View Order)
                                                                        │
                                                                        ▼
                                                              Task 9 (Polish)
```

All tasks are sequential. Each builds on the previous one.
