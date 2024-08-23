package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding

@Composable
fun WooPosExitConfirmationDialog(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    title: String,
    message: String,
    dismissButtonText: String,
    onDismissRequest: () -> Unit,
    onExit: () -> Unit
) {
    WooPosDialogWrapper(
        modifier = modifier
            .fillMaxWidth()
            .padding(102.dp.toAdaptivePadding()),
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            id = R.string.woopos_dialog_exit_confirmation_background_content_description
        ),
        onDismissRequest = onDismissRequest,
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = 8.dp,
            modifier = modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = modifier.padding(
                    horizontal = 150.dp.toAdaptivePadding(),
                    vertical = 40.dp.toAdaptivePadding()
                )
            ) {
                Column(
                    modifier = modifier.padding(16.dp.toAdaptivePadding()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = modifier.height(48.dp.toAdaptivePadding()))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = modifier.height(16.dp.toAdaptivePadding()))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = modifier.height(56.dp.toAdaptivePadding()))
                    Button(
                        onClick = {
                            onExit()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = dismissButtonText,
                            style = MaterialTheme.typography.h5,
                            color = MaterialTheme.colors.onPrimary,
                            modifier = modifier
                                .padding(20.dp.toAdaptivePadding())
                        )
                    }
                }

                IconButton(
                    onClick = { onDismissRequest() },
                    modifier = modifier
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(
                            id = R.string.woopos_exit_dialog_confirmation_close_content_description
                        ),
                        modifier = modifier
                            .padding(16.dp.toAdaptivePadding())
                            .size(40.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosExitConfirmationDialogPreview() {
    WooPosTheme {
        WooPosExitConfirmationDialog(
            isVisible = true,
            title = "Exit Point of Sale mode?",
            message = "Any orders in progress will be lost.",
            dismissButtonText = "Exit",
            onDismissRequest = {},
            onExit = {}
        )
    }
}
