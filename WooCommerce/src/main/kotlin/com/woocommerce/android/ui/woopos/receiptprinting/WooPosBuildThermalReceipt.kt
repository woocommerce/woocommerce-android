package com.woocommerce.android.ui.woopos.receiptprinting

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardDetails
import com.woocommerce.android.extensions.formatToLocalizedMediumWithTime
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.stripHtml
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.toCardBrandDisplayName
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsRepository
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.LabeledText
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Row
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosReceiptDataResult
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsReceiptRepository
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsStoreRepository
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import java.math.BigDecimal
import javax.inject.Inject

class WooPosBuildThermalReceipt @Inject constructor(
    private val selectedSite: SelectedSite,
    private val totalsRepository: WooPosTotalsRepository,
    private val receiptRepository: WooPosSettingsReceiptRepository,
    private val storeRepository: WooPosSettingsStoreRepository,
    private val formatPrice: WooPosFormatPrice,
    private val resourceProvider: ResourceProvider,
) {
    suspend operator fun invoke(orderId: Long, cardDetails: CardDetails?): WooPosThermalReceipt? {
        val site = selectedSite.get()
        val order = totalsRepository.getOrderById(orderId, site) ?: return null
        val receiptInfo = receiptRepository.getCachedReceiptInfo(site)
            ?: (receiptRepository.getReceiptInfo(site) as? WooPosReceiptDataResult.Success)?.receiptInfo
        val storeInfo = storeRepository.getStoreInfo(site)
        return WooPosThermalReceipt(
            storeName = receiptInfo?.storeName.nullIfBlank() ?: storeInfo.storeName,
            storeAddress = (receiptInfo?.address.nullIfBlank() ?: storeInfo.address).nullIfBlank(),
            storePhone = receiptInfo?.phone.nullIfBlank(),
            storeEmail = receiptInfo?.email.nullIfBlank(),
            datePaid = LabeledText(
                label = resourceProvider.getString(R.string.woopos_receipt_date_paid),
                text = (order.datePaid ?: order.dateCreated).formatToLocalizedMediumWithTime(),
            ),
            orderNumber = resourceProvider.getString(R.string.woopos_receipt_order_number, order.number),
            paymentMethod = cardDetails?.let(::paymentMethod),
            lineItems = order.items.map { lineItem(it, order.currency) },
            totals = totals(order),
            orderNote = order.customerNote.stripHtml().trim().ifEmpty { null }?.let {
                LabeledText(label = resourceProvider.getString(R.string.woopos_receipt_notes), text = it)
            },
            refundPolicy = receiptInfo?.refundPolicy.nullIfBlank(),
        )
    }

    private fun paymentMethod(cardDetails: CardDetails): LabeledText {
        val brand = cardDetails.brand.toCardBrandDisplayName()
        return LabeledText(
            label = resourceProvider.getString(R.string.woopos_receipt_payment_method),
            text = if (brand.isEmpty()) {
                cardDetails.last4
            } else {
                resourceProvider.getString(R.string.woopos_receipt_card_brand_and_last4, brand, cardDetails.last4)
            },
        )
    }

    private fun lineItem(item: Order.Item, currency: String): Row {
        val attributes = item.displayableAttributes.joinToString { "${it.key} ${it.value}" }
        val title = if (attributes.isEmpty()) {
            item.name
        } else {
            resourceProvider.getString(R.string.woopos_receipt_item_with_attributes, item.name, attributes)
        }
        return Row(
            label = resourceProvider.getString(
                R.string.woopos_receipt_item_and_quantity,
                title,
                item.quantity.formatToString()
            ),
            amount = formatPrice(item.subtotal, currency),
        )
    }

    private fun totals(order: Order): List<Row> = listOfNotNull(
        order.totalRow(R.string.woopos_receipt_subtotal, order.productsTotal),
        order.discountRow(),
    ) + order.feeRows() + listOfNotNull(
        order.totalRowIfNotZero(R.string.woopos_receipt_shipping, order.shippingTotal),
        order.totalRowIfNotZero(R.string.woopos_receipt_taxes, order.totalTax),
        order.totalRow(R.string.woopos_receipt_amount_paid, order.total),
    )

    private fun Order.feeRows(): List<Row> = feesLines
        .filter { it.total.signum() != 0 }
        .map { fee ->
            Row(
                label = fee.name?.trim().nullIfBlank()
                    ?: resourceProvider.getString(R.string.woopos_custom_amount_dialog_name_placeholder),
                amount = formatPrice(fee.total, currency),
            )
        }

    private fun Order.discountRow(): Row? {
        if (discountTotal.signum() == 0 && discountCodes.isEmpty()) return null
        val label = if (discountCodes.isEmpty()) {
            resourceProvider.getString(R.string.woopos_receipt_discount)
        } else {
            resourceProvider.getString(R.string.woopos_receipt_discount_with_coupons, discountCodes)
        }
        return Row(label = label, amount = formatPrice(discountTotal.negate(), currency))
    }

    private fun Order.totalRowIfNotZero(@StringRes label: Int, amount: BigDecimal): Row? =
        if (amount.signum() == 0) null else totalRow(label, amount)

    private fun Order.totalRow(@StringRes label: Int, amount: BigDecimal) =
        Row(label = resourceProvider.getString(label), amount = formatPrice(amount, currency))

    private fun String?.nullIfBlank(): String? = this?.ifBlank { null }
}
