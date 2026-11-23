package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

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
        modifier = modifier,
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            id = R.string.woopos_dialog_exit_confirmation_background_content_description
        ),
        onCloseClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WooPosText(
                text = title,
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            WooPosText(
                text = message,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.XXXLarge.value))
            WooPosButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onDismissRequest()
                    onExit()
                },
                text = dismissButtonText
            )
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
