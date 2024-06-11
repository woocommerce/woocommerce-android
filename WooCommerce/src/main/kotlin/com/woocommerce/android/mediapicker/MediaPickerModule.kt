package com.woocommerce.android.mediapicker

import com.woocommerce.android.tools.SelectedSite
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjectionModule
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.login.di.LoginServiceModule
import org.wordpress.android.mediapicker.api.Log
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MimeTypeProvider
import org.wordpress.android.mediapicker.api.Tracker
import org.wordpress.android.mediapicker.loader.MediaLoaderFactory
import org.wordpress.android.mediapicker.source.wordpress.MediaLibrarySource
import org.wordpress.android.mediapicker.source.wordpress.util.NetworkUtilsWrapper

@InstallIn(SingletonComponent::class)
@Module(
    includes = [
        AndroidInjectionModule::class,
        LoginServiceModule::class
    ]
)
abstract class MediaPickerModule {
    companion object {
        @Provides
        fun provideMediaLibrarySourceFactory(
            mediaStore: MediaStore,
            dispatcher: Dispatcher,
            bgDispatcher: CoroutineDispatcher,
            networkUtilsWrapper: NetworkUtilsWrapper,
            site: SelectedSite
        ): MediaLibrarySource.Factory {
            return MediaLibrarySource.Factory(
                mediaStore,
                dispatcher,
                bgDispatcher,
                networkUtilsWrapper,
                site.get()
            )
        }
    }

    @Binds
    internal abstract fun provideMediaPickerLogger(logger: MediaPickerLogger): Log

    @Binds
    internal abstract fun provideMimeTypeSupportProvider(provider: MediaPickerMimeTypeProvider): MimeTypeProvider

    @Binds
    abstract fun bindMediaLoaderFactory(
        mediaPickerLoaderFactory: MediaPickerLoaderFactory
    ): MediaLoaderFactory

    @Binds
    abstract fun bindTracker(tracker: MediaPickerTracker): Tracker

    @Binds
    abstract fun bindMediaPickerSetupFactory(
        factory: MediaPickerSetupFactory
    ): MediaPickerSetup.Factory
}
