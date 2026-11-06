package com.woocommerce.android.ui.woopos.home.items.customamount

import java.math.BigDecimal

sealed class WooPosCustomAmountUIEvent {
    data class AmountChanged(val amount: BigDecimal?) : WooPosCustomAmountUIEvent()
    data class NameChanged(val name: String) : WooPosCustomAmountUIEvent()
    data class TaxableToggled(val isTaxable: Boolean) : WooPosCustomAmountUIEvent()
    data object SubmitClicked : WooPosCustomAmountUIEvent()
    data object Closed : WooPosCustomAmountUIEvent()
}
