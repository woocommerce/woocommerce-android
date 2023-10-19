package com.woocommerce.android.mediapicker

import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.mediapicker.MediaPickerUtil.processDeviceMediaResult
import com.woocommerce.android.mediapicker.MediaPickerUtil.processMediaLibraryResult
import dagger.hilt.android.scopes.FragmentScoped
import org.wordpress.android.mediapicker.MediaPickerConstants
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import org.wordpress.android.mediapicker.ui.MediaPickerActivity
import javax.inject.Inject

@FragmentScoped
class MediaPickerHelper @Inject constructor(
    private val fragment: Fragment,
    private val mediaPickerSetupFactory: MediaPickerSetup.Factory
) {
    private lateinit var onMediaSelected: (String) -> Unit

    private val deviceLibraryLauncher = fragment.registerForActivityResult(StartActivityForResult()) {
        handleDeviceMediaResult(it)
    }

    fun showMediaPicker(source: DataSource, onMediaSelected: (String) -> Unit) {
        this.onMediaSelected = onMediaSelected
        showMediaPicker(source)
    }

    private val mediaLibraryLauncher = fragment.registerForActivityResult(StartActivityForResult()) {
        handleMediaLibraryPickerResult(it)
    }


    private fun showMediaPicker(source: DataSource) {
        val mediaPickerIntent = MediaPickerActivity.buildIntent(
            context = fragment.requireContext(),
            mediaPickerSetupFactory.build(source)
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
                onMediaSelected(mediaUris.first().toString())
            }
        }
    }

    private fun handleMediaLibraryPickerResult(result: ActivityResult) {
        result.processMediaLibraryResult()?.let { mediaItems ->
            onMediaSelected(mediaItems.map { it.source }.first())
        }
    }
}
