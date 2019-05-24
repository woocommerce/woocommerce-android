package com.woocommerce.android.ui.orders

import android.content.Context
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCOrderStore

@Module
abstract class MockedOrderFulfillmentModule {
    @Module
    companion object {
        private var order: WCOrderModel? = null
        private var orderShipmentTrackings: List<WCOrderShipmentTrackingModel>? = null

        fun setOrderInfo(order: WCOrderModel) {
            this.order = order
        }

        fun setOrderShipmentTrackings(orderShipmentTrackings: List<WCOrderShipmentTrackingModel>) {
            this.orderShipmentTrackings = orderShipmentTrackings
        }

        @JvmStatic
        @ActivityScope
        @Provides
        fun provideOrderFulfillmentPresenter(): OrderFulfillmentContract.Presenter {
            /**
             * Creating a spy object here since we need to mock specific methods of [OrderFulfillmentPresenter] class
             * instead of mocking all the methods in the class.
             * We cannot mock final classes ([WCOrderStore], [Dispatcher], [SelectedSite] and [NetworkStatus]), so
             * creating a mock instance of those classes and passing to the presenter class constructor.
             */
            val mockDispatcher = mock<Dispatcher>()
            val mockContext = mock<Context>()
            val mockSiteStore = mock<SiteStore>()
            val mockNetworkStatus = mock<NetworkStatus>()

            val mockedOrderFulfillmentPresenter = spy(OrderFulfillmentPresenter(
                    mockDispatcher,
                    WCOrderStore(mockDispatcher, OrderRestClient(mockContext, mockDispatcher, mock(), mock(), mock())),
                    SelectedSite(mockContext, mockSiteStore),
                    mock(),
                    mockNetworkStatus
            ))

            /**
             * Mocking the below methods in [OrderFulfillmentPresenter] class to pass mock values.
             * These are the methods that invoke [WCOrderShipmentTrackingModel] methods from FluxC.
             */
            doReturn(true).whenever(mockedOrderFulfillmentPresenter).isShipmentTrackingsFetched
            doReturn(order).whenever(mockedOrderFulfillmentPresenter).loadOrderDetailFromDb(any())
            doReturn(orderShipmentTrackings).whenever(mockedOrderFulfillmentPresenter)
                    .requestShipmentTrackingsFromDb(any())

            return mockedOrderFulfillmentPresenter
        }
    }

    @ContributesAndroidInjector
    abstract fun orderFulfillmentFragment(): OrderFulfillmentFragment
}
