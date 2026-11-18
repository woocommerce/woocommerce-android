package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.barcodescanner.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptFailureAction

sealed class WooPosHomeUIEvent {
    data object SystemBackClicked : WooPosHomeUIEvent()
    data object ExitConfirmationDialogDismissed : WooPosHomeUIEvent()
    data object DismissScanningSetupDialog : WooPosHomeUIEvent()
    data object DismissCardReaderConnectionDialog : WooPosHomeUIEvent()
    data object DismissReceiptPrinterSetupDialog : WooPosHomeUIEvent()
    data object DismissPrintReceiptFailureDialog : WooPosHomeUIEvent()
    data class PrintReceiptFailureActionSelected(
        val action: WooPosPrintReceiptFailureAction
    ) : WooPosHomeUIEvent()
    data object OnPaymentCompletedViaCash : WooPosHomeUIEvent()
    data object ExitPosClicked : WooPosHomeUIEvent()
    data object PhoneOpenCartClicked : WooPosHomeUIEvent()
    data object PhoneBackFromCartClicked : WooPosHomeUIEvent()
    data object PhoneBackFromCheckoutClicked : WooPosHomeUIEvent()
    data class OnBarcodeEvent(
        val result: BarcodeInputDetector.BarcodeResult
    ) : WooPosHomeUIEvent()
}
