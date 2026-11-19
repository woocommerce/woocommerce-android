package com.woocommerce.android.ui.woopos.receiptprinting

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosConnectionDialogContent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosPrintReceiptFailureDialog(
    failure: WooPosPrintReceiptFailure,
    onActionSelected: (WooPosPrintReceiptFailureAction) -> Unit,
    onDismiss: () -> Unit,
) {
    WooPosDialogWrapper(
        isVisible = true,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_print_receipt_failure_dialog_background_content_description
        ),
        widthFraction = 0.55f,
        onCloseClick = onDismiss,
        onDismissRequest = onDismiss,
    ) {
        WooPosConnectionDialogContent(
            title = stringResource(failure.title),
            visual = {
                Image(
                    imageVector = WooPosIcons.ErrorX,
                    contentDescription = null,
                    modifier = Modifier.size(WooPosComponentSize.XLarge.value),
                )
            },
        ) {
            WooPosText(
                text = stringResource(failure.message),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            WooPosButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.woopos_print_receipt_failure_retry_button),
                onClick = { onActionSelected(WooPosPrintReceiptFailureAction.Retry) },
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.woopos_receipt_button),
                onClick = { onActionSelected(WooPosPrintReceiptFailureAction.EmailReceipt) },
            )
        }
    }
}

@Composable
private fun PrintReceiptFailureDialogPreview(failure: WooPosPrintReceiptFailure) {
    WooPosTheme {
        WooPosPrintReceiptFailureDialog(
            failure = failure,
            onActionSelected = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosPrintReceiptFailureDialogPaperEmptyPreview() {
    PrintReceiptFailureDialogPreview(WooPosPrintReceiptFailure.PaperEmpty)
}

@WooPosPreview
@Composable
fun WooPosPrintReceiptFailureDialogCoverOpenPreview() {
    PrintReceiptFailureDialogPreview(WooPosPrintReceiptFailure.CoverOpen)
}

@WooPosPreview
@Composable
fun WooPosPrintReceiptFailureDialogPrinterOfflinePreview() {
    PrintReceiptFailureDialogPreview(WooPosPrintReceiptFailure.PrinterOffline)
}

@WooPosPreview
@Composable
fun WooPosPrintReceiptFailureDialogGenericPreview() {
    PrintReceiptFailureDialogPreview(WooPosPrintReceiptFailure.Generic)
}
