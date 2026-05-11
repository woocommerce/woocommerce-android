package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector

sealed class WooPosHomeUIEvent {
    data object SystemBackClicked : WooPosHomeUIEvent()
    data object ExitConfirmationDialogDismissed : WooPosHomeUIEvent()
    data object DismissScanningSetupDialog : WooPosHomeUIEvent()
    data object DismissCardReaderConnectionDialog : WooPosHomeUIEvent()
    data object DismissCustomAmountDialog : WooPosHomeUIEvent()
    data object OnPaymentCompletedViaCash : WooPosHomeUIEvent()
    data object ExitPosClicked : WooPosHomeUIEvent()
    data object PhoneOpenCartClicked : WooPosHomeUIEvent()
    data object PhoneBackFromCartClicked : WooPosHomeUIEvent()
    data object PhoneBackFromCheckoutClicked : WooPosHomeUIEvent()
    data class OnBarcodeEvent(
        val result: BarcodeInputDetector.BarcodeResult
    ) : WooPosHomeUIEvent()
}
