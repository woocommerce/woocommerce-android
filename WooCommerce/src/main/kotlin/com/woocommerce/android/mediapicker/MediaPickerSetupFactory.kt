package com.woocommerce.android.mediapicker

import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.CAMERA
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.SYSTEM_PICKER
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY
import org.wordpress.android.mediapicker.model.MediaTypes
import org.wordpress.android.mediapicker.source.device.DeviceMediaPickerSetup
import org.wordpress.android.mediapicker.source.wordpress.MediaLibraryPickerSetup
import java.security.InvalidParameterException
import javax.inject.Inject

class MediaPickerSetupFactory @Inject constructor() : MediaPickerSetup.Factory {
    override fun build(source: DataSource, isMultiSelectAllowed: Boolean): MediaPickerSetup {
        return when (source) {
            CAMERA -> DeviceMediaPickerSetup.buildCameraPicker()
            WP_MEDIA_LIBRARY -> MediaLibraryPickerSetup.build(
                mediaTypes = MediaTypes.IMAGES,
                canMultiSelect = isMultiSelectAllowed
            )
            DEVICE -> DeviceMediaPickerSetup.buildMediaPicker(
                mediaTypes = MediaTypes.IMAGES,
                canMultiSelect = true
            )
            SYSTEM_PICKER -> DeviceMediaPickerSetup.buildSystemPicker(
                mediaTypes = MediaTypes.IMAGES,
                canMultiSelect = false
            )
            else -> throw InvalidParameterException("${source.name} source is not supported")
        }
    }
}
