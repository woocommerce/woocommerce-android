package com.woocommerce.android.ui.bookings.filter.attendancestatus

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.AttendanceStatus

@OptIn(ExperimentalCoroutinesApi::class)
class BookingAttendanceStatusFilterViewModelTest : BaseUnitTest() {

    private var lastFilterResult: BookingsFilterOption.AttendanceStatus? = null

    private fun createViewModel(
        initialStatus: BookingsFilterOption.AttendanceStatus? = null
    ): BookingAttendanceStatusFilterViewModel {
        lastFilterResult = null
        return BookingAttendanceStatusFilterViewModel(
            initialStatus = initialStatus,
            onFilterChanged = { lastFilterResult = it },
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun `when Attended is selected, then only Attended is in the filter`() = testBlocking {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.uiState.getOrAwaitValue().items[ATTENDED_INDEX].onClick()

        // THEN
        val state = viewModel.uiState.getOrAwaitValue()
        assertThat(state.selectedStatus).isEqualTo(AttendanceStatus.Attended)
        assertThat(lastFilterResult).isEqualTo(BookingsFilterOption.AttendanceStatus(AttendanceStatus.Attended))
    }

    @Test
    fun `given Attended selected, when Unattended is selected, then only Unattended is in the filter`() =
        testBlocking {
            // GIVEN
            val viewModel = createViewModel(BookingsFilterOption.AttendanceStatus(AttendanceStatus.Attended))

            // WHEN
            viewModel.uiState.getOrAwaitValue().items[UNATTENDED_INDEX].onClick()

            // THEN
            val state = viewModel.uiState.getOrAwaitValue()
            assertThat(state.selectedStatus).isEqualTo(AttendanceStatus.Unattended)
            assertThat(lastFilterResult).isEqualTo(BookingsFilterOption.AttendanceStatus(AttendanceStatus.Unattended))
        }

    @Test
    fun `given Attended selected, when Any is selected, then filter is cleared`() = testBlocking {
        // GIVEN
        val viewModel = createViewModel(BookingsFilterOption.AttendanceStatus(AttendanceStatus.Attended))

        // WHEN
        viewModel.uiState.getOrAwaitValue().items[ANY_INDEX].onClick()

        // THEN
        val state = viewModel.uiState.getOrAwaitValue()
        assertThat(state.selectedStatus).isNull()
        assertThat(lastFilterResult).isEqualTo(BookingsFilterOption.AttendanceStatus(null))
    }

    @Test
    fun `given Attended selected, when Attended is selected again, then Attended remains selected`() = testBlocking {
        // GIVEN
        val viewModel = createViewModel(BookingsFilterOption.AttendanceStatus(AttendanceStatus.Attended))

        // WHEN
        viewModel.uiState.getOrAwaitValue().items[ATTENDED_INDEX].onClick()

        // THEN
        val state = viewModel.uiState.getOrAwaitValue()
        assertThat(state.selectedStatus).isEqualTo(AttendanceStatus.Attended)
        assertThat(lastFilterResult).isEqualTo(BookingsFilterOption.AttendanceStatus(AttendanceStatus.Attended))
    }

    companion object {
        private const val ANY_INDEX = 0
        private const val ATTENDED_INDEX = 1
        private const val UNATTENDED_INDEX = 2
    }
}
