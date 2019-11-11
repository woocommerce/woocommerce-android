package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.nhaarman.mockitokotlin2.mock
import com.woocommerce.android.di.BG_THREAD
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.helpers.mockPagedList
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.WcOrderTestUtils
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject
import javax.inject.Named

class MockedOrderListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    repository: OrderListRepository,
    orderStore: WCOrderStore,
    listStore: ListStore,
    networkStatus: NetworkStatus,
    dispatcher: Dispatcher,
    selectedSite: SelectedSite
) : OrderListViewModel(
        mainDispatcher,
        bgDispatcher,
        repository,
        orderStore,
        listStore,
        networkStatus,
        dispatcher,
        selectedSite
) {
    override fun getLifecycle(): Lifecycle = mock()

    override val lifecycleRegistry: LifecycleRegistry
        get() = mock()

    /**
     * Set the data that will be emitted during tests before the UI calls [loadList]
     */
    var testOrderData: PagedOrdersList? = mockPagedList(WcOrderTestUtils.generateOrderListUIItems())

    override fun start() {
        // DO NOTHING
    }

    override fun loadList(
        statusFilter: String?,
        searchQuery: String?,
        excludeFutureOrders: Boolean
    ) {
        _pagedListData.value = testOrderData
    }

    override fun reloadListFromCache() {
        _pagedListData.value = testOrderData
    }
}
