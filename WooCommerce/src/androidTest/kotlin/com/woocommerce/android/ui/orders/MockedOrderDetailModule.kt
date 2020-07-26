package com.woocommerce.android.ui.orders

import android.content.Context
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers.Unconfined
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.refunds.RefundMapper
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelMapper
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient
import org.wordpress.android.fluxc.persistence.NotificationSqlUtils
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.tools.FormattableContentMapper
import org.wordpress.android.fluxc.utils.AppLogWrapper

@Module
abstract class MockedOrderDetailModule {
    @Module
    companion object {
        private var order: WCOrderModel? = null
        private var isVirtualProduct: Boolean = false
        private var isNetworkConnected: Boolean = false
        private var onOrderChanged: OnOrderChanged? = null
        private var orderStatus: WCOrderStatusModel? = null
        private var orderNotes: List<WCOrderNoteModel>? = null
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

        fun setOrderStatus(orderStatus: WCOrderStatusModel) {
            this.orderStatus = orderStatus
        }

        fun setOrderNotes(orderNotes: List<WCOrderNoteModel>) {
            this.orderNotes = orderNotes
        }

        fun setOrderShipmentTrackings(orderShipmentTrackings: List<WCOrderShipmentTrackingModel>) {
            this.orderShipmentTrackings = orderShipmentTrackings
        }

        fun setOnOrderChanged(onOrderChanged: OnOrderChanged?) {
            this.onOrderChanged = onOrderChanged
        }

        @JvmStatic
        @Provides
        fun provideOrderDetailPresenter(): OrderDetailContract.Presenter {
            /**
             * Creating a spy object here since we need to mock specific methods of [OrderDetailPresenter] class
             * instead of mocking all the methods in the class.
             * We cannot mock final classes ([WCOrderStore], [WCProductStore], [SelectedSite] and [NetworkStatus]), so
             * creating a mock instance of those classes and passing to the presenter class constructor.
             */
            val mockDispatcher = mock<Dispatcher>()
            val mockContext = mock<Context>()
            val mockSelectedSite = mock<SelectedSite>()
            val mockNetworkStatus = mock<NetworkStatus>()
            val refundStore = WCRefundStore(
                    RefundRestClient(
                            mockDispatcher,
                            JetpackTunnelGsonRequestBuilder(),
                            mockContext,
                            mock(),
                            mock(),
                            mock()
                    ),
                    CoroutineEngine(Unconfined, AppLogWrapper()),
                    RefundMapper()
            )
            val shippingLabelStore = WCShippingLabelStore(
                ShippingLabelRestClient(
                    mockDispatcher,
                    JetpackTunnelGsonRequestBuilder(),
                    mockContext,
                    mock(),
                    mock(),
                    mock()
                ),
                CoroutineEngine(Unconfined, AppLogWrapper()),
                WCShippingLabelMapper()
            )
            val orderStore = WCOrderStore(
                mockDispatcher, OrderRestClient(mockContext, mockDispatcher, mock(), mock(), mock())
            )
            val coroutineDispatchers = CoroutineDispatchers(Unconfined, Unconfined, Unconfined)

            val mockedOrderDetailPresenter = spy(OrderDetailPresenter(
                    coroutineDispatchers,
                    mockDispatcher,
                    orderStore,
                    WCProductStore(
                            mockDispatcher,
                            ProductRestClient(mockContext, mockDispatcher, mock(), mock(), mock())
                    ),
                    mockSelectedSite,
                    mock(),
                    mockNetworkStatus,
                    NotificationStore(
                            mock(), mockContext,
                            NotificationRestClient(mockContext, mockDispatcher, mock(), mock(), mock()),
                            NotificationSqlUtils(FormattableContentMapper(Gson()))),
                    OrderDetailRepository(
                        mockDispatcher,
                        orderStore,
                        refundStore,
                        shippingLabelStore,
                        mockSelectedSite
                    )
            ))

            /*
             * Mocking the below methods in [OrderDetailPresenter] class to pass mock values.
             * These are the methods that invoke [WCOrderModel], [WCOrderStatusModel] methods from FluxC.
             */
            doNothing().whenever(mockedOrderDetailPresenter).requestShipmentTrackingsFromApi(any())
            doNothing().whenever(mockedOrderDetailPresenter).requestOrderNotesFromApi(any())
            doReturn(SiteModel()).whenever(mockSelectedSite).get()
            doReturn(isVirtualProduct).whenever(mockedOrderDetailPresenter).isVirtualProduct(any())
            doReturn(isNetworkConnected).whenever(mockNetworkStatus).isConnected()
            doReturn(order).whenever(mockedOrderDetailPresenter).loadOrderDetailFromDb(any())
            doReturn(orderStatus).whenever(mockedOrderDetailPresenter).getOrderStatusForStatusKey(any())
            doReturn(orderNotes).whenever(mockedOrderDetailPresenter).fetchOrderNotesFromDb(any())
            doReturn(orderShipmentTrackings).whenever(mockedOrderDetailPresenter)
                    .getOrderShipmentTrackingsFromDb(any())
            orderShipmentTrackings?.let {
                if (it.isNotEmpty()) {
                    doReturn(it[0]).whenever(mockedOrderDetailPresenter).deletedOrderShipmentTrackingModel
                }
            }

            // adding mock response when order status is marked as complete
            doAnswer {
                onOrderChanged?.let { mockedOrderDetailPresenter.onOrderChanged(it) }
            }.whenever(mockDispatcher).dispatch(any<Action<UpdateOrderStatusPayload>>())

            return mockedOrderDetailPresenter
        }
    }
}
