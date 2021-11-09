package com.woocommerce.android.mediapicker

import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjectionModule
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.login.di.LoginServiceModule
import org.wordpress.android.mediapicker.api.MimeTypeProvider
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
abstract class MediaPickerModule {
    @Binds
    internal abstract fun provideMediaPickerLogger(logger: MediaPickerLogger): Log

    @Binds
    internal abstract fun provideMimeTypeSupportProvider(provider: WooMimeTypeProvider): MimeTypeProvider

    @Binds
    abstract fun bindMediaLoaderFactory(
        sampleMediaLoaderFactory: WooMediaLoaderFactory
    ): MediaLoaderFactory

    @Binds
    abstract fun bindTracker(tracker: MediaPickerTracker): Tracker
}

@Qualifier
@MustBeDocumented
@Retention(RUNTIME)
annotation class AppCoroutineScope
