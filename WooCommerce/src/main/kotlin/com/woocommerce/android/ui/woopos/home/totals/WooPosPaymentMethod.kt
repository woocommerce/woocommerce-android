package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

internal enum class WooPosPaymentMethod {
    CARD_READER,
    TAP_TO_PAY,
    SCAN_TO_PAY,
    MARK_ORDER_AS_PAID,
}

internal fun WooPosPaymentMethod.labelRes(): Int = when (this) {
    WooPosPaymentMethod.CARD_READER -> R.string.woopos_payment_method_card_reader_label
    WooPosPaymentMethod.TAP_TO_PAY -> R.string.woopos_payment_method_tap_to_pay_label
    WooPosPaymentMethod.SCAN_TO_PAY -> R.string.woopos_payment_method_scan_to_pay_label
    WooPosPaymentMethod.MARK_ORDER_AS_PAID -> R.string.woopos_payment_method_mark_order_as_paid_label
}

internal fun WooPosPaymentMethod.testTag(): String = when (this) {
    WooPosPaymentMethod.CARD_READER -> WooPosTestTags.CARD_READER_PAYMENT_METHOD_BUTTON
    WooPosPaymentMethod.TAP_TO_PAY -> WooPosTestTags.TAP_TO_PAY_PAYMENT_BUTTON
    WooPosPaymentMethod.SCAN_TO_PAY -> WooPosTestTags.SCAN_TO_PAY_PAYMENT_BUTTON
    WooPosPaymentMethod.MARK_ORDER_AS_PAID -> WooPosTestTags.MARK_ORDER_AS_PAID_PAYMENT_BUTTON
}

internal fun buildAllPaymentMethods(
    readerStatus: WooPosTotalsViewState.ReaderStatus,
    isTapToPayAvailable: Boolean,
    isScanToPayEnabled: Boolean,
    isMarkOrderAsCompleteEnabled: Boolean,
): List<WooPosPaymentMethod> = buildList {
    val readerConnected = readerStatus.isReaderConnected()
    if (!readerConnected && isTapToPayAvailable) add(WooPosPaymentMethod.CARD_READER)
    if (readerConnected && isTapToPayAvailable) add(WooPosPaymentMethod.TAP_TO_PAY)
    if (isScanToPayEnabled) add(WooPosPaymentMethod.SCAN_TO_PAY)
    if (isMarkOrderAsCompleteEnabled) add(WooPosPaymentMethod.MARK_ORDER_AS_PAID)
}

private fun WooPosTotalsViewState.ReaderStatus.isReaderConnected(): Boolean = when (this) {
    is WooPosTotalsViewState.ReaderStatus.Preparing,
    is WooPosTotalsViewState.ReaderStatus.CheckingOrder,
    is WooPosTotalsViewState.ReaderStatus.ReadyForPayment -> true
    is WooPosTotalsViewState.ReaderStatus.Disconnected,
    WooPosTotalsViewState.ReaderStatus.Unavailable -> false
}
