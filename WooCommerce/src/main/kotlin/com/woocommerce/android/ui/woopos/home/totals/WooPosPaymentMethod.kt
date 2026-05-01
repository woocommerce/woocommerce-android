package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

internal enum class WooPosPaymentMethod {
    CARD_READER,
    CASH,
}

internal fun WooPosPaymentMethod.labelRes(): Int = when (this) {
    WooPosPaymentMethod.CARD_READER -> R.string.woopos_payment_method_card_reader_label
    WooPosPaymentMethod.CASH -> R.string.woopos_payment_take_cash_payment_label
}

internal fun WooPosPaymentMethod.testTag(): String = when (this) {
    WooPosPaymentMethod.CARD_READER -> WooPosTestTags.CARD_READER_PAYMENT_BUTTON
    WooPosPaymentMethod.CASH -> WooPosTestTags.CASH_PAYMENT_BUTTON
}
