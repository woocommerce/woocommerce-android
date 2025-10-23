package com.woocommerce.android.di

import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsInMemoryCache
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSourceInterface
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsInDbDataSource
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsDataSource
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsDataSourceInterface
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsInDbDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WooPosModule {
    @Binds
    @Singleton
    abstract fun provideWooPosProductsCache(implementation: WooPosProductsInMemoryCache): WooPosProductsCache

    companion object {
        @Provides
        @Singleton
        fun provideWooPosProductsDataSource(
            wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
            productsDataSource: WooPosProductsDataSource,
            productsInDbDataSource: WooPosProductsInDbDataSource
        ): WooPosProductsDataSourceInterface {
            return if (wooPosLocalCatalogM1Enabled()) {
                productsInDbDataSource
            } else {
                productsDataSource
            }
        }

        @Provides
        @Singleton
        fun provideWooPosVariationsDataSource(
            wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
            variationsDataSource: WooPosVariationsDataSource,
            variationsInDbDataSource: WooPosVariationsInDbDataSource
        ): WooPosVariationsDataSourceInterface {
            return if (wooPosLocalCatalogM1Enabled()) {
                variationsInDbDataSource
            } else {
                variationsDataSource
            }
        }
    }
}
