package com.woocommerce.android.ui.woopos.di

import com.woocommerce.android.ui.products.selector.ProductListHandler
import com.woocommerce.android.ui.woopos.cart.products.ProductsDataSource
import com.woocommerce.android.ui.woopos.cart.products.ProductsDataSourceImpl
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
    ): ProductsDataSource {
        return ProductsDataSourceImpl(handler)
    }
}