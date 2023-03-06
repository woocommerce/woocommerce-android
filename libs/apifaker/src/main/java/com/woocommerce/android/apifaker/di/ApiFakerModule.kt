package com.woocommerce.android.apifaker.di

import android.content.Context
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

@Module
@InstallIn(SingletonComponent::class)
abstract class ApiFakerModule {
    companion object {
        @Provides
        @Singleton
        internal fun providesDatabase(context: Context) = ApiFakerDatabase.buildDb(context)

        @Provides
        internal fun providesEndpointDao(db: ApiFakerDatabase) = db.endpointDao

        @Provides
        @IntoSet
        @Named("interceptors")
        internal fun providesInterceptor(endpointProcessor: EndpointProcessor): Interceptor =
            ApiFakerInterceptor(endpointProcessor)
    }
}
