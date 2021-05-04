package com.woocommerce.android.media

import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
internal abstract class ProductImagesServiceModule {
    @ContributesAndroidInjector
    abstract fun productImagesService(): ProductImagesService
}
