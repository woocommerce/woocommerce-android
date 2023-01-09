package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.AnalyticsViewModel.OrdersState
import com.woocommerce.android.ui.analytics.AnalyticsViewModel.VisitorsState
import java.text.DecimalFormat
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class UpdateSessionStats @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    private val ordersState = MutableStateFlow(OrdersState.Available(OrdersStat.EMPTY) as OrdersState)
    private val visitorsCountState = MutableStateFlow(0)

    private val sessionChanges: Flow<VisitorsState>

    init {
        sessionChanges = combine(ordersState, visitorsCountState) { orders, visitorsCount ->
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

    operator fun invoke() : Flow<VisitorsState> {
        ordersState.update { OrdersState.Loading }
        return sessionChanges
    }

}
