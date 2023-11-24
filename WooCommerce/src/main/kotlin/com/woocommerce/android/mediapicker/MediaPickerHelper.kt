package com.woocommerce.android.mediapicker

import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.mediapicker.MediaPickerUtil.processDeviceMediaResult
import com.woocommerce.android.mediapicker.MediaPickerUtil.processMediaLibraryResult
import com.woocommerce.android.model.Product
import dagger.hilt.android.scopes.FragmentScoped
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.CAMERA
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.SYSTEM_PICKER
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY
import org.wordpress.android.mediapicker.model.MediaTypes
import org.wordpress.android.mediapicker.model.MediaTypes.IMAGES
import org.wordpress.android.mediapicker.ui.MediaPickerActivity
import javax.inject.Inject

@FragmentScoped
class MediaPickerHelper @Inject constructor(
    private val fragment: Fragment,
    private val mediaPickerSetupFactory: MediaPickerSetup.Factory
) {

    private val photoPicker = fragment.registerForActivityResult(PickVisualMedia()) { uri ->
        handlePhotoPickerResult(uri?.let { listOf(it) } ?: emptyList())
    }

    private val multiPhotoPicker = fragment.registerForActivityResult(PickMultipleVisualMedia()) { uris ->
        handlePhotoPickerResult(uris)
    }

    private val mediaLibraryLauncher = fragment.registerForActivityResult(StartActivityForResult()) {
        handleMediaLibraryPickerResult(it)
    }

    private val cameraLauncher = fragment.registerForActivityResult(StartActivityForResult()) {
        handleMediaPickerResult(it, AnalyticsTracker.IMAGE_SOURCE_CAMERA)
    }

    private val systemMediaPickerLauncher = fragment.registerForActivityResult(StartActivityForResult()) {
        handleMediaPickerResult(it, AnalyticsTracker.IMAGE_SOURCE_DEVICE)
    }

    fun showMediaPicker(source: DataSource, allowMultiSelect: Boolean = false, mediaTypes: MediaTypes = IMAGES) {
        when (source) {
            WP_MEDIA_LIBRARY -> launchWPMediaLibrary(source, allowMultiSelect, mediaTypes)
            CAMERA -> launchCamera()
            DEVICE -> launchPhotoPicker(allowMultiSelect, mediaTypes)
            SYSTEM_PICKER -> launchSystemMediaPicker(mediaTypes)
            else -> throw IllegalArgumentException("Unsupported data source: $source")
        }
    }

    private fun launchSystemMediaPicker(mediaTypes: MediaTypes) {
        val intent = MediaPickerActivity.buildIntent(
            fragment.requireContext(),
            mediaPickerSetupFactory.build(
                source = SYSTEM_PICKER,
                mediaTypes = mediaTypes
            )
        )

        systemMediaPickerLauncher.launch(intent)
    }

    private fun launchPhotoPicker(allowMultiSelect: Boolean, mediaTypes: MediaTypes) {
        if (allowMultiSelect) {
            multiPhotoPicker.launch(
                PickVisualMediaRequest(mediaTypes.allowedTypes.toPhotoPickerTypes())
            )
        } else {
            photoPicker.launch(
                PickVisualMediaRequest(mediaTypes.allowedTypes.toPhotoPickerTypes())
            )
        }
    }

    private fun launchCamera() {
        val intent = MediaPickerActivity.buildIntent(
            fragment.requireContext(),
            mediaPickerSetupFactory.build(CAMERA)
        )

        cameraLauncher.launch(intent)
    }

    private fun launchWPMediaLibrary(
        source: DataSource,
        allowMultiSelect: Boolean,
        mediaTypes: MediaTypes
    ) {
        val mediaPickerIntent = MediaPickerActivity.buildIntent(
            context = fragment.requireContext(),
            mediaPickerSetupFactory.build(
                source = source,
                mediaTypes = mediaTypes,
                isMultiSelectAllowed = allowMultiSelect
            )
        )
        mediaLibraryLauncher.launch(mediaPickerIntent)
    }

    private fun handlePhotoPickerResult(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            (fragment as MediaPickerResultHandler).onDeviceMediaSelected(uris, AnalyticsTracker.IMAGE_SOURCE_DEVICE)
        }
    }

    private fun handleMediaLibraryPickerResult(result: ActivityResult) {
        result.processMediaLibraryResult()?.let { mediaItems ->
            (fragment as MediaPickerResultHandler).onWPMediaSelected(mediaItems)
        }
    }

    private fun handleMediaPickerResult(result: ActivityResult, source: String) {
        result.processDeviceMediaResult()?.let { uris ->
            (fragment as MediaPickerResultHandler).onDeviceMediaSelected(uris, source)
        }
    }

    interface MediaPickerResultHandler {
        fun onDeviceMediaSelected(imageUris: List<Uri>, source: String)
        fun onWPMediaSelected(images: List<Product.Image>)
    }
}
