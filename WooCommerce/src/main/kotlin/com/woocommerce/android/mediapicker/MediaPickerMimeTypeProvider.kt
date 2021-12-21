package com.woocommerce.android.mediapicker

import org.wordpress.android.mediapicker.api.MimeTypeProvider
import javax.inject.Inject

class MediaPickerMimeTypeProvider @Inject constructor() : MimeTypeProvider {
    override val imageTypes: List<String> = listOf("JPEG", "PNG")
    override val videoTypes: List<String> = listOf("MP4")
    override val audioTypes: List<String> = emptyList()

    override fun isApplicationTypeSupported(applicationType: String): Boolean = true
    override fun isMimeTypeSupported(mimeType: String): Boolean = true
}
