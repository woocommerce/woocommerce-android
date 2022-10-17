package com.woocommerce.android.di

import com.automattic.android.tracks.crashlogging.CrashLoggingOkHttpInterceptorProvider
import com.woocommerce.android.performance.WooRequestFormatter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import javax.inject.Named

@InstallIn(SingletonComponent::class)
@Module
class InterceptorModule {
    @Provides
    @IntoSet
    @Named("network-interceptors")
    fun provideMonitoring(): Interceptor = CrashLoggingOkHttpInterceptorProvider
        .createInstance(WooRequestFormatter)
}
