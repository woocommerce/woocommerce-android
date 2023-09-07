package com.woocommerce.android.mediapicker

import org.wordpress.android.fluxc.utils.MimeTypes
import org.wordpress.android.mediapicker.api.MimeTypeProvider
import javax.inject.Inject

class MediaPickerMimeTypeProvider @Inject constructor() : MimeTypeProvider {
    private val supportedMimes = MimeTypes()

    override val imageTypes: List<String> = supportedMimes.getImageTypesOnly().asList()
    override val videoTypes: List<String> = supportedMimes.getVideoTypesOnly().asList()
    override val audioTypes: List<String> = supportedMimes.getAudioTypesOnly().asList()
    override val documentTypes: List<String> = supportedMimes.getDocumentTypesOnly().asList()

    override fun isApplicationTypeSupported(applicationType: String): Boolean = true
    override fun isMimeTypeSupported(mimeType: String): Boolean = true
}
