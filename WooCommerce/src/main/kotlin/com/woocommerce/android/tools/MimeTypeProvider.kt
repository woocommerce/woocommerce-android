package com.woocommerce.android.tools

import android.webkit.MimeTypeMap
import org.wordpress.android.mediapicker.api.MimeTypeSupportProvider
import javax.inject.Inject

class MimeTypeProvider @Inject constructor() : MimeTypeSupportProvider {
    override fun isMimeTypeSupportedBySitePlan(siteId: Long, mimeType: String): Boolean {
        return true
    }

    override fun isSupportedApplicationType(mimeType: String): Boolean {
        return true
    }

    override fun isSupportedMimeType(mimeType: String): Boolean {
        return true
    }

    override fun getMimeTypeForExtension(fileExtension: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension) ?: ""
    }

    override fun getExtensionForMimeType(mimeType: String): String {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
    }

    override fun getImageTypesOnly(): List<String> {
        return listOf("JPEG", "PNG")
    }

    override fun getVideoTypesOnly(): List<String> {
        return emptyList()
    }

    override fun getAudioTypesOnly(): List<String> {
        return emptyList()
    }

    override fun getVideoAndImagesTypes(): List<String> {
        return listOf("JPEG", "PNG", "AVI", "MPEG")
    }

    override fun getAllTypes(): List<String> {
        return listOf("JPEG", "PNG", "AVI", "MPEG", "WAV")
    }
}
