package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.model.BundleStat
import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat

sealed class AnalyticsHubUpdateState {
    object Finished : AnalyticsHubUpdateState()
    object Loading : AnalyticsHubUpdateState()
}

sealed class OrdersState {
    data class Available(val orders: OrdersStat) : OrdersState()
    object Loading : OrdersState()
    object Error : OrdersState()

    val isIdle get() = this !is Loading
}

sealed class SessionState {
    data class Available(val session: SessionStat) : SessionState()
    object Loading : SessionState()
    object Error : SessionState()

    val isIdle get() = this !is Loading
}

sealed class RevenueState {
    data class Available(val revenue: RevenueStat) : RevenueState()
    object Loading : RevenueState()
    object Error : RevenueState()

    val isIdle get() = this !is Loading
}

sealed class ProductsState {
    data class Available(val products: ProductsStat) : ProductsState()
    object Loading : ProductsState()
    object Error : ProductsState()

    val isIdle get() = this !is Loading
}

sealed class VisitorsState {
    data class Available(val visitors: Int) : VisitorsState()
    object Loading : VisitorsState()
    object Error : VisitorsState()
}

sealed class BundlesState {
    data class Available(val bundles: BundleStat) : BundlesState()
    data object Loading : BundlesState()
    data object Error : BundlesState()
    val isIdle get() = this !is Loading
}
