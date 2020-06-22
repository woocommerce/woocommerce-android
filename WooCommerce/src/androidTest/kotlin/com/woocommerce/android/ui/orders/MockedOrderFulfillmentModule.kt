package com.woocommerce.android.ui.orders

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.refunds.RefundMapper
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelMapper
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper

@Module
abstract class MockedOrderFulfillmentModule {
    @Module
    companion object {
        private var order: WCOrderModel? = null
        private var isVirtualProduct: Boolean = false
        private var isNetworkConnected: Boolean = false
        private var onOrderChanged: OnOrderChanged? = null
        private var orderShipmentTrackings: List<WCOrderShipmentTrackingModel>? = null

        fun setOrderInfo(order: WCOrderModel) {
            this.order = order
        }

        fun setNetworkConnected(isNetworkConnected: Boolean) {
            this.isNetworkConnected = isNetworkConnected
        }

        fun setIsVirtualProduct(isVirtualProduct: Boolean) {
            this.isVirtualProduct = isVirtualProduct
        }

        fun setOnOrderChanged(onOrderChanged: OnOrderChanged?) {
            this.onOrderChanged = onOrderChanged
        }

        fun setOrderShipmentTrackings(orderShipmentTrackings: List<WCOrderShipmentTrackingModel>) {
            this.orderShipmentTrackings = orderShipmentTrackings
        }

        @JvmStatic
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
            val mockNetworkStatus = mock<NetworkStatus>()
            val mockSelectedSite = mock<SelectedSite>()

            val mockedOrderFulfillmentPresenter = spy(OrderFulfillmentPresenter(
                    mockDispatcher,
                    WCOrderStore(mockDispatcher, OrderRestClient(mockContext, mockDispatcher, mock(), mock(), mock())),
                    WCProductStore(
                            mockDispatcher,
                            ProductRestClient(mockContext, mockDispatcher, mock(), mock(), mock())
                    ),
                    WCRefundStore(
                            RefundRestClient(
                                    mockDispatcher,
                                    JetpackTunnelGsonRequestBuilder(),
                                    mock(),
                                    mock(),
                                    mock(),
                                    mock()
                            ),
                            CoroutineEngine(Dispatchers.Unconfined, AppLogWrapper()),
                            RefundMapper()
                    ),
                    mockSelectedSite,
                    WCShippingLabelStore(
                    ShippingLabelRestClient(
                        mockDispatcher,
                        JetpackTunnelGsonRequestBuilder(),
                        mockContext,
                        mock(),
                        mock(),
                        mock()
                    ),
                    CoroutineEngine(Dispatchers.Unconfined, AppLogWrapper()),
                    WCShippingLabelMapper()
                    ),
                    mock(),
                    mockNetworkStatus
            ))

            /**
             * Mocking the below methods in [OrderFulfillmentPresenter] class to pass mock values.
             * These are the methods that invoke [WCOrderShipmentTrackingModel] methods from FluxC.
             */
            doReturn(SiteModel()).whenever(mockSelectedSite).get()
            doReturn(isNetworkConnected).whenever(mockNetworkStatus).isConnected()
            doReturn(isVirtualProduct).whenever(mockedOrderFulfillmentPresenter).isVirtualProduct(any())
            doReturn(true).whenever(mockedOrderFulfillmentPresenter).isShipmentTrackingsFetched
            doReturn(order).whenever(mockedOrderFulfillmentPresenter).getOrderDetailFromDb(any())
            doReturn(orderShipmentTrackings).whenever(mockedOrderFulfillmentPresenter)
                    .getShipmentTrackingsFromDb(any())
            orderShipmentTrackings?.let {
                if (it.isNotEmpty()) {
                    doReturn(it[0]).whenever(mockedOrderFulfillmentPresenter).deletedOrderShipmentTrackingModel
                }
            }

            // adding mock response when order status is marked as complete
            doAnswer {
                onOrderChanged?.let { mockedOrderFulfillmentPresenter.onOrderChanged(it) }
            }.whenever(mockDispatcher).dispatch(any<Action<UpdateOrderStatusPayload>>())

            return mockedOrderFulfillmentPresenter
        }
    }
}
