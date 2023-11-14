package com.woocommerce.android.mediapicker

import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.loader.MediaLoader
import org.wordpress.android.mediapicker.loader.MediaLoaderFactory
import org.wordpress.android.mediapicker.source.device.DeviceMediaSource
import org.wordpress.android.mediapicker.source.wordpress.MediaLibrarySource
import javax.inject.Inject

// A factory class responsible for building an image-loader class, which is specific to a source.
class MediaPickerLoaderFactory @Inject constructor(
    private val deviceMediaSourceFactory: DeviceMediaSource.Factory,
    private val mediaLibrarySourceFactory: MediaLibrarySource.Factory
) : MediaLoaderFactory {
    override fun build(mediaPickerSetup: MediaPickerSetup): MediaLoader {
        return when (mediaPickerSetup.primaryDataSource) {
            MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY -> {
                mediaLibrarySourceFactory.build(mediaPickerSetup.allowedTypes)
            }
            else -> deviceMediaSourceFactory.build(mediaPickerSetup.allowedTypes)
        }.toMediaLoader()
    }
}
