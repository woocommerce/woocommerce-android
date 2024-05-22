package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.util.DateUtils
import com.woocommerce.commons.WearOrder
import com.woocommerce.commons.WearOrderedProduct
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Locale
import javax.inject.Inject

class FormatOrderData @Inject constructor(
    private val context: Context,
    private val wooCommerceStore: WooCommerceStore,
    private val dateUtils: DateUtils,
    private val locale: Locale,
) {
    operator fun invoke(
        selectedSite: SiteModel,
        order: WearOrder,
        products: List<WearOrderedProduct>?
    ) = order.toOrderItem(selectedSite, products)

    operator fun invoke(
        selectedSite: SiteModel,
        orders: List<WearOrder>
    ) = orders.map { it.toOrderItem(selectedSite) }

    private fun WearOrder.toOrderItem(
        selectedSite: SiteModel,
        products: List<WearOrderedProduct>? = null
    ): OrderItem {
        val orderProducts = products?.map {
            ProductItem(
                amount = it.amount.toDoubleOrNull()?.toInt() ?: 0,
                total = wooCommerceStore.formatCurrencyForDisplay(
                    amount = it.total.toDoubleOrNull() ?: 0.0,
                    site = selectedSite,
                    currencyCode = null,
                    applyDecimalFormatting = true
                ),
                name = it.name
            )
        }

        val formattedOrderTotals = wooCommerceStore.formatCurrencyForDisplay(
            amount = total.toDoubleOrNull() ?: 0.0,
            site = selectedSite,
            currencyCode = null,
            applyDecimalFormatting = true
        )

        val formattedCreationDate = dateUtils.getFormattedDateWithSiteTimeZone(date) ?: date

        val formattedBillingName = takeUnless {
            billingFirstName.isEmpty() && billingLastName.isEmpty()
        }?.let { "$billingFirstName $billingLastName" } ?: context.getString(R.string.orders_list_guest_customer)

        val formattedStatus = status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }

        val formattedNumber = "#$number"

        return OrderItem(
            id = id,
            date = formattedCreationDate,
            number = formattedNumber,
            customerName = formattedBillingName,
            total = formattedOrderTotals,
            status = formattedStatus,
            products = orderProducts
        )
    }

    @Parcelize
    data class OrderItem(
        val id: Long,
        val date: String,
        val number: String,
        val customerName: String,
        val total: String,
        val status: String,
        val products: List<ProductItem>? = null
    ) : Parcelable

    @Parcelize
    data class ProductItem(
        val amount: Int,
        val total: String,
        val name: String
    ) : Parcelable
}
