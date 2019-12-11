package com.woocommerce.android.di

import com.woocommerce.android.ui.orders.list.OrderFetcher
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import javax.inject.Singleton

@Module
class OrderFetcherModule {
    @Singleton
    @Provides
    fun provideOrderFetcher(dispatcher: Dispatcher) = OrderFetcher(dispatcher)
}
