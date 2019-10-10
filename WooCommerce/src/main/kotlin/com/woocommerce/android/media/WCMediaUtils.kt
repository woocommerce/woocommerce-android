package com.woocommerce.android.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.UrlUtils
import java.io.File

object MediaUploadUtils {
    fun mediaModelFromLocalUri(
        context: Context,
        localSiteId: Int,
        uri: Uri,
        mediaStore: MediaStore
    ): MediaModel? {
        val path = MediaUtils.getRealPathFromURI(context, uri)
        val file = File(path)
        if (!file.exists()) {
            return null
        }

        val media = mediaStore.instantiateMediaModel()
        var filename = org.wordpress.android.fluxc.utils.MediaUtils.getFileName(path)
        var fileExtension: String? = org.wordpress.android.fluxc.utils.MediaUtils.getExtension(path)

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
        media.filePath = path
        media.localSiteId = localSiteId
        media.fileExtension = fileExtension
        media.mimeType = mimeType
        media.uploadDate = DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000)

        media.setUploadState(MediaModel.MediaUploadState.UPLOADING) // ??

        return media
    }
}
