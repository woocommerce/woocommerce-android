package com.woocommerce.android.ui.bookings.filter.datetime

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.compose.component.Time
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DateTimeFilterViewModelTest : BaseUnitTest() {

    private val clock = Clock.fixed(
        LocalDateTime.of(2025, 11, 13, 10, 0)
            .toInstant(ZoneOffset.UTC),
        ZoneOffset.UTC
    )

    @Test
    fun `given no existing FROM date, when onDateClick(FROM) called, then opens DateDialog with null date`() =
        testBlocking {
            val vm = createVm()

            // When
            vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.FROM)

            // Then
            val state = vm.uiState.getOrAwaitValue()!!
            val dialog = state.pickerDialogState as PickerDialogState.DateDialog
            assertThat(dialog.date).isNull()
        }

    @Test
    fun `given no existing TO date, when onDateClick(TO) called, then opens DateDialog with null date`() =
        testBlocking {
            val vm = createVm()

            vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.TO)

            val state = vm.uiState.getOrAwaitValue()!!
            val dialog = state.pickerDialogState as PickerDialogState.DateDialog
            assertThat(dialog.date).isNull()
        }

    @Test
    fun `given DateDialog opened for FROM, when a date is selected, then fromDateTime and formattedFromDate are updated`() =
        testBlocking {
            val vm = createVm()
            val zone = ZoneId.systemDefault()

            vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.FROM)
            var state = vm.uiState.getOrAwaitValue()!!
            val dialog = state.pickerDialogState as PickerDialogState.DateDialog

            val pickedDateTime = LocalDateTime.of(2024, 5, 10, 0, 0)
            val pickedMillis = pickedDateTime.atZone(zone).toInstant().toEpochMilli()
            dialog.onDateSelected(pickedMillis)

            state = vm.uiState.getOrAwaitValue()!!
            assertThat(state.fromDateTime).isNotNull()
            assertThat(state.fromDateTime!!.year).isEqualTo(2024)
            assertThat(state.fromDateTime.monthValue).isEqualTo(5)
            assertThat(state.fromDateTime.dayOfMonth).isEqualTo(10)
            // time should be preserved from picked (midnight)
            assertThat(state.fromDateTime.hour).isEqualTo(0)
            assertThat(state.fromDateTime.minute).isEqualTo(0)
            assertThat(state.formattedFromDate).isNotEmpty()
            // time is not formatted on date selection
            assertThat(state.formattedFromTime).isEmpty()
            assertThat(state.pickerDialogState).isNull()
        }

    @Test
    fun `given DateDialog opened for TO, when a date is selected, then toDateTime and formattedToDate are updated`() =
        testBlocking {
            val vm = createVm()
            val zone = ZoneId.systemDefault()

            vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.TO)
            var state = vm.uiState.getOrAwaitValue()!!
            val dialog = state.pickerDialogState as PickerDialogState.DateDialog

            val pickedDateTime = LocalDateTime.of(2024, 6, 2, 0, 0)
            val pickedMillis = pickedDateTime.atZone(zone).toInstant().toEpochMilli()
            dialog.onDateSelected(pickedMillis)

            state = vm.uiState.getOrAwaitValue()!!
            assertThat(state.toDateTime).isNotNull()
            assertThat(state.toDateTime!!.year).isEqualTo(2024)
            assertThat(state.toDateTime.monthValue).isEqualTo(6)
            assertThat(state.toDateTime.dayOfMonth).isEqualTo(2)
            assertThat(state.toDateTime.hour).isEqualTo(0)
            assertThat(state.toDateTime.minute).isEqualTo(0)
            assertThat(state.formattedToDate).isNotEmpty()
            assertThat(state.formattedToTime).isEmpty()
            assertThat(state.pickerDialogState).isNull()
        }

    @Test
    fun `given FROM doesn't exists, when onTimeClick(FROM) and a time is selected, then from time and its formatting are updated`() =
        testBlocking {
            val vm = createVm()

            vm.uiState.getOrAwaitValue()!!.onTimeClick(DateBoundary.FROM)
            var state = vm.uiState.getOrAwaitValue()!!
            val timeDialog = state.pickerDialogState as PickerDialogState.TimeDialog
            val newTime = Time(14, 30)
            timeDialog.onTimeSelected(newTime)

            // If date was not selected first we default to now
            val expectedDateTime = LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC)
                .withHour(newTime.hour)
                .withMinute(newTime.minute)
                .withNano(0)
                .withSecond(0)

            state = vm.uiState.getOrAwaitValue()!!
            assertThat(state.fromDateTime).isNotNull()
            assertThat(state.fromDateTime!!.year).isEqualTo(expectedDateTime.year)
            assertThat(state.fromDateTime.monthValue).isEqualTo(expectedDateTime.monthValue)
            assertThat(state.fromDateTime.dayOfMonth).isEqualTo(expectedDateTime.dayOfMonth)
            assertThat(state.fromDateTime.hour).isEqualTo(expectedDateTime.hour)
            assertThat(state.fromDateTime.minute).isEqualTo(expectedDateTime.minute)
            assertThat(state.formattedFromTime).isNotEmpty()
            assertThat(state.formattedFromDate).isNotEmpty()
            assertThat(state.pickerDialogState).isNull()
        }

    @Test
    fun `given TO doesn't exists, when onTimeClick(TO) and a time is selected, then to time and its formatting are updated`() =
        testBlocking {
            val vm = createVm()

            vm.uiState.getOrAwaitValue()!!.onTimeClick(DateBoundary.TO)
            val timeDialog = (vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.TimeDialog)
            val newTime = Time(23, 45)
            timeDialog.onTimeSelected(newTime)

            // If date was not selected first we default to now
            val expectedDateTime = LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC)
                .withHour(newTime.hour)
                .withMinute(newTime.minute)
                .withNano(0)
                .withSecond(0)

            val state = vm.uiState.getOrAwaitValue()!!
            assertThat(state.toDateTime).isNotNull()
            assertThat(state.toDateTime!!.year).isEqualTo(expectedDateTime.year)
            assertThat(state.toDateTime.monthValue).isEqualTo(expectedDateTime.monthValue)
            assertThat(state.toDateTime.dayOfMonth).isEqualTo(expectedDateTime.dayOfMonth)
            assertThat(state.toDateTime.hour).isEqualTo(expectedDateTime.hour)
            assertThat(state.toDateTime.minute).isEqualTo(expectedDateTime.minute)
            assertThat(state.formattedToDate).isNotEmpty()
            assertThat(state.formattedToTime).isNotEmpty()
            assertThat(state.pickerDialogState).isNull()
        }

    @Test
    fun `given existing FROM date, when onDateClick(FROM) called again, then DateDialog is prefilled with matching millis`() =
        testBlocking {
            val vm = createVm()
            val zone = ZoneId.systemDefault()

            // Set FROM date first
            vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.FROM)
            val dialog = vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.DateDialog
            val dateTime = LocalDateTime.of(2025, 1, 5, 10, 0)
            val millis = dateTime.atZone(zone).toInstant().toEpochMilli()
            dialog.onDateSelected(millis)

            // Open again - the dialog should be pre-populated with the existing millis
            vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.FROM)
            val dialog2 = vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.DateDialog
            val providedMillis = dialog2.date
            val expectedMillis = vm.uiState.getOrAwaitValue()!!.fromDateTime!!.atZone(zone).toInstant().toEpochMilli()
            assertThat(providedMillis).isEqualTo(expectedMillis)
        }

    private fun createVm(): DateTimeFilterViewModel {
        return DateTimeFilterViewModel(
            savedStateHandle = SavedStateHandle(),
            clock = clock,
        )
    }
}
