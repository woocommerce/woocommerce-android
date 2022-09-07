package com.woocommerce.android.ui.main

import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@InstallIn(ActivityComponent::class)
@Module
interface UIMessageResolverModule {
    @ActivityScoped
    @Binds
    abstract fun provideUiMessageResolver(mainUIMessageResolver: MainUIMessageResolver): UIMessageResolver
}
