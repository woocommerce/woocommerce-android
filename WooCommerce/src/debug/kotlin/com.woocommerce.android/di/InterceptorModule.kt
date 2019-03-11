package com.woocommerce.android.di

import com.facebook.stetho.okhttp3.StethoInterceptor

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import javax.inject.Named

@Module
class InterceptorModule {
    @Provides @IntoSet @Named("network-interceptors")
    fun provideNetworkInterceptor(): Interceptor = StethoInterceptor()
}
