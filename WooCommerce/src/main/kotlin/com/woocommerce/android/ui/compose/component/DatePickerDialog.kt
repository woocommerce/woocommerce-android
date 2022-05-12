package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import android.widget.CalendarView as AndroidCalendarView

@Composable
fun DatePickerDialog(
    currentDate: Date?,
    onDateSelected: (Date) -> Unit,
    onDismissRequest: () -> Unit,
    minDate: Date = GregorianCalendar(1900, 0, 1).time,
    maxDate: Date = GregorianCalendar(2100, 0, 1).time,
    dateFormat: DateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM),
    dialogProperties: DialogProperties = DialogProperties()
) {
    fun Date.toCalendar() = Calendar.getInstance().apply { time = this@toCalendar }
    DatePickerDialog(
        currentDate = currentDate?.toCalendar(),
        onDateSelected = { onDateSelected(it.time) },
        onDismissRequest = onDismissRequest,
        minDate = minDate.toCalendar(),
        maxDate = maxDate.toCalendar(),
        dateFormat = dateFormat,
        dialogProperties = dialogProperties
    )
}

@Composable
fun DatePickerDialog(
    currentDate: Calendar?,
    onDateSelected: (Calendar) -> Unit,
    onDismissRequest: () -> Unit,
    minDate: Calendar = GregorianCalendar(1900, 0, 1),
    maxDate: Calendar = GregorianCalendar(2100, 0, 1),
    dateFormat: DateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM),
    dialogProperties: DialogProperties = DialogProperties()
) {
    var selectedDate: Calendar by remember { mutableStateOf(currentDate ?: Calendar.getInstance()) }
    var isShowingYearSelector by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest, properties = dialogProperties) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = MaterialTheme.shapes.large
                )
        ) {
            Column(
                Modifier
                    .defaultMinSize(minHeight = 72.dp)
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colors.primary)
                    .padding(dimensionResource(id = R.dimen.major_100))
            ) {
                Text(
                    text = selectedDate.year.toString(),
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onPrimary,
                    modifier = Modifier.clickable { isShowingYearSelector = true }
                )

                Text(
                    text = dateFormat.format(selectedDate.time),
                    style = MaterialTheme.typography.h5,
                    color = MaterialTheme.colors.onPrimary
                )
            }

            if (isShowingYearSelector) {
                YearSelector(
                    currentDate = selectedDate,
                    minDate = minDate,
                    maxDate = maxDate,
                    onYearSelected = {
                        isShowingYearSelector = false
                        selectedDate = selectedDate
                            .apply { year = it }
                    }
                )
            } else {
                CalendarView(
                    currentDate = selectedDate,
                    minDate = minDate,
                    maxDate = maxDate,
                    onDateSelected = { selectedDate = it }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    space = dimensionResource(id = R.dimen.minor_100),
                    alignment = Alignment.End
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = stringResource(id = android.R.string.cancel),
                    )
                }

                TextButton(onClick = { onDateSelected(selectedDate) }) {
                    Text(
                        text = stringResource(id = android.R.string.ok),
                    )
                }

            }
        }
    }
}

@Composable
private fun CalendarView(
    currentDate: Calendar,
    minDate: Calendar? = null,
    maxDate: Calendar? = null,
    onDateSelected: (Calendar) -> Unit
) {
    AndroidView(
        modifier = Modifier.wrapContentSize(),
        factory = { context ->
            AndroidCalendarView(context)
        },
        update = { view ->
            if (minDate != null)
                view.minDate = minDate.timeInMillis
            if (maxDate != null)
                view.maxDate = maxDate.timeInMillis

            view.date = currentDate.timeInMillis

            view.setOnDateChangeListener { _, year, month, dayOfMonth ->
                onDateSelected(Calendar
                    .getInstance()
                    .apply {
                        set(year, month, dayOfMonth)
                    }
                )
            }
        }
    )
}

@Composable
fun YearSelector(currentDate: Calendar, minDate: Calendar, maxDate: Calendar, onYearSelected: (Int) -> Unit) {
    val currentYear = currentDate.year
    val items = (minDate.year..maxDate.year).toList()

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(currentYear),
        initialFirstVisibleItemScrollOffset = -2
    )

    LazyColumn(state = lazyListState) {
        items(items) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable { onYearSelected(it) }
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.major_75))
            ) {
                Text(
                    text = it.toString(),
                    style = if (it == currentYear) MaterialTheme.typography.h5 else MaterialTheme.typography.body1,
                    color = if (it == currentYear) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                    modifier = Modifier
                )
            }
        }
    }
}

private var Calendar.year
    get() = this.get(Calendar.YEAR)
    set(value) = set(Calendar.YEAR, value)


@Preview
@Composable
fun DatePickerPreview() {
    WooThemeWithBackground {
        var date by remember { mutableStateOf<Date?>(null) }
        var showPicker by remember { mutableStateOf(false) }

        Column {
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
                    onDismissRequest = { showPicker = false })
            }

            if (date != null) {
                Text(text = "Selected date: ${SimpleDateFormat.getDateInstance().format(date)}")
            }
        }
    }
}
