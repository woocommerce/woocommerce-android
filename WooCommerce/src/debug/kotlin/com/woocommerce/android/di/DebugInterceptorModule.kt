package com.woocommerce.android.di

import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import javax.inject.Named

@InstallIn(SingletonComponent::class)
@Module
class DebugInterceptorModule {
    @Provides
    @IntoSet
    @Named("network-interceptors")
    fun provideNetworkInterceptor(): Interceptor = FlipperOkhttpInterceptor(
        AndroidFlipperClient.getInstanceIfInitialized()?.getPlugin(NetworkFlipperPlugin.ID)
    )
}
