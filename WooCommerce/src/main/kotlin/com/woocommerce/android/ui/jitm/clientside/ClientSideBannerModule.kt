package com.woocommerce.android.ui.jitm.clientside

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientSideBannerModule {

    @Binds
    @IntoSet
    abstract fun bindPosBanner(banner: ClientSidePosBanner): ClientSideBanner
}
