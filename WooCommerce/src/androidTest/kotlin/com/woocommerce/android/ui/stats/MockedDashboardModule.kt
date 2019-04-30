package com.woocommerce.android.ui.stats

import android.content.Context
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.DashboardContract
import com.woocommerce.android.ui.dashboard.DashboardFragment
import com.woocommerce.android.ui.dashboard.DashboardPresenter
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@Module
abstract class MockedDashboardModule {
    @Module
    companion object {
        @JvmStatic
        @ActivityScope
        @Provides
        fun provideDashboardPresenter(): DashboardContract.Presenter {
            /**
             * Creating a spy object here since we need to mock specific methods of [DashboardPresenter] class
             * instead of mocking all the methods in the class.
             * We cannot mock final classes ([WCStatsStore], [WCOrderStore], [SelectedSite] and [NetworkStatus]), so
             * creating a mock instance of those classes and passing to the presenter class constructor.
             */
            val mockDispatcher = mock<Dispatcher>()
            val mockContext = mock<Context>()
            val mockedDashboardPresenter = spy(DashboardPresenter(
                    mockDispatcher,
                    WooCommerceStore(
                            mockContext,
                            mockDispatcher,
                            WooCommerceRestClient(mockContext, mockDispatcher, mock(), mock(), mock())
                    ),
                    WCStatsStore(
                            mockDispatcher,
                            OrderStatsRestClient(mockContext, mockDispatcher, mock(), mock(), mock())
                    ),
                    WCOrderStore(mockDispatcher, OrderRestClient(mockContext, mockDispatcher, mock(), mock(), mock())),
                    SelectedSite(mockContext, mock()),
                    NetworkStatus(mockContext)
            ))

            /**
             * Mocking the below methods in [DashboardPresenter] class to pass mock values.
             * These are the methods that invoke [WCStatsStore] methods from FluxC
             */
            doNothing().whenever(mockedDashboardPresenter).fetchHasOrders()
//            doReturn(any()).whenever(mockedDashboardPresenter).getStatsCurrency()
//            doReturn(orders).whenever(mockedDashboardPresenter).loadStats(DAYS, false)
            return mockedDashboardPresenter
        }
    }

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun dashboardFragment(): DashboardFragment
}
