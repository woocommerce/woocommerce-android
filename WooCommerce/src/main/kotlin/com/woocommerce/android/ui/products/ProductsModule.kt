package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.products.ProductsModule.WPMediaPickerFragmentModule
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    WPMediaPickerFragmentModule::class
])

object ProductsModule {
    @Module
    internal abstract class WPMediaPickerFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [WPMediaPickerModule::class])
        abstract fun wpMediaPickerFragment(): WPMediaPickerFragment
    }
}
