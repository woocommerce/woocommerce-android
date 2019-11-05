package com.woocommerce.android.ui.products

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ProductImagesModule {
    @ContributesAndroidInjector
    abstract fun productImagesfragment(): ProductImagesFragment
}
