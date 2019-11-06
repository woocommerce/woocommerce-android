package com.woocommerce.android.media

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ProductImagesServiceModule {
    @ContributesAndroidInjector
    abstract fun productImagesService(): ProductImagesService
}
