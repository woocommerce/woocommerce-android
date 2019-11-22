package com.woocommerce.android.ui.orders

import android.content.Context
import androidx.paging.PagedList
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.helpers.TEST_DISPATCHER
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.list.MockedOrderListViewModel
import com.woocommerce.android.ui.orders.list.OrderListFragment
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.InternalCoroutinesApi
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore

@Module
abstract class MockedOrderListModule {
    @Module
    companion object {
        private var orders: PagedList<OrderListItemUIType>? = null
        private var orderStatusOptions: Map<String, WCOrderStatusModel>? = null
        private val mockDispatcher = mock<Dispatcher>()

        fun setMockedOrders(ordersList: PagedList<OrderListItemUIType>) {
            this.orders = ordersList
        }

        fun setMockedOrderStatusList(orderStatusOptions: Map<String, WCOrderStatusModel>) {
            this.orderStatusOptions = orderStatusOptions
        }

        @UseExperimental(InternalCoroutinesApi::class)
        @JvmStatic
        @ActivityScope
        @Provides
        fun provideOrderListViewModel(
            site: SelectedSite,
            networkStatus: NetworkStatus,
            listStore: ListStore
        ): OrderListViewModel {
            val mockContext = mock<Context>()
            val orderStore = WCOrderStore(
                    mockDispatcher, OrderRestClient(mockContext, mockDispatcher, mock(), mock(), mock()))
            val gatewayStore = mock<WCGatewayStore>()
            val repository = spy(OrderListRepository(mockDispatcher, orderStore, gatewayStore, site))

            return spy(MockedOrderListViewModel(
                    mainDispatcher = TEST_DISPATCHER,
                    bgDispatcher = TEST_DISPATCHER,
                    repository = repository,
                    orderStore = orderStore,
                    listStore = listStore,
                    networkStatus = networkStatus,
                    dispatcher = mockDispatcher,
                    selectedSite = site
            )).apply {
                this.testOrderData = orders
            }
        }
    }

    @ContributesAndroidInjector
    abstract fun orderListFragment(): OrderListFragment
}
