package com.woocommerce.android.ui.bookings.filter

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.bookings.filter.data.BookingFilterRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.BookingType
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.AttendanceStatus

@OptIn(ExperimentalCoroutinesApi::class)
class BookingFilterListViewModelTest : BaseUnitTest() {

    private lateinit var viewModel: BookingFilterListViewModel
    private val bookingFilterRepository: BookingFilterRepository = mock {
        on { bookingFiltersFlow } doReturn flowOf(BookingFilters())
    }

    @Before
    fun setup() {
        viewModel = BookingFilterListViewModel(
            savedStateHandle = SavedStateHandle(),
            bookingFilterRepository = bookingFilterRepository,
        )
    }

    @Test
    fun `when init, then current page is List`() {
        // WHEN
        val state = viewModel.uiState.value!!

        // THEN
        assertThat(state.currentPage).isInstanceOf(BookingFilterPage.List::class.java)
    }

    @Test
    fun `given DateTimePicker page, when openPage, then current page is DateTimePicker`() {
        // GIVEN
        val initial = viewModel.uiState.getOrAwaitValue()
        assertThat(initial.currentPage).isInstanceOf(BookingFilterPage.List::class.java)

        // WHEN
        initial.openPage(BookingFilterPage.DateTime)

        // THEN
        val updated = viewModel.uiState.getOrAwaitValue()
        assertThat(updated.currentPage).isInstanceOf(BookingFilterPage.DateTime::class.java)
    }

    @Test
    fun `given current page is DateTimePicker, when onClose, then current page set to List`() {
        // GIVEN
        val initial = viewModel.uiState.getOrAwaitValue()
        initial.openPage(BookingFilterPage.DateTime)
        val afterOpen = viewModel.uiState.getOrAwaitValue()
        assertThat(afterOpen.currentPage).isInstanceOf(BookingFilterPage.DateTime::class.java)

        // WHEN
        afterOpen.onClose()

        // THEN
        val afterClose = viewModel.uiState.getOrAwaitValue()
        assertThat(afterClose.currentPage).isInstanceOf(BookingFilterPage.List::class.java)
    }

    @Test
    fun `given current page is List, and no changes, when onClose, then Exit event is emitted`() {
        // GIVEN
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { events.add(it) }
        val initial = viewModel.uiState.value!!
        assertThat(initial.currentPage).isInstanceOf(BookingFilterPage.List::class.java)

        // WHEN
        initial.onClose()

        // THEN
        assertThat(events).isNotEmpty
        assertThat(events.last()).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `given unsaved changes on Filters, when onClose, then dialog becomes visible`() {
        // GIVEN
        val initial = viewModel.uiState.getOrAwaitValue()
        // introduce a change different from initial filters (which are empty)
        initial.onUpdateFilterOption(BookingType(BookingType.Type.SERVICE))
        val changed = viewModel.uiState.getOrAwaitValue()

        // WHEN
        changed.onClose()

        // THEN
        val afterClose = viewModel.uiState.getOrAwaitValue()
        assertThat(afterClose.dialogState).isNotNull()
    }

    @Test
    fun `given dialog visible, when Keep Changes tapped, then dialog hides`() {
        // GIVEN
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { events.add(it) }
        val initial = viewModel.uiState.getOrAwaitValue()
        initial.onUpdateFilterOption(BookingType(BookingType.Type.SERVICE))
        viewModel.uiState.getOrAwaitValue().onClose() // shows dialog

        // WHEN: tap Keep Changes
        val withDialog = viewModel.uiState.getOrAwaitValue()
        withDialog.dialogState?.negativeButton?.onClick()

        // THEN: dialog hidden
        val afterDismiss = viewModel.uiState.getOrAwaitValue()
        assertThat(afterDismiss.dialogState).isNull()
    }

    @Test
    fun `given dialog visible, when Discard pressed, then dialog hides, and exit`() {
        // GIVEN
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { events.add(it) }
        val initial = viewModel.uiState.getOrAwaitValue()
        initial.onUpdateFilterOption(BookingType(BookingType.Type.SERVICE))
        viewModel.uiState.getOrAwaitValue().onClose() // shows dialog

        // WHEN: tap Discard
        val withDialog = viewModel.uiState.getOrAwaitValue()
        withDialog.dialogState?.positiveButton?.onClick()

        // THEN: dialog hidden and Exit event emitted
        val afterDiscard = viewModel.uiState.getOrAwaitValue()
        assertThat(afterDiscard.dialogState).isNull()
        assertThat(events).isNotEmpty
        assertThat(events.last()).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `when onClear is invoked, then updated filters reset to default`() {
        // GIVEN: a changed filter state
        val initial = viewModel.uiState.getOrAwaitValue()
        initial.onUpdateFilterOption(BookingType(BookingType.Type.SERVICE))
        val changed = viewModel.uiState.getOrAwaitValue()
        assertThat(changed.updatedBookingFilters.bookingType).isNotEqualTo(null)

        // WHEN: clear is invoked
        changed.onClear()

        // THEN: updated filters become the default
        val afterClear = viewModel.uiState.getOrAwaitValue()
        assertThat(afterClear.updatedBookingFilters.bookingType).isEqualTo(null)
    }

    @Test
    fun `when attendance status changed and leaving page, then root shows selected value`() {
        // GIVEN: navigate to Attendance Status page and select one status
        val initial = viewModel.uiState.getOrAwaitValue()
        initial.openPage(BookingFilterPage.AttendanceStatus)
        val onPage = viewModel.uiState.getOrAwaitValue()

        // Select one attendance status (Booked)
        onPage.onUpdateFilterOption(BookingsFilterOption.AttendanceStatuses(values = setOf(AttendanceStatus.Booked)))

        // WHEN: leave the page (go back to root list)
        viewModel.uiState.getOrAwaitValue().onClose()

        // THEN: root list shows the selected status as subtitle values
        val root = viewModel.uiState.getOrAwaitValue()

        val attendanceItem = root.items.first { it.title == R.string.bookings_filter_title_attendance_status }
        val values = attendanceItem.values?.map { (it as UiStringRes).stringRes }

        assertThat(values).containsExactly(R.string.booking_attendance_status_booked)
    }

    @Test
    fun `when two attendance statuses selected and leaving page, then root shows selected both statuses`() {
        // GIVEN: navigate to Attendance Status page and select two statuses
        val initial = viewModel.uiState.getOrAwaitValue()
        initial.openPage(BookingFilterPage.AttendanceStatus)
        val onPage = viewModel.uiState.getOrAwaitValue()

        // Select two attendance statuses (Booked and Cancelled)
        onPage.onUpdateFilterOption(
            BookingsFilterOption.AttendanceStatuses(values = setOf(AttendanceStatus.Booked, AttendanceStatus.Cancelled))
        )

        // WHEN: leave the page (go back to root list)
        viewModel.uiState.getOrAwaitValue().onClose()

        // THEN: root list shows both selected statuses in subtitle values (order agnostic)
        val root = viewModel.uiState.getOrAwaitValue()

        val attendanceItem = root.items.first { it.title == R.string.bookings_filter_title_attendance_status }
        val values = attendanceItem.values?.map { (it as UiStringRes).stringRes }

        assertThat(values).containsExactlyInAnyOrder(
            R.string.booking_attendance_status_booked,
            R.string.booking_attendance_status_cancelled
        )
    }
}
