package com.woocommerce.android.ui.products

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.orders.OrderProductListContract
import com.woocommerce.android.ui.orders.OrderProductListFragment
import com.woocommerce.android.ui.orders.OrderProductListPresenter
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.store.WCOrderStore

@Module
abstract class MockedOrderProductListModule {
    @Module
    companion object {
        private var order: WCOrderModel? = null

        fun setOrderInfo(order: WCOrderModel) {
            this.order = order
        }

        @JvmStatic
        @ActivityScope
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

            val mockedOrderProductListPresenter = spy(OrderProductListPresenter(
                    WCOrderStore(mockDispatcher, OrderRestClient(mockContext, mockDispatcher, mock(), mock(), mock()))
            ))

            /**
             * Mocking the below methods in [OrderProductListPresenter] class to pass mock values.
             * These are the methods that invoke [WCOrderModel] methods from FluxC.
             */
            doReturn(order).whenever(mockedOrderProductListPresenter).getOrderDetailFromDb(any())
            return mockedOrderProductListPresenter
        }
    }

    @ContributesAndroidInjector
    abstract fun orderProductListFragment(): OrderProductListFragment
}
