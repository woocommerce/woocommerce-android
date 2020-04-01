package com.woocommerce.android.ui.products

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.helpers.initCoroutineEngine
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderProductListContract
import com.woocommerce.android.ui.orders.OrderProductListPresenter
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.refunds.RefundMapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore

@Module
abstract class MockedOrderProductListModule {
    @Module
    companion object {
        private var order: WCOrderModel? = null

        fun setOrderInfo(order: WCOrderModel) {
            this.order = order
        }

        @JvmStatic
        @Provides
        fun provideOrderProductListPresenter(): OrderProductListContract.Presenter {
            /**
             * Creating a spy object here since we need to mock specific methods of [OrderProductListPresenter] class
             * instead of mocking all the methods in the class.
             * We cannot mock final classes ([WCOrderStore]), so
             * creating a mock instance of those classes and passing to the presenter class constructor.
             */
            val mockDispatcher = mock<Dispatcher>()
            val mockContext = mock<Context>()
            val mockSelectedSite = mock<SelectedSite>()
            val testDispatchers = CoroutineDispatchers(
                    Dispatchers.Unconfined,
                    Dispatchers.Unconfined,
                    Dispatchers.Unconfined
            )

            val mockedOrderProductListPresenter = spy(
                    OrderProductListPresenter(
                            WCOrderStore(
                                    mockDispatcher,
                                    OrderRestClient(
                                            mockContext,
                                            mockDispatcher,
                                            mock(),
                                            mock(),
                                            mock()
                                    )
                            ),
                            WCRefundStore(
                                    RefundRestClient(mockDispatcher, mock(), mock(), mock(), mock(), mock()),
                                    initCoroutineEngine(),
                                    RefundMapper()
                            ),
                            mockSelectedSite
                    )
            )

            /**
             * Mocking the below methods in [OrderProductListPresenter] class to pass mock values.
             * These are the methods that invoke [WCOrderModel] methods from FluxC.
             */
            doReturn(order).whenever(mockedOrderProductListPresenter).getOrderDetailFromDb(any())
            return mockedOrderProductListPresenter
        }
    }
}
