package com.woocommerce.android.mediapicker.medialibrary

import com.woocommerce.android.R
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY
import org.wordpress.android.mediapicker.api.MediaPickerSetup.SearchMode.HIDDEN
import org.wordpress.android.mediapicker.model.MediaTypes

object MediaLibraryPickerSetup {
    fun build(mediaTypes: MediaTypes, canMultiSelect: Boolean): MediaPickerSetup {
        return MediaPickerSetup(
            primaryDataSource = WP_MEDIA_LIBRARY,
            isMultiSelectEnabled = canMultiSelect,
            allowedTypes = mediaTypes.allowedTypes,
            areResultsQueued = false,
            searchMode = HIDDEN,
            title = R.string.media_library_title
        )
    }
}
