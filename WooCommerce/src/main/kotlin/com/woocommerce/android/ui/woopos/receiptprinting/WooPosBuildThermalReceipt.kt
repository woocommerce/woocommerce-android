package com.woocommerce.android.ui.woopos.receiptprinting

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.formatToLocalizedMediumWithTime
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsRepository
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Labels
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Row
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosReceiptDataResult
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsReceiptRepository
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsStoreRepository
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsStoreState.ReceiptInfo
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
    suspend operator fun invoke(orderId: Long): WooPosThermalReceipt? {
        val siteId = selectedSite.getSelectedSiteId()
        val order = totalsRepository.getOrderById(orderId) ?: return null
        val receiptInfo = getReceiptInfo()
        val storeInfo = storeRepository.getStoreInfo()
        if (selectedSite.getSelectedSiteId() != siteId) return null
        return WooPosThermalReceipt(
            storeName = receiptInfo?.storeName.nullIfBlank() ?: storeInfo.storeName,
            storeAddress = (receiptInfo?.address.nullIfBlank() ?: storeInfo.address).nullIfBlank(),
            storePhone = receiptInfo?.phone.nullIfBlank(),
            storeEmail = receiptInfo?.email.nullIfBlank(),
            datePaid = (order.datePaid ?: order.dateCreated).formatToLocalizedMediumWithTime(),
            orderNumber = resourceProvider.getString(R.string.woopos_receipt_order_number, order.number),
            paymentMethod = null,
            lineItems = order.items.map { lineItem(it, order.currency) },
            totals = totals(order),
            orderNote = order.customerNote.toReceiptNote(),
            refundPolicy = receiptInfo?.refundPolicy.nullIfBlank(),
        )
    }

    fun labels() = Labels(
        datePaid = resourceProvider.getString(R.string.woopos_receipt_date_paid),
        paymentMethod = resourceProvider.getString(R.string.woopos_receipt_payment_method),
        notes = resourceProvider.getString(R.string.woopos_receipt_notes),
    )

    private suspend fun getReceiptInfo(): ReceiptInfo? =
        (receiptRepository.getReceiptInfo() as? WooPosReceiptDataResult.Success)?.receiptInfo
            ?: receiptRepository.getCachedReceiptInfo()

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
        order.totalRowIfNotZero(R.string.woopos_receipt_fees, order.feesTotal),
        order.totalRowIfNotZero(R.string.woopos_receipt_shipping, order.shippingTotal),
        order.totalRowIfNotZero(R.string.woopos_receipt_taxes, order.totalTax),
        order.totalRow(R.string.woopos_receipt_amount_paid, order.total),
    )

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

    // fastStripHtml returns the markup itself when stripping leaves nothing, and keeps leading whitespace.
    // Only a note set through the API can be made of tags alone, so the receipt drops such a note and
    // trims the rest here, rather than changing the shared helper for every store screen that uses it.
    private fun String.toReceiptNote(): String? =
        fastStripHtml().trim().takeUnless { it.replace(HTML_TAG, "").isEmpty() }

    private fun String?.nullIfBlank(): String? = this?.ifBlank { null }

    private companion object {
        val HTML_TAG = Regex("<(.|\n)*?>")
    }
}
