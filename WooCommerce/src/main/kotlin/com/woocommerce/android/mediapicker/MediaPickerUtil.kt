package com.woocommerce.android.mediapicker

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.model.Product
import com.woocommerce.android.util.WooLog
import org.wordpress.android.mediapicker.MediaPickerConstants
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.model.MediaItem
import org.wordpress.android.util.DateTimeUtils
import java.security.InvalidParameterException

// A helper class containing the shared boilerplate code for extracting the Media picker result from an Intent
object MediaPickerUtil {
    fun ActivityResult.processDeviceMediaResult(): List<Uri>? = processIntent()

    fun ActivityResult.processMediaLibraryResult(): List<Product.Image>? = processIntent()

    @Suppress("UNCHECKED_CAST")
    private fun <T> ActivityResult.processIntent(): List<T>? {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            val sourceExtra = data?.getStringExtra(MediaPickerConstants.EXTRA_MEDIA_SOURCE)
            if (sourceExtra != null) {
                val data = data!!.extras!!
                val list = when (val dataSource = MediaPickerSetup.DataSource.valueOf(sourceExtra)) {
                    MediaPickerSetup.DataSource.SYSTEM_PICKER,
                    MediaPickerSetup.DataSource.DEVICE -> {
                        handleDeviceMediaPickerResult(data)
                    }
                    MediaPickerSetup.DataSource.CAMERA -> {
                        handleDeviceMediaPickerResult(data)
                    }
                    MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY -> {
                        handleMediaLibraryPickerResult(data)
                    }
                    else -> throw InvalidParameterException("${dataSource.name} is not a supported data source")
                }

                if (list.isEmpty()) {
                    WooLog.w(WooLog.T.MEDIA, "Media picker returned empty result")
                }

                return list as List<T>
            } else {
                WooLog.w(WooLog.T.MEDIA, "Media picker returned empty media source")
            }
        }
        return null
    }

    private fun handleDeviceMediaPickerResult(data: Bundle): List<Uri> {
        return data.getStringArray(MediaPickerConstants.EXTRA_MEDIA_URIS)?.asList()?.map { Uri.parse(it) }
            ?: emptyList()
    }

    private fun handleMediaLibraryPickerResult(data: Bundle): List<Product.Image> {
        return data.getParcelableArrayList<MediaItem.Identifier.RemoteMedia>(MediaPickerConstants.EXTRA_REMOTE_MEDIA)
            ?.map { Product.Image(it.id, it.name, it.url, DateTimeUtils.dateFromIso8601(it.date)) }
            ?: emptyList()
    }
}
