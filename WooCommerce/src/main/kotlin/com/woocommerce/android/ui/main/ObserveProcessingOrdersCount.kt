package com.woocommerce.android.ui.main

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import com.woocommerce.android.util.observeEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import javax.inject.Inject

class ObserveProcessingOrdersCount @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wcOrderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val dispatchers: CoroutineDispatchers
) {
    companion object {
        // A debounce duration to avoid fetching the value multiple times when there are multiple simultaneous events
        private const val DEBOUNCE_DURATION_MS = 200L
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    operator fun invoke(): Flow<Int?> = selectedSite.observe().transformLatest { site ->
        if (site == null) {
            emit(null)
            return@transformLatest
        }

        // Start with the cached value
        emit(getCachedValue(site))

        // Observe value changes
        coroutineScope {
            merge(
                wcOrderStore.observeOrderCountForSite(site)
                    .distinctUntilChanged(),
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
                .debounce(DEBOUNCE_DURATION_MS)
                .onEach {
                    // Fetch value from API, the value will be emitted when OnOrderStatusOptionsChanged event
                    // is received below
                    fetchOrderStatusOptions(site)
                }
                .launchIn(this)

            emitAll(
                dispatcher.observeEvents<OnOrderStatusOptionsChanged>()
                    .map { getCachedValue(site) }
            )
        }
    }

    private suspend fun getCachedValue(site: SiteModel): Int? = withContext(dispatchers.io) {
        wcOrderStore.getOrderStatusForSiteAndKey(site, CoreOrderStatus.PROCESSING.value)?.statusCount
    }

    private suspend fun fetchOrderStatusOptions(site: SiteModel): Result<Unit> {
        val event: OnOrderStatusOptionsChanged = dispatcher.dispatchAndAwait(
            WCOrderActionBuilder.newFetchOrderStatusOptionsAction(
                FetchOrderStatusOptionsPayload(site)
            )
        )

        return when {
            event.isError -> {
                WooLog.e(
                    WooLog.T.ORDERS,
                    "Error fetching order status options: ${event.error.message}"
                )
                Result.failure(OnChangedException(event.error))
            }

            else -> Result.success(Unit)
        }
    }
}
