package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import javax.inject.Inject

class GetSelectedOrderFiltersCount @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository
) {
    operator fun invoke(): Int =
        orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.ORDER_STATUS)
            .plus(orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.DATE_RANGE))
            .size
}
