package com.woocommerce.android.media

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ProductImagesModule {
    @ContributesAndroidInjector
    abstract fun productImagesService(): ProductImagesService
}
