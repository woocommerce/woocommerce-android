package com.woocommerce.android.ciab

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.R
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import javax.inject.Inject

class CIABOrderStatusMapper @Inject constructor(
    private val ciabSiteGateKeeper: CIABSiteGateKeeper,
    private val resourceProvider: ResourceProvider
) {
    fun mapOrderStatusOptionsList(
        statusMap: Map<String, WCOrderStatusModel>
    ): Map<String, WCOrderStatusModel> {
        if (!ciabSiteGateKeeper.isCurrentSiteCIAB()) return statusMap

        val openLabel = resourceProvider.getString(R.string.ciab_order_status_open)
        return statusMap.mapValues { (key, model) ->
            if (key in OPEN_CORE_KEYS) {
                model.copy(statusKey = OPEN_KEY, label = openLabel)
            } else {
                model
            }
        }
    }

    fun mapOrderStatus(orderStatus: OrderStatus): OrderStatus {
        if (!ciabSiteGateKeeper.isCurrentSiteCIAB()) return orderStatus

        return if (orderStatus.statusKey in OPEN_CORE_KEYS) {
            OrderStatus(
                statusKey = OPEN_KEY,
                label = resourceProvider.getString(R.string.ciab_order_status_open)
            )
        } else {
            orderStatus
        }
    }

    fun mapFilterOptions(options: List<OrderStatusOption>): List<OrderStatusOption> {
        if (!ciabSiteGateKeeper.isCurrentSiteCIAB()) return options

        val openOptions = options.filter { it.key in OPEN_CORE_KEYS }
        val otherOptions = options.filter { it.key !in OPEN_CORE_KEYS && it.key !in HIDDEN_KEYS }

        if (openOptions.isEmpty()) return otherOptions

        val openLabel = resourceProvider.getString(R.string.ciab_order_status_open)
        val groupedOpen = OrderStatusOption(
            key = OPEN_KEY,
            label = openLabel,
            statusCount = openOptions.sumOf { it.statusCount },
            isSelected = openOptions.any { it.isSelected }
        )

        return listOf(groupedOpen) + otherOptions
    }

    fun resolveFilterKeys(selectedKeys: List<String>): List<String> {
        if (!ciabSiteGateKeeper.isCurrentSiteCIAB()) return selectedKeys

        return selectedKeys.flatMap { key ->
            if (key == OPEN_KEY) OPEN_CORE_KEYS.toList() else listOf(key)
        }
    }

    companion object {
        const val OPEN_KEY = "open"
        private const val CHECKOUT_DRAFT_KEY = "checkout-draft"

        @VisibleForTesting
        val OPEN_CORE_KEYS = setOf(
            CoreOrderStatus.PENDING.value,
            CoreOrderStatus.PROCESSING.value,
            CoreOrderStatus.ON_HOLD.value,
            CoreOrderStatus.FAILED.value
        )

        @VisibleForTesting
        val HIDDEN_KEYS = setOf(CHECKOUT_DRAFT_KEY)
    }
}
