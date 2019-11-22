package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import androidx.savedstate.SavedStateRegistryOwner
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.orders.list.MockedOrderListViewModel
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers.Unconfined
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
            val mockSavedState: SavedStateWithArgs = mock()
            val testDispatchers = CoroutineDispatchers(Unconfined, Unconfined, Unconfined)

            return spy(MockedOrderListViewModel(
                    dispatchers = testDispatchers,
                    repository = repository,
                    orderStore = orderStore,
                    listStore = listStore,
                    networkStatus = networkStatus,
                    dispatcher = mockDispatcher,
                    selectedSite = site,
                    arg0 = mockSavedState
            )).apply {
                this.testOrderData = orders
            }
        }

        @JvmStatic
        @Provides
        fun provideDefaultArgs(): Bundle? {
            return null
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(MockedOrderListViewModel::class)
    abstract fun bindFactory(factory: MockedOrderListViewModel.Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(activity: MainActivity): SavedStateRegistryOwner
}
