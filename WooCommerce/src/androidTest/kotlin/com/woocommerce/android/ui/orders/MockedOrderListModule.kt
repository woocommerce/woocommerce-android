package com.woocommerce.android.ui.orders

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.model.WCOrderModel

@Module
object MockedOrderListModule {
    private var orders: List<WCOrderModel>? = null

    fun setOrders(orders: List<WCOrderModel>) {
        this.orders = orders
    }

    @JvmStatic
    @FragmentScope
    @Provides
    fun provideOrderListPresenter(): OrderListContract.Presenter {
        val mockedOrderListPresenter = mock<OrderListPresenter>()
        whenever(mockedOrderListPresenter.fetchOrdersFromDb(isForceRefresh = false))
                .thenReturn(orders)
        return mockedOrderListPresenter
    }

    @JvmStatic
    @FragmentScope
    @Provides
    fun orderListFragment(): OrderListFragment {
        return OrderListFragment.newInstance()
    }
}
