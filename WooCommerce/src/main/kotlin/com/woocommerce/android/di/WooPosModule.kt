package com.woocommerce.android.di

import com.woocommerce.android.ui.woopos.common.data.WooPosOrdersInMemoryCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsInMemoryCache
import com.woocommerce.android.ui.woopos.common.data.searchbyidentifier.WooPosOrdersCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WooPosModule {
    @Binds
    @Singleton
    abstract fun provideWooPosProductsCache(implementation: WooPosProductsInMemoryCache): WooPosProductsCache

    @Binds
    @Singleton
    abstract fun provideWooPosOrdersCache(implementation: WooPosOrdersInMemoryCache): WooPosOrdersCache
}
