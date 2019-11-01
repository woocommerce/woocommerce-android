package com.woocommerce.android.media

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class MediaUploadModule {
    @ContributesAndroidInjector
    abstract fun mediaUploadService(): MediaUploadService
}
