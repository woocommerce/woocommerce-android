package com.woocommerce.android.ui.woopos.cardreader;

import androidx.compose.runtime.Composable
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentViewModel
import javax.inject.Singleton;

@Singleton
class IppPaymentUIConfig {
    var uiConfig: IppPaymentUI = IppPaymentUI.Default
}

sealed class IppPaymentUI {
    data object Default : IppPaymentUI()
    sealed class Custom : IppPaymentUI() {
        data object Hidden : Custom()
        data class Visible(val uiFactory: (vm: CardReaderPaymentViewModel) -> @Composable ()->Unit) : Custom()
    }
}
