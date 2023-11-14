package com.woocommerce.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import javax.inject.Named

@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DebugInterceptorModule::class]
)
@Module
class DummyInterceptorModule {
    @Provides @IntoSet @Named("network-interceptors")
    fun provideNetworkInterceptor(): Interceptor = Interceptor { it.proceed(it.request()) }
}
