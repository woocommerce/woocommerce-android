package com.woocommerce.android.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.woocommerce.android.ui.orders.list.MockedOrderListViewModel
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.products.MockedProductDetailViewModel
import com.woocommerce.android.ui.reviews.ReviewListViewModel
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal abstract class MockedViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(ReviewListViewModel::class)
    internal abstract fun reviewListViewModel(viewModel: ReviewListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MockedProductDetailViewModel::class)
    internal abstract fun productDetailViewModel(viewModel: MockedProductDetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OrderListViewModel::class)
    internal abstract fun orderListViewModel(viewModel: MockedOrderListViewModel): ViewModel

    @Binds
    internal abstract fun provideViewModelFactory(viewModelFactor: ViewModelFactory): ViewModelProvider.Factory
}
