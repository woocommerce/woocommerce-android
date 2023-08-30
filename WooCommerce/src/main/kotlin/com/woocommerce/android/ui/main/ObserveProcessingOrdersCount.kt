package com.woocommerce.android.ui.main

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.observeEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrdersCountResult.Failure
import org.wordpress.android.fluxc.store.WCOrderStore.OrdersCountResult.Success
import javax.inject.Inject

class ObserveProcessingOrdersCount @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wcOrderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Int?> = selectedSite.observe().transformLatest { site ->
        if (site == null) {
            emit(null)
            return@transformLatest
        }

        // Start with the cached value
        emit(getCachedValue(site))

        // emit updated value
        fetchOrdersCount(site)?.let { emit(it) }

        // Observe value changes
        coroutineScope {
            dispatcher.observeEvents<OnOrderStatusOptionsChanged>()
                .onEach {
                    emit(getCachedValue(site))
                }
                .launchIn(this)

            merge(
                dispatcher.observeEvents<OnOrderChanged>()
                    .filter {
                        @Suppress("DEPRECATION")
                        it.causeOfChange == WCOrderAction.UPDATE_ORDER_STATUS
                    }
                    .onEach {
                        WooLog.d(WooLog.T.ORDERS, "Order status changed, re-check unfilled orders count")
                    },
                EventBus.getDefault().observeEvents<NotificationReceivedEvent>()
                    .filter {
                        it.siteId == site.siteId && it.channel == NotificationChannelType.NEW_ORDER
                    }
                    .onEach {
                        WooLog.d(WooLog.T.ORDERS, "New order notification received, re-check unfilled orders count")
                    }
            )
                .onEach {
                    fetchOrdersCount(site)?.let { emit(it) }
                }
                .launchIn(this)
        }
    }

    private fun getCachedValue(site: SiteModel): Int? =
        wcOrderStore.getOrderStatusForSiteAndKey(site, CoreOrderStatus.PROCESSING.value)?.statusCount

    private suspend fun fetchOrdersCount(site: SiteModel): Int? {
        return wcOrderStore.fetchOrdersCount(site, CoreOrderStatus.PROCESSING.value).let {
            when (it) {
                is Success -> {
                    AnalyticsTracker.track(
                        AnalyticsEvent.UNFULFILLED_ORDERS_LOADED,
                        mapOf(AnalyticsTracker.KEY_HAS_UNFULFILLED_ORDERS to it.count)
                    )
                    it.count
                }

                is Failure -> {
                    WooLog.e(
                        WooLog.T.ORDERS,
                        "Error fetching a count of orders waiting to be fulfilled: ${it.error.message}"
                    )
                    null
                }
            }
        }
    }
}
