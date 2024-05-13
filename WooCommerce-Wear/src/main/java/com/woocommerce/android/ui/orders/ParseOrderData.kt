package com.woocommerce.android.ui.orders

import android.os.Parcelable
import com.woocommerce.android.util.DateUtils
import java.util.Locale
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

class ParseOrderData @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val dateUtils: DateUtils,
    private val locale: Locale,
) {
    operator fun invoke(
        selectedSite: SiteModel,
        order: OrderEntity
    ) = order.toOrderItem(selectedSite)

    operator fun invoke(
        selectedSite: SiteModel,
        orders: List<OrderEntity>
    ) = orders.map { it.toOrderItem(selectedSite) }

    private fun OrderEntity.toOrderItem(
        selectedSite: SiteModel
    ): OrderItem {
        val formattedOrderTotals = wooCommerceStore.formatCurrencyForDisplay(
            amount = total.toDoubleOrNull() ?: 0.0,
            site = selectedSite,
            currencyCode = null,
            applyDecimalFormatting = true
        )

        val formattedCreationDate = dateUtils.getFormattedDateWithSiteTimeZone(
            dateCreated
        ) ?: dateCreated

        val formattedBillingName = takeUnless {
            billingFirstName.isEmpty() && billingLastName.isEmpty()
        }?.let { "$billingFirstName $billingLastName" }

        val formattedStatus = status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }

        return OrderItem(
            id = orderId,
            date = formattedCreationDate,
            number = number,
            customerName = formattedBillingName,
            total = formattedOrderTotals,
            status = formattedStatus
        )
    }

    @Parcelize
    data class OrderItem(
        val id: Long,
        val date: String,
        val number: String,
        val customerName: String?,
        val total: String,
        val status: String
    ) : Parcelable
}
