package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.products.ProductsModule.AddProductCategoryFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ParentCategoryListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductCatalogVisibilityFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductCategoriesFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductDetailFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductExternalLinkFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductFilterListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductFilterOptionListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductImageViewerFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductImagesFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductInventoryFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductMenuOrderFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductPricingFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductSettingsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductShippingClassFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductShippingFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductSlugFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductSortingFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductStatusFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductTagsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductVariantFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductVariantsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductVisibilityFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.WPMediaPickerFragmentModule
import com.woocommerce.android.ui.products.categories.AddProductCategoryFragment
import com.woocommerce.android.ui.products.categories.AddProductCategoryModule
import com.woocommerce.android.ui.products.categories.ParentCategoryListFragment
import com.woocommerce.android.ui.products.categories.ParentCategoryListModule
import com.woocommerce.android.ui.products.categories.ProductCategoriesFragment
import com.woocommerce.android.ui.products.categories.ProductCategoriesModule
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibilityFragment
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibilityModule
import com.woocommerce.android.ui.products.settings.ProductMenuOrderFragment
import com.woocommerce.android.ui.products.settings.ProductMenuOrderModule
import com.woocommerce.android.ui.products.settings.ProductSettingsFragment
import com.woocommerce.android.ui.products.settings.ProductSettingsModule
import com.woocommerce.android.ui.products.settings.ProductSlugFragment
import com.woocommerce.android.ui.products.settings.ProductSlugModule
import com.woocommerce.android.ui.products.settings.ProductStatusFragment
import com.woocommerce.android.ui.products.settings.ProductStatusModule
import com.woocommerce.android.ui.products.settings.ProductVisibilityFragment
import com.woocommerce.android.ui.products.settings.ProductVisibilityModule
import com.woocommerce.android.ui.products.tags.ProductTagsFragment
import com.woocommerce.android.ui.products.tags.ProductTagsModule
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    ProductDetailFragmentModule::class,
    ProductVariantFragmentModule::class,
    ProductListFragmentModule::class,
    ProductFilterListFragmentModule::class,
    ProductFilterOptionListFragmentModule::class,
    ProductVariantsFragmentModule::class,
    ProductImagesFragmentModule::class,
    ProductImageViewerFragmentModule::class,
    ProductInventoryFragmentModule::class,
    ProductShippingFragmentModule::class,
    ProductShippingClassFragmentModule::class,
    ProductPricingFragmentModule::class,
    ProductSettingsFragmentModule::class,
    ProductCatalogVisibilityFragmentModule::class,
    ProductStatusFragmentModule::class,
    ProductSlugFragmentModule::class,
    ProductExternalLinkFragmentModule::class,
    ProductMenuOrderFragmentModule::class,
    ProductVisibilityFragmentModule::class,
    WPMediaPickerFragmentModule::class,
    ProductSortingFragmentModule::class,
    ProductCategoriesFragmentModule::class,
    AddProductCategoryFragmentModule::class,
    ParentCategoryListFragmentModule::class,
    ProductTagsFragmentModule::class
])
object ProductsModule {
    @Module
    abstract class ProductListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductListModule::class])
        abstract fun productListFragment(): ProductListFragment
    }

    @Module
    abstract class ProductFilterListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductFilterListModule::class])
        abstract fun productFilterListFragment(): ProductFilterListFragment
    }

    @Module
    abstract class ProductFilterOptionListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductFilterOptionListModule::class])
        abstract fun productFilterOptionListFragment(): ProductFilterOptionListFragment
    }

    @Module
    abstract class ProductDetailFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductDetailModule::class])
        abstract fun productDetailFragment(): ProductDetailFragment
    }

    @Module
    abstract class ProductVariantFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductVariantModule::class])
        abstract fun productVariantFragment(): ProductVariantFragment
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
    internal abstract class ProductStatusFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductStatusModule::class])
        abstract fun productStatusFragment(): ProductStatusFragment
    }

    @Module
    internal abstract class ProductCatalogVisibilityFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductCatalogVisibilityModule::class])
        abstract fun productCatalogVisibilityFragment(): ProductCatalogVisibilityFragment
    }

    @Module
    internal abstract class ProductVisibilityFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductVisibilityModule::class])
        abstract fun productVisibilityFragment(): ProductVisibilityFragment
    }

    @Module
    internal abstract class ProductSlugFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductSlugModule::class])
        abstract fun productSlugFragment(): ProductSlugFragment
    }

    @Module
    internal abstract class ProductExternalLinkFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductExternalLinkModule::class])
        abstract fun productExternalLinkFragment(): ProductExternalLinkFragment
    }

    @Module
    internal abstract class ProductMenuOrderFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductMenuOrderModule::class])
        abstract fun productMenuOrderFragment(): ProductMenuOrderFragment
    }

    @Module
    internal abstract class WPMediaPickerFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [WPMediaPickerModule::class])
        abstract fun wpMediaPickerFragment(): WPMediaPickerFragment
    }

    @Module
    internal abstract class ProductSortingFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductSortingModule::class])
        abstract fun productSortingFragment(): ProductSortingFragment
    }

    @Module
    internal abstract class ProductCategoriesFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductCategoriesModule::class])
        abstract fun productCategoriesFragment(): ProductCategoriesFragment
    }

    @Module
    internal abstract class AddProductCategoryFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AddProductCategoryModule::class])
        abstract fun addProductCategoryFragment(): AddProductCategoryFragment
    }

    @Module
    internal abstract class ParentCategoryListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ParentCategoryListModule::class])
        abstract fun parentCategoryListFragment(): ParentCategoryListFragment
    }

    @Module
    internal abstract class ProductTagsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductTagsModule::class])
        abstract fun productTagsFragment(): ProductTagsFragment
    }
}
