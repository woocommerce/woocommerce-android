@file:OptIn(ExperimentalComposeUiApi::class)

package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
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

private const val DEFAULT_MIN_YEAR = 1900
private const val DEFAULT_MAX_YEAR = 2100

@Composable
fun DatePickerDialog(
    currentDate: Date?,
    onDateSelected: (Date) -> Unit,
    onDismissRequest: () -> Unit,
    neutralButton: (@Composable () -> Unit)? = null,
    minDate: Date = GregorianCalendar(DEFAULT_MIN_YEAR, 0, 1).time,
    maxDate: Date = GregorianCalendar(DEFAULT_MAX_YEAR, 0, 1).time,
    dateFormat: DateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM),
    dialogProperties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
) {
    fun Date.toCalendar() = Calendar.getInstance().apply { time = this@toCalendar }
    DatePickerDialog(
        currentDate = currentDate?.toCalendar(),
        onDateSelected = { onDateSelected(it.time) },
        onDismissRequest = onDismissRequest,
        neutralButton = neutralButton,
        minDate = minDate.toCalendar(),
        maxDate = maxDate.toCalendar(),
        dateFormat = dateFormat,
        dialogProperties = dialogProperties
    )
}

@Suppress("LongMethod")
@Composable
fun DatePickerDialog(
    currentDate: Calendar?,
    onDateSelected: (Calendar) -> Unit,
    onDismissRequest: () -> Unit,
    neutralButton: (@Composable () -> Unit)? = null,
    minDate: Calendar = GregorianCalendar(DEFAULT_MIN_YEAR, 0, 1),
    maxDate: Calendar = GregorianCalendar(DEFAULT_MAX_YEAR, 0, 1),
    dateFormat: DateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM),
    dialogProperties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
) {
    Dialog(onDismissRequest = onDismissRequest, properties = dialogProperties) {
        var selectedDate: Calendar by rememberSaveable { mutableStateOf(currentDate ?: Calendar.getInstance()) }

        val orientation = LocalConfiguration.current.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(
                modifier = Modifier
                    .padding(
                        vertical = dimensionResource(id = R.dimen.major_100),
                        horizontal = dimensionResource(id = R.dimen.major_200)
                    )
                    .width(IntrinsicSize.Max)
                    .height(IntrinsicSize.Max)
                    .clip(MaterialTheme.shapes.medium)
                    .background(color = MaterialTheme.colors.surface)
            ) {
                DatePickerContent(
                    selectedDate = selectedDate,
                    onDateChanged = { selectedDate = it },
                    onSubmitRequest = { onDateSelected(selectedDate) },
                    onDismissRequest = onDismissRequest,
                    neutralButton = neutralButton,
                    minDate = minDate,
                    maxDate = maxDate,
                    dateFormat = dateFormat
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(dimensionResource(id = R.dimen.major_200))
                    .width(IntrinsicSize.Max)
                    .height(IntrinsicSize.Max)
                    .clip(MaterialTheme.shapes.medium)
                    .background(color = MaterialTheme.colors.surface)
            ) {
                DatePickerContent(
                    selectedDate = selectedDate,
                    onDateChanged = { selectedDate = it },
                    onSubmitRequest = { onDateSelected(selectedDate) },
                    onDismissRequest = onDismissRequest,
                    neutralButton = neutralButton,
                    minDate = minDate,
                    maxDate = maxDate,
                    dateFormat = dateFormat
                )
            }
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun Any.DatePickerContent(
    selectedDate: Calendar,
    onDateChanged: (Calendar) -> Unit,
    onSubmitRequest: () -> Unit,
    onDismissRequest: () -> Unit,
    neutralButton: (@Composable () -> Unit)?,
    minDate: Calendar,
    maxDate: Calendar,
    dateFormat: DateFormat,
) {
    var isShowingYearSelector by remember { mutableStateOf(false) }
    // Keep track of the calculated height for the picker, to pass it to the YearSelector
    var pickerSize by remember { mutableStateOf(IntSize(0, 0)) }

    Column(
        Modifier
            .wrapContentSize()
            .run {
                // Fill the height or width depending on parent layout
                if (this@DatePickerContent is RowScope) fillMaxHeight() else fillMaxWidth()
            }
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
    ) {
        if (isShowingYearSelector) {
            YearSelector(
                currentDate = selectedDate,
                minDate = minDate,
                maxDate = maxDate,
                onYearSelected = {
                    isShowingYearSelector = false
                    onDateChanged(selectedDate.apply { year = it })
                },
                modifier = Modifier.size(with(LocalDensity.current) { pickerSize.toSize().toDpSize() })
            )
        } else {
            CalendarView(
                currentDate = selectedDate,
                minDate = minDate,
                maxDate = maxDate,
                onDateSelected = onDateChanged,
                modifier = Modifier.onSizeChanged {
                    pickerSize = it
                }
            )
        }
        DialogButtonsRowLayout(
            confirmButton = {
                TextButton(onClick = onSubmitRequest) {
                    Text(
                        text = stringResource(id = android.R.string.ok),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text(
                        text = stringResource(id = android.R.string.cancel),
                    )
                }
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

@Composable
private fun CalendarView(
    currentDate: Calendar,
    minDate: Calendar?,
    maxDate: Calendar?,
    onDateSelected: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.wrapContentSize(),
        factory = { context ->
            AndroidCalendarView(context)
        },
        update = { view ->
            if (minDate != null)
                view.minDate = minDate.timeInMillis
            if (maxDate != null)
                view.maxDate = maxDate.timeInMillis

            view.setDate(currentDate.timeInMillis, false, true)

            view.setOnDateChangeListener { _, year, month, dayOfMonth ->
                onDateSelected(
                    Calendar
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
private fun YearSelector(
    currentDate: Calendar,
    minDate: Calendar,
    maxDate: Calendar,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYear = currentDate.year
    val items = (minDate.year..maxDate.year).toList()

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(currentYear),
        initialFirstVisibleItemScrollOffset = -2
    )

    LazyColumn(state = lazyListState, modifier = modifier) {
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
                    color = if (it == currentYear) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
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
private fun InteractiveDatePickerPreview() {
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
                    onDismissRequest = { showPicker = false }
                )
            }

            if (date != null) {
                Text(text = "Selected date: ${SimpleDateFormat.getDateInstance().format(date)}")
            }
        }
    }
}
