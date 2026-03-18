package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.ciab.CIABOrderStatusMapper
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.ui.orders.list.OrderListRepository
import javax.inject.Inject

class GetOrderStatusFilterOptions @Inject constructor(
    private val orderListRepository: OrderListRepository,
    private val orderFiltersRepository: OrderFiltersRepository,
    private val ciabOrderStatusMapper: CIABOrderStatusMapper
) {
    suspend operator fun invoke(): List<OrderStatusOption> {
        var orderStatus = orderListRepository.getCachedOrderStatusOptions()
        if (orderStatus.isEmpty()) {
            when (orderListRepository.fetchOrderStatusOptionsFromApi()) {
                RequestResult.SUCCESS -> orderStatus = orderListRepository.getCachedOrderStatusOptions()
                else -> {
                    /* do nothing */
                }
            }
        }
        val selectedFilterKeys = ciabOrderStatusMapper.resolveFilterKeys(
            orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.ORDER_STATUS)
        )

        val options = orderStatus.values
            .toList()
            .map {
                OrderStatusOption(
                    key = it.statusKey,
                    label = it.label,
                    statusCount = it.statusCount,
                    isSelected = it.statusKey in selectedFilterKeys
                )
            }
        return ciabOrderStatusMapper.mapFilterOptions(options)
    }
}
