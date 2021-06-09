package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.products.ProductsModule.AddProductDownloadFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.EditVariationAttributesFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.GroupedProductListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductDownloadDetailsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductReviewsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductSelectionListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductTypesBottomSheetFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.WPMediaPickerFragmentModule
import com.woocommerce.android.ui.products.downloads.AddProductDownloadBottomSheetFragment
import com.woocommerce.android.ui.products.downloads.AddProductDownloadModule
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsFragment
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsModule
import com.woocommerce.android.ui.products.reviews.ProductReviewsFragment
import com.woocommerce.android.ui.products.reviews.ProductReviewsModule
import com.woocommerce.android.ui.products.variations.attributes.edit.EditVariationAttributesFragment
import com.woocommerce.android.ui.products.variations.attributes.edit.EditVariationAttributesModule
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    EditVariationAttributesFragmentModule::class,
    WPMediaPickerFragmentModule::class,
    ProductTypesBottomSheetFragmentModule::class,
    ProductReviewsFragmentModule::class,
    GroupedProductListFragmentModule::class,
    ProductSelectionListFragmentModule::class,
    ProductDownloadDetailsFragmentModule::class,
    AddProductDownloadFragmentModule::class
])

object ProductsModule {
    @Module
    internal abstract class WPMediaPickerFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [WPMediaPickerModule::class])
        abstract fun wpMediaPickerFragment(): WPMediaPickerFragment
    }

    @Module
    internal abstract class ProductTypesBottomSheetFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductTypesBottomSheetModule::class])
        abstract fun productTypesBottomSheetFragment(): ProductTypesBottomSheetFragment
    }

    @Module
    internal abstract class ProductReviewsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductReviewsModule::class])
        abstract fun productReviewsFragment(): ProductReviewsFragment
    }

    @Module
    internal abstract class GroupedProductListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [GroupedProductListModule::class])
        abstract fun groupedProductListFragment(): GroupedProductListFragment
    }

    @Module
    internal abstract class ProductSelectionListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductSelectionListModule::class])
        abstract fun productSelectionListFragment(): ProductSelectionListFragment
    }

    @Module
    internal abstract class ProductDownloadDetailsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductDownloadDetailsModule::class])
        abstract fun productDownloadDetailsFragment(): ProductDownloadDetailsFragment
    }

    @Module
    internal abstract class AddProductDownloadFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AddProductDownloadModule::class])
        abstract fun provideAddProductDownloadFragment(): AddProductDownloadBottomSheetFragment
    }

    @Module
    internal abstract class EditVariationAttributesFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [EditVariationAttributesModule::class])
        abstract fun editVariationAttributesFragment(): EditVariationAttributesFragment
    }
}
