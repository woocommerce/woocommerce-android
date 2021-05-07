package com.woocommerce.android.di

import com.woocommerce.android.helpers.MockingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import javax.inject.Named

@InstallIn(SingletonComponent::class)
@Module
class InterceptorModuleTest {
    @Provides @IntoSet @Named("interceptors")
    fun provideMockingInterceptor(): Interceptor = MockingInterceptor()
}
