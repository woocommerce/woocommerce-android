package com.woocommerce.android.mediapicker

import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import com.woocommerce.android.mediapicker.MediaPickerUtil.processDeviceMediaResult
import com.woocommerce.android.mediapicker.MediaPickerUtil.processMediaLibraryResult
import dagger.hilt.android.scopes.FragmentScoped
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import org.wordpress.android.mediapicker.model.MediaType
import org.wordpress.android.mediapicker.model.MediaType.IMAGE
import org.wordpress.android.mediapicker.model.MediaType.VIDEO
import org.wordpress.android.mediapicker.model.MediaTypes
import org.wordpress.android.mediapicker.model.MediaUri
import org.wordpress.android.mediapicker.ui.MediaPickerActivity
import javax.inject.Inject

@FragmentScoped
class MediaPickerHelper @Inject constructor(
    private val fragment: Fragment,
    private val mediaPickerSetupFactory: MediaPickerSetup.Factory
) {

    private val photoPicker = fragment.registerForActivityResult(PickVisualMedia()) { uri ->
        handlePhotoPickerResult(uri)
    }

    private val mediaLibraryLauncher = fragment.registerForActivityResult(StartActivityForResult()) {
        handleMediaLibraryPickerResult(it)
    }

    fun showMediaPicker(source: DataSource) {
        if (source == DataSource.WP_MEDIA_LIBRARY) {
            val mediaPickerIntent = MediaPickerActivity.buildIntent(
                context = fragment.requireContext(),
                mediaPickerSetupFactory.build(
                    source = source,
                    mediaTypes = MediaTypes.IMAGES
                )
            )
            mediaLibraryLauncher.launch(mediaPickerIntent)
        } else {
            photoPicker.launch(
                PickVisualMediaRequest(MediaTypes.IMAGES.allowedTypes.toPhotoPickerTypes())
            )
        }
    }

    private fun handlePhotoPickerResult(uri: Uri?) {
        uri?.let {
            (fragment as MediaPickerResultHandler).onMediaSelected(it.toString())
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
