package com.woocommerce.android.media

import android.net.Uri
import android.webkit.MimeTypeMap
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.UrlUtils
import java.io.File

object MediaUploadUtils {
    fun mediaModelFromLocalUri(
        localSiteId: Int,
        uri: Uri,
        mediaStore: MediaStore
    ): MediaModel? {
        if (uri.path.isNullOrEmpty()) {
            WooLog.w(T.MEDIA, "mediaModelFromLocalUri > uri path is null")
            return null
        }

        val file = File(uri.path!!)
        if (!file.exists()) {
            WooLog.w(T.MEDIA, "mediaModelFromLocalUri > file does not exist")
            return null
        }

        val media = mediaStore.instantiateMediaModel()
        var filename = org.wordpress.android.fluxc.utils.MediaUtils.getFileName(uri.path)
        var fileExtension: String? = org.wordpress.android.fluxc.utils.MediaUtils.getExtension(uri.path)

        var mimeType = UrlUtils.getUrlMimeType(uri.toString())
        if (mimeType == null) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
            if (mimeType == null) {
                // Default to image jpeg
                mimeType = "image/jpeg"
            }
        }

        // If file extension is null, upload won't work on wordpress.com
        if (fileExtension.isNullOrBlank()) {
            fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            filename += "." + fileExtension!!
        }

        media.fileName = filename
        media.title = filename
        media.filePath = uri.path
        media.localSiteId = localSiteId
        media.fileExtension = fileExtension
        media.mimeType = mimeType
        media.uploadDate = DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000)

        return media
    }
}
