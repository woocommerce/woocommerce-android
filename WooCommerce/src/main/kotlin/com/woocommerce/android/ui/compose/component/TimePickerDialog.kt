package com.woocommerce.android.ui.compose.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties

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

    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(Time(timeState.hour, timeState.minute))
                },
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        text = {
            TimePicker(
                state = timeState,
            )
        },
        properties = dialogProperties,
    )
}

data class Time(
    val hour: Int,
    val minute: Int
)
