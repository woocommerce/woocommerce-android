package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.products.ProductsModule.ProductDetailFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductImageViewerFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductImagesFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductInventoryFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductPricingFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductSettingsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductShippingClassFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductShippingFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductStatusListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductVariantsFragmentModule
import com.woocommerce.android.ui.products.settings.ProductSettingsFragment
import com.woocommerce.android.ui.products.settings.ProductSettingsModule
import com.woocommerce.android.ui.products.settings.ProductStatusListFragment
import com.woocommerce.android.ui.products.settings.ProductStatusListModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    ProductDetailFragmentModule::class,
    ProductListFragmentModule::class,
    ProductVariantsFragmentModule::class,
    ProductImagesFragmentModule::class,
    ProductImageViewerFragmentModule::class,
    ProductInventoryFragmentModule::class,
    ProductShippingFragmentModule::class,
    ProductShippingClassFragmentModule::class,
    ProductPricingFragmentModule::class,
    ProductSettingsFragmentModule::class,
    ProductStatusListFragmentModule::class
])
object ProductsModule {
    @Module
    abstract class ProductListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductListModule::class])
        abstract fun productListFragment(): ProductListFragment
    }

    @Module
    abstract class ProductDetailFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductDetailModule::class])
        abstract fun productDetailFragment(): ProductDetailFragment
    }

    @Module
    internal abstract class ProductVariantsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductVariantsModule::class])
        abstract fun productVariantsFragment(): ProductVariantsFragment
    }

    @Module
    internal abstract class ProductInventoryFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductInventoryModule::class])
        abstract fun productInventoryFragment(): ProductInventoryFragment
    }

    @Module
    internal abstract class ProductShippingFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductShippingModule::class])
        abstract fun productShippingFragment(): ProductShippingFragment
    }

    @Module
    internal abstract class ProductShippingClassFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductShippingClassModule::class])
        abstract fun productShippingClassFragment(): ProductShippingClassFragment
    }

    @Module
    internal abstract class ProductImagesFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductImagesModule::class])
        abstract fun productImagesFragment(): ProductImagesFragment
    }

    @Module
    internal abstract class ProductImageViewerFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductImageViewerModule::class])
        abstract fun productImageViewerFragment(): ProductImageViewerFragment
    }

    @Module
    internal abstract class ProductPricingFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductPricingModule::class])
        abstract fun productPricingFragment(): ProductPricingFragment
    }

    @Module
    internal abstract class ProductSettingsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductSettingsModule::class])
        abstract fun productSettingsFragment(): ProductSettingsFragment
    }

    @Module
    internal abstract class ProductStatusListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductStatusListModule::class])
        abstract fun productStatusListFragment(): ProductStatusListFragment
    }
}
