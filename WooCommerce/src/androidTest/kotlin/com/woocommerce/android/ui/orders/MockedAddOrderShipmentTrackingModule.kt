package com.woocommerce.android.ui.orders

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged

@Module
internal abstract class MockedAddOrderShipmentTrackingModule {
    @Module
    companion object {
        private var order: WCOrderModel? = null
        private var isNetworkConnected: Boolean = false
        private var onOrderChanged: OnOrderChanged? = null

        fun setOrderInfo(order: WCOrderModel) {
            this.order = order
        }

        fun setOnOrderChanged(onOrderChanged: OnOrderChanged) {
            this.onOrderChanged = onOrderChanged
        }

        fun setNetworkConnected(isNetworkConnected: Boolean) {
            this.isNetworkConnected = isNetworkConnected
        }

        @JvmStatic
        @ActivityScope
        @Provides
        fun provideAddOrderShipmentTrackingPresenter(): AddOrderShipmentTrackingContract.Presenter {
            /**
             * Creating a spy object here since we need to mock specific methods of
             * [AddOrderShipmentTrackingPresenter] class instead of mocking all the methods in the class.
             * We cannot mock final classes ([WCOrderStore], [SelectedSite] and [NetworkStatus]), so
             * creating a mock instance of those classes and passing to the presenter class constructor.
             */
            val mockDispatcher = mock<Dispatcher>()
            val mockContext = mock<Context>()
            val mockSelectedSite = mock<SelectedSite>()
            val mockNetworkStatus = mock<NetworkStatus>()

            val mockPresenter = spy(AddOrderShipmentTrackingPresenter(
                    mockDispatcher,
                    WCOrderStore(mockDispatcher, OrderRestClient(mockContext, mockDispatcher, mock(), mock(), mock())),
                    mockSelectedSite,
                    mockNetworkStatus
            ))

            /**
             * Mocking the below methods in [AddOrderShipmentTrackingPresenter] class to pass mock values.
             */
            doReturn(SiteModel()).whenever(mockSelectedSite).get()
            doReturn(isNetworkConnected).whenever(mockNetworkStatus).isConnected()
            doReturn(order).whenever(mockPresenter).loadOrderDetailFromDb(any())
            doAnswer {
                onOrderChanged?.let { mockPresenter.onOrderChanged(it) }
            }.whenever(mockDispatcher).dispatch(any<Action<AddOrderShipmentTrackingPayload>>())

            return mockPresenter
        }
    }

    @ContributesAndroidInjector
    abstract fun addOrderShipmentTrackingFragment(): AddOrderShipmentTrackingFragment
}
