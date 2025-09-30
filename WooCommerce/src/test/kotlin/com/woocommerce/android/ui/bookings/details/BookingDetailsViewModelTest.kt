package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BookingDetailsViewModelTest : BaseUnitTest() {

    @Test
    fun `given bookingId in SavedStateHandle, when ViewModel created, then toolbar title formatted`() {
        // Given
        val bookingId = 123L
        val savedState = SavedStateHandle(mapOf("bookingId" to bookingId))
        val resourceProvider = mock<ResourceProvider> {
            on { getString(R.string.booking_details_title, bookingId) } doReturn "Booking #$bookingId"
        }

        // When
        val viewModel = createViewModel(savedState, resourceProvider)

        // Then
        val state = viewModel.state.value
        assertThat(state?.toolbarTitle).isEqualTo("Booking #$bookingId")
    }

    @Test
    fun `when onAttendanceStatusSelected called, then state updates with new status`() {
        // Given
        val bookingId = 456L
        val savedState = SavedStateHandle(mapOf("bookingId" to bookingId))
        val resourceProvider = mock<ResourceProvider> {
            on { getString(R.string.booking_details_title, bookingId) } doReturn "Booking #$bookingId"
        }
        val viewModel = createViewModel(savedState, resourceProvider)

        // When
        viewModel.onAttendanceStatusSelected(com.woocommerce.android.ui.bookings.compose.AttendanceStatus.CANCELLED)

        // Then
        val updated = viewModel.state.value?.bookingSummary?.attendanceStatus
        assertThat(updated).isEqualTo(com.woocommerce.android.ui.bookings.compose.AttendanceStatus.CANCELLED)
    }

    private fun createViewModel(
        savedState: SavedStateHandle,
        resourceProvider: ResourceProvider
    ): BookingDetailsViewModel {
        return BookingDetailsViewModel(savedState, resourceProvider).apply {
            state.observeForever { }
        }
    }
}
