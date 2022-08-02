package com.woocommerce.android.di

import com.automattic.android.tracks.crashlogging.CrashLoggingOkHttpInterceptorProvider
import com.automattic.android.tracks.crashlogging.FormattedUrl
import com.automattic.android.tracks.crashlogging.RequestFormatter
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.woocommerce.android.performance.WooRequestFormatter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import javax.inject.Named
import okhttp3.Request

@InstallIn(SingletonComponent::class)
@Module
class InterceptorModule {
    @Provides
    @IntoSet
    @Named("network-interceptors")
    fun provideNetworkInterceptor(): Interceptor = FlipperOkhttpInterceptor(
        AndroidFlipperClient.getInstanceIfInitialized()?.getPlugin(NetworkFlipperPlugin.ID)
    )

    @Provides
    @IntoSet
    @Named("network-interceptors")
    fun provideMonitoring(): Interceptor = CrashLoggingOkHttpInterceptorProvider
        .createInstance(
//            WooRequestFormatter,
        NoOp
        )


    object NoOp: RequestFormatter{
        override fun formatRequestUrl(request: Request): FormattedUrl {
            return request.url.toString()
        }

    }
}
