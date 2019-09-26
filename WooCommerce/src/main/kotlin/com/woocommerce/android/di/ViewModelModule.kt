package com.woocommerce.android.di

import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.viewmodel.ViewModelKey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.refunds.IssueRefundViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(ProductDetailViewModel::class)
    internal abstract fun pluginProductDetailViewModel(viewModel: ProductDetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(IssueRefundViewModel::class)
    internal abstract fun refundsViewModel(viewModel: IssueRefundViewModel): ViewModel

    @Binds
    internal abstract fun provideViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory
}
