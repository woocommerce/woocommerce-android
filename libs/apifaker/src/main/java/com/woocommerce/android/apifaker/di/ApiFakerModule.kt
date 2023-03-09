package com.woocommerce.android.apifaker.di

import android.content.Context
import com.woocommerce.android.apifaker.ApiFakerConfig
import com.woocommerce.android.apifaker.ApiFakerInterceptor
import com.woocommerce.android.apifaker.EndpointProcessor
import com.woocommerce.android.apifaker.db.ApiFakerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal class ApiFakerModule {
    @Provides
    @Singleton
    internal fun providesDatabase(context: Context) = ApiFakerDatabase.buildDb(context)

    @Provides
    internal fun providesEndpointDao(db: ApiFakerDatabase) = db.endpointDao

    @Provides
    @IntoSet
    @Named("interceptors")
    internal fun providesInterceptor(
        apiFakerConfig: ApiFakerConfig,
        endpointProcessor: EndpointProcessor
    ): Interceptor =
        ApiFakerInterceptor(apiFakerConfig, endpointProcessor)
}
