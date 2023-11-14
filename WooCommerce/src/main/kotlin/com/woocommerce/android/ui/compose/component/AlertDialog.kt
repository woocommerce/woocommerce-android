package com.woocommerce.android.ui.compose.component

import android.R.string
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

/**
 * An [androidx.compose.material.AlertDialog] that supports a third neutral button.
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable () -> Unit,
    neutralButton: (@Composable () -> Unit),
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties()
) {
    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismissRequest,
        buttons = {
            DialogButtonsRowLayout(
                confirmButton = confirmButton,
                dismissButton = dismissButton,
                neutralButton = neutralButton,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(id = R.dimen.minor_100),
                    vertical = dimensionResource(id = R.dimen.minor_25)
                )
            )
        },
        modifier = modifier,
        title = title,
        text = text,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        properties = properties
    )
}

@Preview
@Composable
private fun AlertDialogPreview() {
    WooThemeWithBackground {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(text = "Title")
            },
            text = {
                Text(text = "This is a long text to preview the dialog dasd asdas ddsad")
            },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text(stringResource(id = string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text(stringResource(id = string.cancel))
                }
            },
            neutralButton = {
                TextButton(onClick = { }) {
                    Text("Neutral")
                }
            }
        )
    }
}
