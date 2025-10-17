package com.woocommerce.android.ui.bookings.filter

import androidx.lifecycle.SavedStateHandle
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
    fun `given current page is List, when onClose, then Exit event is emitted`() {
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
}
