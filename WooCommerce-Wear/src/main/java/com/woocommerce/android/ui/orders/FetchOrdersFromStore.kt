package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.orders.OrdersListViewModel.OrderItem
import com.woocommerce.android.util.DateUtils
import java.util.Locale
import javax.inject.Inject
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore.OrdersForWearablesResult.Success
import org.wordpress.android.fluxc.store.WooCommerceStore

class FetchOrdersFromStore @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val wooCommerceStore: WooCommerceStore,
    private val dateUtils: DateUtils,
    private val locale: Locale
) {
    suspend operator fun invoke(
        selectedSite: SiteModel
    ) = when (val result = ordersRepository.fetchOrders(selectedSite)) {
        is Success -> {
            result.orders.map {
                OrderItem(
                    date = it.formattedCreationDate,
                    number = it.number,
                    customerName = it.billingName,
                    total = it.formattedCurrencyTotal(selectedSite),
                    status = it.capitalizedStatus
                )
            }
        }
        else -> emptyList()
    }

    private fun OrderEntity.formattedCurrencyTotal(site: SiteModel): String {
        return wooCommerceStore.formatCurrencyForDisplay(
            amount = total.toDoubleOrNull() ?: 0.0,
            site = site,
            currencyCode = null,
            applyDecimalFormatting = true
        )
    }

    private val OrderEntity.formattedCreationDate
        get() = dateUtils.getFormattedDateWithSiteTimeZone(dateCreated)
            ?: dateCreated

    private val OrderEntity.billingName: String?
        get() {
            if (billingFirstName.isEmpty() && billingLastName.isEmpty()) {
                return null
            }
            return "$billingFirstName $billingLastName"
        }

    private val OrderEntity.capitalizedStatus
        get() = status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
}
