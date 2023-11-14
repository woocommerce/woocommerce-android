package com.woocommerce.android.mediapicker

import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.CAMERA
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.SYSTEM_PICKER
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY
import org.wordpress.android.mediapicker.model.MediaTypes
import org.wordpress.android.mediapicker.setup.SystemMediaPickerSetup
import org.wordpress.android.mediapicker.source.camera.CameraMediaPickerSetup
import org.wordpress.android.mediapicker.source.wordpress.MediaLibraryPickerSetup
import java.security.InvalidParameterException
import javax.inject.Inject

class MediaPickerSetupFactory @Inject constructor() : MediaPickerSetup.Factory {
    override fun build(
        source: DataSource,
        mediaTypes: MediaTypes,
        isMultiSelectAllowed: Boolean
    ): MediaPickerSetup {
        return when (source) {
            CAMERA -> CameraMediaPickerSetup.build()
            WP_MEDIA_LIBRARY -> MediaLibraryPickerSetup.build(
                mediaTypes = mediaTypes,
                canMultiSelect = isMultiSelectAllowed
            )
            SYSTEM_PICKER -> SystemMediaPickerSetup.build(mediaTypes = mediaTypes, canMultiSelect = false)

            else -> throw InvalidParameterException("${source.name} source is not supported")
        }
    }
}
