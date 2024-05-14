package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.util.DateUtils
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Locale
import javax.inject.Inject

class ParseOrderData @Inject constructor(
    private val context: Context,
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
        }?.let { "$billingFirstName $billingLastName" } ?: context.getString(R.string.orders_list_guest_customer)

        val formattedStatus = status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }

        val formattedNumber = "#$number"

        return OrderItem(
            id = orderId,
            date = formattedCreationDate,
            number = formattedNumber,
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
        val customerName: String,
        val total: String,
        val status: String
    ) : Parcelable
}
