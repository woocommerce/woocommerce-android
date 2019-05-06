package com.woocommerce.android.ui.orders

import android.content.Context
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.store.WCOrderStore

@Module
abstract class MockedOrderListModule {
    @Module
    companion object {
        private var orders: List<WCOrderModel>? = null
        private var orderStatusList: Map<String, WCOrderStatusModel>? = null

        fun setOrders(orders: List<WCOrderModel>) {
            this.orders = orders
        }

        fun setOrderStatusList(orderStatusList: Map<String, WCOrderStatusModel>) {
            this.orderStatusList = orderStatusList
        }

        @JvmStatic
        @ActivityScope
        @Provides
        fun provideOrderListPresenter(): OrderListContract.Presenter {
            /**
             * Creating a spy object here since we need to mock specific methods of [OrderListPresenter] class
             * instead of mocking all the methods in the class.
             * We cannot mock final classes ([WCOrderStore], [SelectedSite] and [NetworkStatus]), so
             * creating a mock instance of those classes and passing to the presenter class constructor.
             */
            val mockDispatcher = mock<Dispatcher>()
            val mockContext = mock<Context>()
            val mockedOrderListPresenter = spy(OrderListPresenter(
                    mockDispatcher,
                    WCOrderStore(mockDispatcher, OrderRestClient(mockContext, mockDispatcher, mock(), mock(), mock())),
                    SelectedSite(mockContext, mock()),
                    NetworkStatus(mockContext)
            ))

            /**
             * Mocking the below methods in [OrderListPresenter] class to pass mock values.
             * These are the methods that invoke [WCOrderStore] methods from FluxC
             */
            doReturn(true).whenever(mockedOrderListPresenter).isOrderStatusOptionsRefreshing()
            doReturn(orderStatusList).whenever(mockedOrderListPresenter).getOrderStatusOptions()
            doReturn(orders).whenever(mockedOrderListPresenter).fetchOrdersFromDb(null, false)
            return mockedOrderListPresenter
        }
    }

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun orderListFragment(): OrderListFragment
}
