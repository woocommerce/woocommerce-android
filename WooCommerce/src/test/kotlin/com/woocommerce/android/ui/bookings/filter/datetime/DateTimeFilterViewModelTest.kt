package com.woocommerce.android.ui.bookings.filter.datetime

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.compose.component.Time
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DateTimeFilterViewModelTest : BaseUnitTest() {

    private val zone = ZoneOffset.UTC

    private val clock = Clock.fixed(
        LocalDateTime.of(2025, 11, 13, 10, 0)
            .toInstant(zone),
        zone
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
    fun `given DateDialog opened for FROM, when initial date is selected, then fromDateTime and formattedFromDate are updated`() =
        testBlocking {
            val vm = createVm()

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
            // initial time should be set at start of day
            assertThat(state.fromDateTime.hour).isEqualTo(0)
            assertThat(state.fromDateTime.minute).isEqualTo(0)
            assertThat(state.formattedFromDate).isNotEmpty()
            assertThat(state.formattedFromTime).isNotEmpty()
            assertThat(state.pickerDialogState).isNull()
        }

    @Test
    fun `given DateDialog opened for TO, when initial date is selected, then toDateTime and formattedToDate are updated`() =
        testBlocking {
            val vm = createVm()

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
            // initial time should be set at end of day
            assertThat(state.toDateTime.hour).isEqualTo(23)
            assertThat(state.toDateTime.minute).isEqualTo(59)
            assertThat(state.formattedToDate).isNotEmpty()
            assertThat(state.formattedToTime).isNotEmpty()
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

    @Test
    fun `given initialRange with after and before, when ViewModel is created, then state reflects range`() =
        testBlocking {
            // Given a specific range
            val after = LocalDateTime.of(2025, 1, 2, 3, 4).toInstant(ZoneOffset.UTC)
            val before = LocalDateTime.of(2025, 2, 3, 4, 5).toInstant(ZoneOffset.UTC)
            val range = BookingsFilterOption.DateRange(
                before = before,
                after = after
            )

            // When
            val vm = DateTimeFilterViewModel(
                onTypeFilterChanged = { _ -> },
                savedStateHandle = SavedStateHandle(),
                clock = clock,
                initialRange = range,
            )

            // Then
            val expectedFrom = after.atZone(zone).toLocalDateTime()
            val expectedTo = before.atZone(zone).toLocalDateTime()
            val state = vm.uiState.getOrAwaitValue()!!
            assertThat(state.fromDateTime).isEqualTo(expectedFrom)
            assertThat(state.toDateTime).isEqualTo(expectedTo)
            // formatted strings should be populated
            assertThat(state.formattedFromDate).isNotEmpty()
            assertThat(state.formattedFromTime).isNotEmpty()
            assertThat(state.formattedToDate).isNotEmpty()
            assertThat(state.formattedToTime).isNotEmpty()
        }

    @Test
    fun `given existing TO date, when onDateClick(FROM), then DateDialog maxDate equals TO startOfDayUTC millis`() =
        testBlocking {
            val vm = createVm()

            // First select TO date
            vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.TO)
            val toDialog = vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.DateDialog
            val toLocalDateTime = LocalDateTime.of(2025, 6, 15, 0, 0)
            val toMillisInput = toLocalDateTime.atZone(zone).toInstant().toEpochMilli()
            toDialog.onDateSelected(toMillisInput)

            // Now open FROM dialog; it should constrain by maxDate = TO (startOfDay UTC)
            vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.FROM)
            val fromDialog = vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.DateDialog

            val expectedMaxMillis = toLocalDateTime
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

            assertThat(fromDialog.maxDate).isEqualTo(expectedMaxMillis)
        }

    @Test
    fun `given existing FROM date, when onDateClick(TO), then DateDialog minDate equals FROM startOfDayUTC millis`() =
        testBlocking {
            val vm = createVm()

            // First select FROM date
            vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.FROM)
            val fromDialogInitial = vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.DateDialog
            val fromLocalDateTime = LocalDateTime.of(2025, 6, 1, 0, 0)
            val fromMillisInput = fromLocalDateTime.atZone(zone).toInstant().toEpochMilli()
            fromDialogInitial.onDateSelected(fromMillisInput)

            // Now open TO dialog; it should constrain by minDate = FROM (startOfDay UTC)
            vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.TO)
            val toDialog = vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.DateDialog

            val expectedMinMillis = fromLocalDateTime
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

            assertThat(toDialog.minDate).isEqualTo(expectedMinMillis)
        }

    @Test
    fun `given no TO date, when onDateClick(FROM), then DateDialog maxDate is null`() = testBlocking {
        val vm = createVm()

        vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.FROM)
        val dialog = vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.DateDialog

        assertThat(dialog.maxDate).isNull()
    }

    @Test
    fun `given FROM and TO exist, when selecting a FROM time after TO, then values are swapped`() =
        testBlocking {
            // Given
            val fromInitial = LocalDateTime.of(2025, 1, 1, 9, 0)
            val toInitial = LocalDateTime.of(2025, 1, 1, 10, 0)
            val vm = DateTimeFilterViewModel(
                onTypeFilterChanged = { _ -> },
                savedStateHandle = SavedStateHandle(),
                clock = clock,
                initialRange = BookingsFilterOption.DateRange(
                    after = fromInitial.toInstant(ZoneOffset.UTC),
                    before = toInitial.toInstant(ZoneOffset.UTC)
                )
            )

            // When: user opens FROM time dialog and selects a time after current TO (11:00)
            vm.uiState.getOrAwaitValue()!!.onTimeClick(DateBoundary.FROM)
            val timeDialog = vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.TimeDialog
            timeDialog.onTimeSelected(Time(11, 0))

            // Then: the values are swapped so that FROM = previous TO (10:00) and TO = new 11:00
            val state = vm.uiState.getOrAwaitValue()!!
            assertThat(state.fromDateTime).isEqualTo(toInitial)
            assertThat(state.toDateTime).isEqualTo(fromInitial.withHour(11).withMinute(0).withSecond(0).withNano(0))
            // formatted strings should be present and dialog dismissed
            assertThat(state.formattedFromDate).isNotEmpty()
            assertThat(state.formattedFromTime).isNotEmpty()
            assertThat(state.formattedToDate).isNotEmpty()
            assertThat(state.formattedToTime).isNotEmpty()
            assertThat(state.pickerDialogState).isNull()
        }

    @Test
    fun `given FROM and TO exist, when selecting a TO time before FROM, then values are swapped`() =
        testBlocking {
            // Given
            val fromInitial = LocalDateTime.of(2025, 1, 1, 9, 0)
            val toInitial = LocalDateTime.of(2025, 1, 1, 10, 0)
            val vm = DateTimeFilterViewModel(
                onTypeFilterChanged = { _ -> },
                savedStateHandle = SavedStateHandle(),
                clock = clock,
                initialRange = BookingsFilterOption.DateRange(
                    after = fromInitial.toInstant(ZoneOffset.UTC),
                    before = toInitial.toInstant(ZoneOffset.UTC)
                )
            )

            // When: user opens TO time dialog and selects a time before current FROM (08:00)
            vm.uiState.getOrAwaitValue()!!.onTimeClick(DateBoundary.TO)
            val timeDialog = vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.TimeDialog
            timeDialog.onTimeSelected(Time(8, 0))

            // Then: the values are swapped so that FROM = new 08:00 and TO = previous FROM (09:00)
            val state = vm.uiState.getOrAwaitValue()!!
            assertThat(state.fromDateTime).isEqualTo(fromInitial.withHour(8).withMinute(0).withSecond(0).withNano(0))
            assertThat(state.toDateTime).isEqualTo(fromInitial)
            // formatted strings should be present and dialog dismissed
            assertThat(state.formattedFromDate).isNotEmpty()
            assertThat(state.formattedFromTime).isNotEmpty()
            assertThat(state.formattedToDate).isNotEmpty()
            assertThat(state.formattedToTime).isNotEmpty()
            assertThat(state.pickerDialogState).isNull()
        }

    @Test
    fun `given no FROM date, when onDateClick(TO), then DateDialog minDate is null`() = testBlocking {
        val vm = createVm()

        vm.uiState.getOrAwaitValue()!!.onDateClick(DateBoundary.TO)
        val dialog = vm.uiState.getOrAwaitValue()!!.pickerDialogState as PickerDialogState.DateDialog

        assertThat(dialog.minDate).isNull()
    }

    private fun createVm(): DateTimeFilterViewModel {
        return DateTimeFilterViewModel(
            onTypeFilterChanged = { _ -> },
            savedStateHandle = SavedStateHandle(),
            clock = clock,
        )
    }
}
