package com.woocommerce.android.ui.bookings.compose

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.AlertDialog

@Composable
fun CancelBookingDialog(
    message: String,
    onDismiss: () -> Unit,
    onConfirmCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.booking_cancel_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(
                onClick = onConfirmCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = stringResource(id = R.string.booking_cancel_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                Text(text = stringResource(id = R.string.booking_cancel_dialog_keep))
            }
        },
        neutralButton = { }
    )
}
