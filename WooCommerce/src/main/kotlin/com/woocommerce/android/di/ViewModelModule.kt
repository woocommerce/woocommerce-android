package com.woocommerce.android.di

import com.woocommerce.android.ui.orders.detail.OrderDetailViewModel
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.viewmodel.ViewModelKey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal abstract class ViewModelModule {
    @ActivityScope
    @Binds
    @IntoMap
    @ViewModelKey(OrderDetailViewModel::class)
    internal abstract fun pluginOrderDetailViewModel(viewModel: OrderDetailViewModel): ViewModel

    @Binds
    internal abstract fun provideViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory
}
