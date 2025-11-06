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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.DatePickerDialog
import com.woocommerce.android.ui.compose.component.TimePickerDialog
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar

@Composable
fun DateTimeFilterPage() {
    val viewModel: DateTimeFilterViewModel = hiltViewModel()
    val state by viewModel.uiState.observeAsState()
    state?.let { DateTimeFilterPage(it) }
}

@Composable
fun DateTimeFilterPage(
    state: DateTimeFilterUiState
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        DateTimeSection(
            label = stringResource(id = R.string.bookings_filter_date_from),
            dateReadable = state.formattedFromDate,
            timeReadable = state.formattedFromTime,
            onDateClick = { state.onDateClick(DateBoundary.FROM) },
            onTimeClick = { state.onTimeClick(DateBoundary.FROM) },
        )
        DateTimeSection(
            label = stringResource(id = R.string.bookings_filter_date_to),
            dateReadable = state.formattedToDate,
            timeReadable = state.formattedToTime,
            onDateClick = { state.onDateClick(DateBoundary.TO) },
            onTimeClick = { state.onTimeClick(DateBoundary.TO) },
        )
    }

    // Render dialogs at the top level so state is controlled by the ViewModel
    state.pickerDialogState?.let { dialogState ->
        when (dialogState) {
            is PickerDialogState.DateDialog -> {
                DatePickerDialog(
                    currentDate = dialogState.date?.let { Calendar.getInstance().apply { timeInMillis = it } },
                    onDateSelected = { calendar ->
                        dialogState.onDateSelected(calendar.timeInMillis)
                    },
                    onDismissRequest = dialogState.onDismiss,
                )
            }

            is PickerDialogState.TimeDialog -> {
                TimePickerDialog(
                    time = dialogState.time,
                    onTimeSelected = { time ->
                        dialogState.onTimeSelected(time)
                    },
                    onDismissRequest = dialogState.onDismiss,
                )
            }
        }
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
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(1762445828L), ZoneId.systemDefault())
        val later = now.withHour(now.hour + 1)
        DateTimeFilterPage(
            state = DateTimeFilterUiState(
                fromDateTime = now,
                toDateTime = later,
                formattedFromDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .format(now),
                formattedFromTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    .format(now),
                formattedToDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .format(later),
                formattedToTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    .format(later),
            )
        )
    }
}
