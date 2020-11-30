package com.woocommerce.android.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import com.woocommerce.android.helpers.MockingInterceptor
import javax.inject.Named

@Module
class InterceptorModuleTest {
    @Provides @IntoSet @Named("interceptors")
    fun provideMockingInterceptor(): Interceptor = MockingInterceptor()
}
