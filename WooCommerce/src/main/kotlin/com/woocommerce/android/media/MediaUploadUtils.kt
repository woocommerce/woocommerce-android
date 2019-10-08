package com.woocommerce.android.media

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.util.helpers.MediaFile

object MediaUploadUtils {
    fun mediaModelFromMediaFile(file: MediaFile): MediaModel {
        val mediaModel = MediaModel()
        mediaModel.fileName = file.fileName
        mediaModel.filePath = file.filePath
        mediaModel.fileExtension = org.wordpress.android.fluxc.utils.MediaUtils.getExtension(file.filePath)
        mediaModel.mimeType = file.mimeType
        mediaModel.thumbnailUrl = file.thumbnailURL
        mediaModel.url = file.fileURL
        mediaModel.title = file.title
        mediaModel.description = file.description
        mediaModel.caption = file.caption
        mediaModel.mediaId = if (file.mediaId != null) java.lang.Long.valueOf(file.mediaId) else 0
        mediaModel.id = file.id
        mediaModel.uploadState = file.uploadState
        mediaModel.localSiteId = Integer.valueOf(file.blogId)
        return mediaModel
    }
}
