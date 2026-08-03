@file:OptIn(ExperimentalMaterial3Api::class)

package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

private const val DEFAULT_MIN_YEAR = 1900
private const val DEFAULT_MAX_YEAR = 2100

@Composable
fun LocalDatePickerDialog(
    currentDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
    neutralButton: (@Composable () -> Unit)? = null,
) {
    val dateFormat = remember {
        SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    DatePickerDialog(
        currentDate = currentDate?.toUtcDatePickerCalendar(),
        onDateSelected = { onDateSelected(it.timeInMillis.toUtcLocalDate()) },
        onDismissRequest = onDismissRequest,
        neutralButton = neutralButton,
        dateFormat = dateFormat
    )
}

internal fun LocalDate.toUtcDatePickerCalendar(): Calendar =
    GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            this@toUtcDatePickerCalendar.year,
            this@toUtcDatePickerCalendar.monthValue - 1,
            this@toUtcDatePickerCalendar.dayOfMonth
        )
    }

internal fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
fun DatePickerDialog(
    currentDate: Date?,
    onDateSelected: (Date) -> Unit,
    onDismissRequest: () -> Unit,
    neutralButton: (@Composable () -> Unit)? = null,
    shape: Shape = DatePickerDefaults.shape,
    minDate: Date? = null,
    maxDate: Date? = null,
    dateFormat: DateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM),
    dialogProperties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
) {
    fun Date.toCalendar() = Calendar.getInstance().apply { time = this@toCalendar }
    DatePickerDialog(
        currentDate = currentDate?.toCalendar(),
        onDateSelected = { onDateSelected(it.time) },
        onDismissRequest = onDismissRequest,
        shape = shape,
        neutralButton = neutralButton,
        minDate = (minDate ?: GregorianCalendar(DEFAULT_MIN_YEAR, 0, 1).time).toCalendar(),
        maxDate = (maxDate ?: GregorianCalendar(DEFAULT_MAX_YEAR, 0, 1).time).toCalendar(),
        dateFormat = dateFormat,
        dialogProperties = dialogProperties
    )
}

@Composable
fun DatePickerDialog(
    currentDate: Calendar?,
    onDateSelected: (Calendar) -> Unit,
    onDismissRequest: () -> Unit,
    shape: Shape = DatePickerDefaults.shape,
    neutralButton: (@Composable () -> Unit)? = null,
    minDate: Calendar? = null,
    maxDate: Calendar? = null,
    dateFormat: DateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM),
    dialogProperties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties
    ) {
        val minDefault = minDate ?: GregorianCalendar(DEFAULT_MIN_YEAR, 0, 1)
        val maxDefault = maxDate ?: GregorianCalendar(DEFAULT_MAX_YEAR, 0, 1)
        val selectableDates = remember(minDefault.timeInMillis, maxDefault.timeInMillis) {
            createSelectableDates(minDefault, maxDefault)
        }
        val dateFormatter = remember(dateFormat) { dateFormat.toDatePickerFormatter() }

        val state = rememberDatePickerState(
            initialSelectedDateMillis = currentDate?.timeInMillis,
            selectableDates = selectableDates
        )

        val colors = DatePickerDefaults.colors()

        Surface(
            modifier = Modifier
                // Match the default constraints of the Material3 date picker dialog
                .clip(shape)
                .requiredWidth(360.dp)
                .heightIn(max = 568.dp),
            color = colors.containerColor
        ) {
            Column {
                Box(Modifier.weight(1f, fill = false)) {
                    DatePicker(
                        state = state,
                        colors = colors,
                        dateFormatter = dateFormatter
                    )
                }

                DialogButtonsRowLayout(
                    confirmButton = {
                        WCTextButton(
                            text = stringResource(id = android.R.string.ok),
                            enabled = state.selectedDateMillis != null,
                            onClick = {
                                state.selectedDateMillis?.let {
                                    onDateSelected(
                                        Calendar.getInstance().apply {
                                            timeInMillis = it
                                        }
                                    )
                                }
                            }
                        )
                    },
                    dismissButton = {
                        WCTextButton(
                            text = stringResource(id = android.R.string.cancel),
                            onClick = onDismissRequest
                        )
                    },
                    neutralButton = neutralButton,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = dimensionResource(id = R.dimen.minor_100),
                            vertical = dimensionResource(id = R.dimen.minor_25)
                        )
                )
            }
        }
    }
}

@Suppress("MagicNumber")
private fun createSelectableDates(
    minDate: Calendar,
    maxDate: Calendar
) = object : SelectableDates {
    init {
        // Set minDate to the start of the day and maxDate to the end of the day
        // To ensure that the selection includes the entire day
        minDate.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        maxDate.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
    }

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis in minDate.timeInMillis..maxDate.timeInMillis
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year in minDate.year..maxDate.year
    }
}

private fun DateFormat.toDatePickerFormatter() = object : DatePickerFormatter {
    private val defaultFormatter = DatePickerDefaults.dateFormatter()

    override fun formatDate(dateMillis: Long?, locale: CalendarLocale, forContentDescription: Boolean): String? {
        return dateMillis?.let {
            return this@toDatePickerFormatter.format(Date(it))
        }
    }

    override fun formatMonthYear(monthMillis: Long?, locale: CalendarLocale): String? =
        defaultFormatter.formatMonthYear(monthMillis, locale)
}

private val Calendar.year
    get() = this.get(Calendar.YEAR)

@Preview
@Composable
private fun InteractiveDatePickerPreview() {
    WooThemeWithBackground {
        var date by remember { mutableStateOf<Date?>(null) }
        var showPicker by remember { mutableStateOf(false) }

        Column(modifier = Modifier.safeContentPadding()) {
            Button(onClick = { showPicker = true }) {
                Text(text = "Pick a date")
            }

            if (showPicker) {
                DatePickerDialog(
                    currentDate = date,
                    onDateSelected = {
                        date = it
                        showPicker = false
                    },
                    onDismissRequest = { showPicker = false }
                )
            }

            date?.let {
                Text(text = "Selected date: ${SimpleDateFormat.getDateInstance().format(it)}")
            }
        }
    }
}
