package com.woocommerce.android.util.receipts

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.receipts.ReceiptData
import com.woocommerce.android.cardreader.receipts.ReceiptLineItem
import com.woocommerce.android.cardreader.receipts.ReceiptPaymentInfo
import com.woocommerce.android.cardreader.receipts.ReceiptStaticTexts
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class ReceiptDataMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite
) {
    fun mapToReceiptData(order: WCOrderModel, receiptPaymentInfo: ReceiptPaymentInfo) = ReceiptData(
        staticTexts = getStaticTexts(),
        purchasedProducts = createReceiptLineItems(order),
        storeName = selectedSite.get().displayName,
        receiptPaymentInfo = receiptPaymentInfo
    )

    private fun getStaticTexts() = ReceiptStaticTexts(
        applicationName = resourceProvider.getString(R.string.card_reader_receipt_application_name_title),
        receiptFromFormat = resourceProvider.getString(R.string.card_reader_receipt_from_title),
        receiptTitle = resourceProvider.getString(R.string.card_reader_receipt_title),
        amountPaidSectionTitle = resourceProvider.getString(R.string.card_reader_receipt_amount_paid_title),
        datePaidSectionTitle = resourceProvider.getString(R.string.card_reader_receipt_date_paid_title),
        paymentMethodSectionTitle = resourceProvider.getString(R.string.card_reader_receipt_payment_method_title),
        summarySectionTitle = resourceProvider.getString(R.string.card_reader_receipt_summary_title),
        aid = resourceProvider.getString(R.string.card_reader_receipt_aid_title)
    )

    private fun createReceiptLineItems(order: WCOrderModel): List<ReceiptLineItem> =
        getProducts(order) + getDiscounts(order) + getFees(order) + getShipping(order) + getTaxes(order)

    private fun getProducts(order: WCOrderModel) = order.getLineItemList()
        .mapNotNull { line ->
            line.total?.toBigDecimalOrNull()?.roundError()?.let { total ->
                ReceiptLineItem(
                    title = line.name.orEmpty(),
                    quantity = line.quantity ?: 1f,
                    itemsTotalAmount = total
                )
            }
        }

    private fun getDiscounts(order: WCOrderModel) =
        order.discountTotal.toBigDecimalOrNull()?.roundError()?.let { total ->
            listOf(
                ReceiptLineItem(
                    title = resourceProvider.getString(R.string.discount),
                    quantity = 1f,
                    itemsTotalAmount = total
                )
            )
        } ?: listOf()

    private fun getFees(order: WCOrderModel) = order.getFeeLineList()
        .mapNotNull { line ->
            line.total?.toBigDecimalOrNull()?.roundError()?.let { total ->
                ReceiptLineItem(
                    title = line.name.orEmpty(),
                    quantity = 1f,
                    itemsTotalAmount = total
                )
            }
        }

    private fun getShipping(order: WCOrderModel) = order.getShippingLineList()
        .mapNotNull { line ->
            line.total?.toBigDecimalOrNull()?.roundError()?.let { total ->
                ReceiptLineItem(
                    title = line.methodTitle.orEmpty(),
                    quantity = 1f,
                    itemsTotalAmount = total
                )
            }
        }

    private fun getTaxes(order: WCOrderModel) =
        order.totalTax.toBigDecimalOrNull()?.roundError()?.let { total ->
            listOf(
                ReceiptLineItem(
                    title = resourceProvider.getString(R.string.taxes),
                    quantity = 1f,
                    itemsTotalAmount = total
                )
            )
        } ?: listOf()
}
