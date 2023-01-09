package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.AnalyticsViewModel.OrdersState
import com.woocommerce.android.ui.analytics.AnalyticsViewModel.VisitorsState
import java.text.DecimalFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class UpdateSessionStats {
    private lateinit var ordersState: MutableStateFlow<OrdersState>
    private lateinit var visitorsCountState: MutableStateFlow<Int>

    operator fun invoke(

    ) : Flow<VisitorsState> {
        ordersState = MutableStateFlow(OrdersState.Loading)
        visitorsCountState = MutableStateFlow(0)
        return observeSessionChanges()
    }

    private fun observeSessionChanges() =
        combine(ordersState, visitorsCountState) { orders, visitorsCount ->
            orders.run { this as? OrdersState.Available }
                ?.orders?.ordersCount
                ?.let { (it / visitorsCount.toFloat()) * 100 }
                ?.let { DecimalFormat("##.#").format(it) + "%" }
                ?.let { SessionStat(it, visitorsCount) }
                ?.let { VisitorsState.Available(it) }
                ?: when (orders) {
                    is OrdersState.Error -> VisitorsState.Error
                    else -> VisitorsState.Loading
                }
        }
}
