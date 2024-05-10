package com.woocommerce.android.ui.orders

import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.system.NetworkStatus
import com.woocommerce.android.ui.orders.OrdersListViewModel.OrderItem
import com.woocommerce.android.util.DateUtils
import com.woocommerce.commons.wear.MessagePath.REQUEST_ORDERS
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore.OrdersForWearablesResult.Success
import org.wordpress.android.fluxc.store.WooCommerceStore

class FetchOrders @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val ordersRepository: OrdersRepository,
    private val wooCommerceStore: WooCommerceStore,
    private val dateUtils: DateUtils,
    private val networkStatus: NetworkStatus,
    private val locale: Locale
) {
    suspend operator fun invoke(
        selectedSite: SiteModel
    ): Flow<List<OrderItem>> = flow {
        if (networkStatus.isConnected()) {
            emit(fetchOrdersFromStore(selectedSite))
        } else {
            phoneRepository.sendMessage(REQUEST_ORDERS)

        }
    }

    private suspend fun fetchOrdersFromStore(selectedSite: SiteModel) =
        when (val result = ordersRepository.fetchOrders(selectedSite)) {
            is Success -> result.orders.map { it.toOrderItem(selectedSite) }
            else -> emptyList()
        }

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

        val formattedBillingName = takeIf {
            billingFirstName.isEmpty() && billingLastName.isEmpty()
        }?.let { "$billingFirstName $billingLastName" }

        val formattedStatus = status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }

        return OrderItem(
            date = formattedCreationDate,
            number = number,
            customerName = formattedBillingName,
            total = formattedOrderTotals,
            status = formattedStatus
        )
    }
}
