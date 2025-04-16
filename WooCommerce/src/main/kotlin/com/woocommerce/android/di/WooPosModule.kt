package com.woocommerce.android.di

import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsInMemoryCache
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsDataSource
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.providers.CouponItemsProvider
import com.woocommerce.android.ui.woopos.home.items.providers.ProductItemsProvider
import com.woocommerce.android.ui.woopos.home.items.providers.WooPosItemDataProvider
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WooPosModule {
    @Binds
    @Singleton
    abstract fun provideWooPosProductsCache(implementation: WooPosProductsInMemoryCache): WooPosProductsCache

    companion object {
        @Provides
        @Named("ProductProvider")
        fun provideFirstDataProvider(
            productsDataSource: WooPosProductsDataSource,
            priceFormat: WooPosFormatPrice
        ): WooPosItemDataProvider {
            return ProductItemsProvider(productsDataSource, priceFormat)
        }

        @Provides
        @Named("CouponsProvider")
        fun provideSecondDataProvider(
            couponsDataSource: WooPosCouponsDataSource,
        ): WooPosItemDataProvider {
            return CouponItemsProvider(couponsDataSource)
        }
    }
}
