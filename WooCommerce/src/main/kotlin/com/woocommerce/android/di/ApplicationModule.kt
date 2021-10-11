package com.woocommerce.android.di

import android.content.Context
import com.woocommerce.android.tools.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjectionModule
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.wordpress.android.login.di.LoginServiceModule
import org.wordpress.android.mediapicker.api.MediaInsertHandlerFactory
import org.wordpress.android.mediapicker.api.MimeTypeSupportProvider
import org.wordpress.android.mediapicker.loader.MediaLoaderFactory
import org.wordpress.android.mediapicker.util.Log
import org.wordpress.android.mediapicker.util.Tracker
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@InstallIn(SingletonComponent::class)
@Module(
    includes = [
        AndroidInjectionModule::class,
        LoginServiceModule::class
    ]
)
abstract class ApplicationModule {
    // Expose Application as an injectable context
    @Binds
    internal abstract fun bindContext(@ApplicationContext context: Context): Context

    @Binds
    internal abstract fun provideMediaPickerLogger(logger: MediaPickerLogger): Log

    @Binds
    internal abstract fun provideMimeTypeSupportProvider(provider: MimeTypeProvider): MimeTypeSupportProvider

    @Binds
    abstract fun bindMediaLoaderFactory(
        sampleMediaLoaderFactory: WooMediaLoaderFactory
    ): MediaLoaderFactory

    @Binds
    abstract fun bindMediaInsertHandlerFactory(
        sampleMediaLoaderFactory: WooMediaInsertHandlerFactory
    ): MediaInsertHandlerFactory

    @Binds
    abstract fun bindTracker(tracker: MediaPickerTracker): Tracker

    @Binds
    abstract fun bindCoroutineScope(@AppCoroutineScope scope: CoroutineScope): CoroutineScope

    companion object {
        @Provides
        @AppCoroutineScope
        fun provideAppCoroutineScope(dispatcher: CoroutineDispatcher): CoroutineScope =
            CoroutineScope(SupervisorJob() + dispatcher)

        @Provides
        fun provideBackgroundDispatcher(): CoroutineDispatcher {
            return Dispatchers.Default
        }
    }
}

@Qualifier
@MustBeDocumented
@Retention(RUNTIME)
annotation class AppCoroutineScope
