package com.woocommerce.android.ui.orders

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.helpers.TEST_DISPATCHER
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.list.OrderListFragment
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore

@Module
abstract class MockedOrderListModule {
    @Module
    companion object {
        private var orders: List<WCOrderModel>? = null
        private var orderStatusOptions: Map<String, WCOrderStatusModel>? = null

        fun setMockedOrders(ordersList: List<WCOrderModel>) {
            this.orders = ordersList
        }

        fun setMockedOrderStatusList(orderStatusOptions: Map<String, WCOrderStatusModel>) {
            this.orderStatusOptions = orderStatusOptions
        }

        @UseExperimental(InternalCoroutinesApi::class)
        @JvmStatic
        @ActivityScope
        @Provides
        fun provideOrderListViewModel(site:SelectedSite, networkStatus: NetworkStatus): OrderListViewModel {
            val mockDispatcher: Dispatcher = mock()
            val orderStore: WCOrderStore = mock()

            val pagedListWrapper = mock<PagedListWrapper<OrderListItemUIType>> {
                on { listError } doReturn mock()
                on { isEmpty } doReturn mock()
                on { isFetchingFirstPage } doReturn mock()
                on { isLoadingMore } doReturn mock()
                on { data } doReturn mock()
            }

            val listStore = mock<ListStore> {
                on {
                    getList<WCOrderListDescriptor, OrderListItemIdentifier, OrderListItemUIType>(any(), any(), any())
                } doReturn pagedListWrapper
            }

            val repository = mock<OrderListRepository> {
                on { runBlocking { it.getCachedOrderStatusOptions() } } doReturn orderStatusOptions.orEmpty()
            }

            return spy(OrderListViewModel(
                    mainDispatcher = TEST_DISPATCHER,
                    bgDispatcher = TEST_DISPATCHER,
                    repository = repository,
                    orderStore = orderStore,
                    listStore = listStore,
                    networkStatus = networkStatus,
                    dispatcher = mockDispatcher,
                    selectedSite = site
            ))
        }
    }

    @ContributesAndroidInjector
    abstract fun orderListFragment(): OrderListFragment
}
