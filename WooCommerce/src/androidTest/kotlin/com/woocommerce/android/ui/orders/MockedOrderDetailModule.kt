package com.woocommerce.android.ui.orders

import android.content.Context
import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.any
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
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.persistence.NotificationSqlUtils
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.tools.FormattableContentMapper

@Module
abstract class MockedOrderDetailModule {
    @Module
    companion object {
        private var order: WCOrderModel? = null
        private var orderStatus: WCOrderStatusModel? = null

        fun setOrderInfo(order: WCOrderModel) {
            this.order = order
        }

        fun setOrderStatus(orderStatus: WCOrderStatusModel) {
            this.orderStatus = orderStatus
        }

        @JvmStatic
        @ActivityScope
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
            val mockSiteStore = mock<SiteStore>()

            val mockedOrderDetailPresenter = spy(OrderDetailPresenter(
                    mockDispatcher,
                    WCOrderStore(mockDispatcher, OrderRestClient(mockContext, mockDispatcher, mock(), mock(), mock())),
                    WCProductStore(
                            mockDispatcher,
                            ProductRestClient(mockContext, mockDispatcher, mock(), mock(), mock())),
                    SelectedSite(mockContext, mockSiteStore),
                    mock(),
                    NetworkStatus(mockContext),
                    NotificationStore(
                            mock(), mockContext,
                            NotificationRestClient(mockContext, mockDispatcher, mock(), mock(), mock()),
                            NotificationSqlUtils(FormattableContentMapper(Gson())), mockSiteStore)
            ))

            /**
             * Mocking the below methods in [OrderDetailPresenter] class to pass mock values.
             * These are the methods that invoke [WCOrderModel], [WCOrderStatusModel] methods from FluxC.
             */
            doReturn(order).whenever(mockedOrderDetailPresenter).loadOrderDetailFromDb(any())
            doReturn(orderStatus).whenever(mockedOrderDetailPresenter).getOrderStatusForStatusKey(any())
            return mockedOrderDetailPresenter
        }
    }

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun orderDetailfragment(): OrderDetailFragment
}
