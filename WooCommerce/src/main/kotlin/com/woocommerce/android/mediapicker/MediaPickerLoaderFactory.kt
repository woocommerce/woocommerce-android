package com.woocommerce.android.mediapicker

import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.loader.MediaLoader
import org.wordpress.android.mediapicker.loader.MediaLoaderFactory
import org.wordpress.android.mediapicker.source.wordpress.MediaLibrarySource
import javax.inject.Inject

// A factory class responsible for building an image-loader class, which is specific to a source.
class MediaPickerLoaderFactory @Inject constructor(
    private val mediaLibrarySourceFactory: MediaLibrarySource.Factory
) : MediaLoaderFactory {
    override fun build(mediaPickerSetup: MediaPickerSetup): MediaLoader {
        return mediaLibrarySourceFactory.build(mediaPickerSetup.allowedTypes).toMediaLoader()
    }
}
