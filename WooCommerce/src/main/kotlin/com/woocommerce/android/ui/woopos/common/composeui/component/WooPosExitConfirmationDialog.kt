package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    WooPosDialogWrapper(
        modifier = modifier,
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            id = R.string.woopos_dialog_exit_confirmation_background_content_description
        ),
        onDismissRequest = onDismissRequest,
    ) {
        Box(
            modifier = modifier.padding(WooPosSpacing.XLarge.value.toAdaptivePadding())
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                @Suppress("WooPosDesignSystemSpacingUsageRule")
                Spacer(modifier = modifier.height(48.dp.toAdaptivePadding()))
                WooPosText(
                    text = title,
                    style = WooPosTypography.Heading,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
                WooPosText(
                    text = message,
                    style = WooPosTypography.BodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                @Suppress("WooPosDesignSystemSpacingUsageRule")
                Spacer(modifier = modifier.height(56.dp.toAdaptivePadding()))
                WooPosButton(
                    modifier = modifier
                        .fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            onDismissRequest()
                            delay(300)
                            onExit()
                        }
                    },
                    text = dismissButtonText
                )
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
                        .size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
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
