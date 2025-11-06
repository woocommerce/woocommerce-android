@file:OptIn(ExperimentalMaterial3Api::class)

package com.woocommerce.android.ui.bookings.filter.datetime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.DatePickerDialog
import com.woocommerce.android.ui.compose.component.Time
import com.woocommerce.android.ui.compose.component.TimePickerDialog
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooTheme
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DateTimeFilterPage() {
    val now = remember { Calendar.getInstance() }
    var fromCal by remember {
        mutableStateOf(now.clone() as Calendar)
    }
    var toCal by remember {
        mutableStateOf((now.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) })
    }

    var showDateDialog by rememberSaveable { mutableStateOf(false) }
    var showTimeDialog by rememberSaveable { mutableStateOf(false) }
    var targetIsFrom by rememberSaveable { mutableStateOf(true) }

    val dateFormatter = remember {
        SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault()) as DateFormat
    }
    val timeFormatter = remember {
        SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault()) as DateFormat
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        DateTimeSection(
            label = stringResource(id = R.string.bookings_filter_date_from),
            dateReadable = dateFormatter.format(fromCal.time),
            timeReadable = timeFormatter.format(fromCal.time),
            onDateClick = {
                targetIsFrom = true
                showDateDialog = true
            },
            onTimeClick = {
                targetIsFrom = true
                showTimeDialog = true
            },
        )
        DateTimeSection(
            label = stringResource(id = R.string.bookings_filter_date_to),
            dateReadable = dateFormatter.format(toCal.time),
            timeReadable = timeFormatter.format(toCal.time),
            onDateClick = {
                targetIsFrom = false
                showDateDialog = true
            },
            onTimeClick = {
                targetIsFrom = false
                showTimeDialog = true
            },
        )
    }

    // Render dialogs at the top level so state is controlled here
    if (showDateDialog) {
        val currentMillis = if (targetIsFrom) fromCal.timeInMillis else toCal.timeInMillis
        DatePickerDialog(
            currentDate = Calendar.getInstance().apply { timeInMillis = currentMillis },
            onDateSelected = { selected ->
                if (targetIsFrom) {
                    fromCal = (fromCal.clone() as Calendar).apply { timeInMillis = selected.timeInMillis }
                } else {
                    toCal = (toCal.clone() as Calendar).apply { timeInMillis = selected.timeInMillis }
                }
                showDateDialog = false
            },
            onDismissRequest = { showDateDialog = false },
        )
    }
    if (showTimeDialog) {
        val currentTime = if (targetIsFrom) {
            Time(fromCal.get(Calendar.HOUR_OF_DAY), fromCal.get(Calendar.MINUTE))
        } else {
            Time(toCal.get(Calendar.HOUR_OF_DAY), toCal.get(Calendar.MINUTE))
        }
        TimePickerDialog(
            time = currentTime,
            onTimeSelected = { selected ->
                if (targetIsFrom) {
                    fromCal = (fromCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, selected.hour)
                        set(Calendar.MINUTE, selected.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                } else {
                    toCal = (toCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, selected.hour)
                        set(Calendar.MINUTE, selected.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }
                showTimeDialog = false
            },
            onDismissRequest = { showTimeDialog = false },
        )
    }
}

@Composable
private fun DateTimeSection(
    label: String,
    dateReadable: String,
    timeReadable: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    )
    HorizontalDivider(thickness = 0.5.dp)
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        DateTimeRow(
            label = stringResource(id = R.string.bookings_filter_date_label),
            value = dateReadable,
            onClick = onDateClick
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            thickness = 0.5.dp
        )
        DateTimeRow(
            label = stringResource(id = R.string.bookings_filter_time_label),
            value = timeReadable,
            onClick = onTimeClick
        )
        HorizontalDivider(thickness = 0.5.dp)
    }
    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
private fun DateTimeRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun DateTimeFilterPagePreview() {
    WooTheme {
        DateTimeFilterPage()
    }
}
