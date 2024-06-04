package com.woocommerce.android.ui.woopos.common.di

import com.woocommerce.android.ui.products.selector.ProductListHandler
import com.woocommerce.android.ui.woopos.cartcheckout.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.cartcheckout.products.WooPosProductsDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class POSModule {
    @Provides
    fun provideProductList(
        handler: ProductListHandler
    ): WooPosProductsDataSource {
        return WooPosProductsDataSourceImpl(handler)
    }
}
