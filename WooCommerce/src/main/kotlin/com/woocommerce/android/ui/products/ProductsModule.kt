package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.products.ProductsModule.AddProductDownloadFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.EditVariationAttributesFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.WPMediaPickerFragmentModule
import com.woocommerce.android.ui.products.downloads.AddProductDownloadBottomSheetFragment
import com.woocommerce.android.ui.products.downloads.AddProductDownloadModule
import com.woocommerce.android.ui.products.variations.attributes.edit.EditVariationAttributesFragment
import com.woocommerce.android.ui.products.variations.attributes.edit.EditVariationAttributesModule
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    EditVariationAttributesFragmentModule::class,
    WPMediaPickerFragmentModule::class,
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
