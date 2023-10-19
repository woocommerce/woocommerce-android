package com.woocommerce.android.widgets

import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
    private lateinit var onMediaSelected: (Uri?) -> Unit

    private val resultLauncher = fragment.registerForActivityResult(StartActivityForResult()) {
        handleMediaPickerResult(it)
    }

    fun showMediaPicker(source: DataSource, onMediaSelected: (Uri?) -> Unit) {
        this.onMediaSelected = onMediaSelected
        showMediaPicker(source)
    }

    private fun showMediaPicker(source: DataSource) {
        val mediaPickerIntent = MediaPickerActivity.buildIntent(
            context = fragment.requireContext(),
            mediaPickerSetupFactory.build(source)
        )
        resultLauncher.launch(mediaPickerIntent)
    }

    private fun handleMediaPickerResult(result: ActivityResult) {
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.extras?.let { extra ->
                val uri = (extra.getStringArray(MediaPickerConstants.EXTRA_MEDIA_URIS))
                    ?.map { Uri.parse(it) }
                    ?.first()

                onMediaSelected(uri)
            }
        }
    }
}
