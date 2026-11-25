package com.woocommerce.android.ui.woopos.receiptprinting

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme

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
        MessageContent(
            title = stringResource(failure.title),
            message = stringResource(failure.message),
            icon = WooPosIcons.ErrorX,
            primaryButton = SetupButton(
                text = stringResource(R.string.woopos_print_receipt_failure_retry_button),
                onClick = { onActionSelected(WooPosPrintReceiptFailureAction.Retry) },
            ),
            modifier = Modifier.verticalScroll(rememberScrollState()),
            secondaryButton = SetupButton(
                text = stringResource(R.string.woopos_receipt_button),
                onClick = { onActionSelected(WooPosPrintReceiptFailureAction.EmailReceipt) },
            ),
        )
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
