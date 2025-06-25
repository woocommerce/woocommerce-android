package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector

sealed class WooPosHomeUIEvent {
    data object SystemBackClicked : WooPosHomeUIEvent()
    data object ExitConfirmationDialogDismissed : WooPosHomeUIEvent()
    data object DismissProductsInfoDialog : WooPosHomeUIEvent()
    data object DismissBarcodeInfoDialog : WooPosHomeUIEvent()
    data object OnPaymentCompletedViaCash : WooPosHomeUIEvent()
    data object ExitPosClicked : WooPosHomeUIEvent()
    data class OnBarcodeScanned(
        val barcode: String,
        val metadata: BarcodeInputDetector.ScanMetadata
    ) : WooPosHomeUIEvent()
}
