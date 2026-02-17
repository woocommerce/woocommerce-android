package com.woocommerce.android.ui.woopos.bookings.details

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
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosCancelBookingDialog(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    message: String,
    isProcessing: Boolean = false,
    errorMessage: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WooPosDialogWrapper(
        modifier = modifier,
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_bookings_cancel_dialog_background
        ),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_bookings_cancel_dialog_title),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosText(
                text = message,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            WooPosText(
                text = stringResource(R.string.woopos_bookings_cancel_dialog_email_notice),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

                WooPosText(
                    text = errorMessage,
                    style = WooPosTypography.BodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(WooPosSpacing.XXXLarge.value))

            WooPosButton(
                text = stringResource(R.string.woopos_bookings_cancel_dialog_confirm),
                state = if (isProcessing) WooPosButtonState.LOADING else WooPosButtonState.ENABLED,
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButton(
                text = stringResource(R.string.woopos_bookings_cancel_dialog_keep),
                state = if (isProcessing) WooPosButtonState.DISABLED else WooPosButtonState.ENABLED,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosCancelBookingDialogPreview() {
    WooPosTheme {
        WooPosCancelBookingDialog(
            isVisible = true,
            message = "Booking #333 for Women's Haircut on Monday, 05 July 2025" +
                " at 10:30 AM for Margarita Nikolaevna will be canceled.",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
