package com.woocommerce.android.apifaker.di

import android.content.Context
import androidx.compose.material.SnackbarHostState
import com.google.gson.GsonBuilder
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
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.RUNTIME

@Module
@InstallIn(SingletonComponent::class)
object ApiFakerModule {
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
    ): Interceptor = ApiFakerInterceptor(apiFakerConfig, endpointProcessor)

    @Provides
    @Singleton
    internal fun providesSnackbarHostState() = SnackbarHostState()

    @Provides
    @Singleton
    @ApiFakerGson
    internal fun providesGson() = GsonBuilder().setPrettyPrinting().create()
}

@Qualifier
@MustBeDocumented
@Retention(RUNTIME)
internal annotation class ApiFakerGson
