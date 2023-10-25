package com.woocommerce.android.mediapicker

import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import com.woocommerce.android.mediapicker.MediaPickerUtil.processDeviceMediaResult
import com.woocommerce.android.mediapicker.MediaPickerUtil.processMediaLibraryResult
import dagger.hilt.android.scopes.FragmentScoped
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import org.wordpress.android.mediapicker.model.MediaTypes
import org.wordpress.android.mediapicker.ui.MediaPickerActivity
import javax.inject.Inject

@FragmentScoped
class MediaPickerHelper @Inject constructor(
    private val fragment: Fragment,
    private val mediaPickerSetupFactory: MediaPickerSetup.Factory
) {
    private val deviceLibraryLauncher = fragment.registerForActivityResult(StartActivityForResult()) {
        handleDeviceMediaResult(it)
    }

    private val mediaLibraryLauncher = fragment.registerForActivityResult(StartActivityForResult()) {
        handleMediaLibraryPickerResult(it)
    }

    fun showMediaPicker(source: DataSource) {
        val mediaPickerIntent = MediaPickerActivity.buildIntent(
            context = fragment.requireContext(),
            mediaPickerSetupFactory.build(
                source = source,
                mediaTypes = MediaTypes.IMAGES
            )
        )

        if (source == DataSource.WP_MEDIA_LIBRARY) {
            mediaLibraryLauncher.launch(mediaPickerIntent)
        } else {
            deviceLibraryLauncher.launch(mediaPickerIntent)
        }
    }

    private fun handleDeviceMediaResult(result: ActivityResult) {
        result.processDeviceMediaResult()?.let { mediaUris ->
            if (mediaUris.isNotEmpty()) {
                (fragment as MediaPickerResultHandler).onMediaSelected(mediaUris.first().toString())
            }
        }
    }

    private fun handleMediaLibraryPickerResult(result: ActivityResult) {
        result.processMediaLibraryResult()?.let { mediaItems ->
            (fragment as MediaPickerResultHandler).onMediaSelected(mediaItems.map { it.source }.first())
        }
    }

    interface MediaPickerResultHandler {
        fun onMediaSelected(mediaUri: String)
    }
}
