package com.woocommerce.android.ui.woopos.home.items.customamount

import java.math.BigDecimal

sealed class WooPosCustomAmountUIEvent {
    data class AmountChanged(val amount: BigDecimal?) : WooPosCustomAmountUIEvent()
    data class NameChanged(val name: String) : WooPosCustomAmountUIEvent()
    data class TaxableToggled(val isTaxable: Boolean) : WooPosCustomAmountUIEvent()
    data object AmountConfirmed : WooPosCustomAmountUIEvent()
    data object EditAmountClicked : WooPosCustomAmountUIEvent()
    data object AmountBackClicked : WooPosCustomAmountUIEvent()
    data object EditNameClicked : WooPosCustomAmountUIEvent()
    data class NameDraftChanged(val name: String) : WooPosCustomAmountUIEvent()
    data object NameConfirmed : WooPosCustomAmountUIEvent()
    data object NameBackClicked : WooPosCustomAmountUIEvent()
    data object SubmitClicked : WooPosCustomAmountUIEvent()
    data object Closed : WooPosCustomAmountUIEvent()
}
