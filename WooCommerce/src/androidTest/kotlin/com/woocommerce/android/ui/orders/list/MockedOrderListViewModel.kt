package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.nhaarman.mockitokotlin2.mock
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.helpers.mockPagedList
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.WcOrderTestUtils
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore

class MockedOrderListViewModel @AssistedInject constructor(
    dispatchers: CoroutineDispatchers,
    repository: OrderListRepository,
    orderStore: WCOrderStore,
    listStore: ListStore,
    networkStatus: NetworkStatus,
    dispatcher: Dispatcher,
    selectedSite: SelectedSite,
    fetcher: OrderFetcher,
    resourceProvider: ResourceProvider,
    @Assisted arg0: SavedStateWithArgs
) : OrderListViewModel(
        arg0,
        dispatchers,
        repository,
        orderStore,
        listStore,
        networkStatus,
        dispatcher,
        selectedSite,
        fetcher,
        resourceProvider
) {
    override fun getLifecycle(): Lifecycle = mock()

    override val lifecycleRegistry: LifecycleRegistry
        get() = mock()

    /**
     * Set the data that will be emitted during tests before the UI calls attempts to load lists
     */
    var testOrderData: PagedOrdersList? = mockPagedList(WcOrderTestUtils.generateOrderListUIItems())
    var testOrderStatusData: Map<String, WCOrderStatusModel>? = WcOrderTestUtils.generateOrderStatusOptions()

    override fun loadAllList() {
        _orderStatusOptions.value = testOrderStatusData
        _pagedListData.value = testOrderData
    }

    override fun loadProcessingList() {
        _orderStatusOptions.value = testOrderStatusData
        _pagedListData.value = testOrderData
    }

    override fun submitSearchOrFilter(statusFilter: String?, searchQuery: String?) {
        _orderStatusOptions.value = testOrderStatusData
        _pagedListData.value = testOrderData
    }

    override fun reloadListFromCache() {
        _pagedListData.value = testOrderData
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<MockedOrderListViewModel>
}
