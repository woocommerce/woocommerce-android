package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    time: Time? = null,
    onTimeSelected: (Time) -> Unit,
    onDismissRequest: () -> Unit,
    dialogProperties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
) {
    val timeState = rememberTimePickerState(
        initialHour = time?.hour ?: 0,
        initialMinute = time?.minute ?: 0,
    )

    TimePickerDialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = stringResource(R.string.bookings_filter_time_picker_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        },
        confirmButton = {
            WCTextButton(
                onClick = {
                    onTimeSelected(Time(timeState.hour, timeState.minute))
                },
                text = stringResource(id = android.R.string.ok)
            )
        },
        dismissButton = {
            WCTextButton(
                onClick = { onDismissRequest() },
                text = stringResource(id = android.R.string.cancel)
            )
        }
    ) {
        // WooTheme sets very big (compared to defaults) font sizes. Those are not handled well in the TimePicker,
        // so we have to override those locally.
        val reducedDisplayTypography = MaterialTheme.typography.copy(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontSize = 57.sp),
        )
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            shapes = MaterialTheme.shapes,
            typography = reducedDisplayTypography
        ) {
            TimePicker(
                state = timeState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    }
}

data class Time(
    val hour: Int,
    val minute: Int
)
