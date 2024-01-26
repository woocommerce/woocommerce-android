package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class CardReaderPaymentOrderHelper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite,
    private val currencyFormatter: CurrencyFormatter,
) {
    fun getPaymentDescription(order: Order): String =
        resourceProvider.getString(
            R.string.card_reader_payment_description_v2,
            order.number,
            selectedSite.get().name.orEmpty(),
            selectedSite.get().remoteId().value
        )

    fun getAmountLabel(order: Order): String = currencyFormatter
        .formatAmountWithCurrency(order.total.toDouble(), order.currency)

    fun getReceiptDocumentName(orderId: Long) = "receipt-order-$orderId"
}
