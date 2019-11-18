package com.woocommerce.android.ui.imageviewer

import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.Binds
import dagger.Module

@Module
internal abstract class ImageViewerModule {
    @ActivityScope
    @Binds
    abstract fun provideUiMessageResolver(uiIMessageResolver: ImageViewerUIMessageResolver): UIMessageResolver
}
