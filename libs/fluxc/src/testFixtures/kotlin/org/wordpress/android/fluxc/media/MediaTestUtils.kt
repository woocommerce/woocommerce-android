package org.wordpress.android.fluxc.media

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.utils.MediaUtils

object MediaTestUtils {
    fun generateMediaFromPath(
        localSiteId: Int = 0,
        mediaId: Long = 0,
        filePath: String = ""
    ): MediaModel {
        return MediaModel(
            id = 0,
            localSiteId = localSiteId,
            mediaId = mediaId,
            postId = 0L,
            uploadDate = null,
            url = "",
            thumbnailUrl = null,
            fileName = MediaUtils.getFileName(filePath),
            filePath = filePath,
            mimeType = MediaUtils.getMimeTypeForExtension(MediaUtils.getExtension(filePath)),
            title = MediaUtils.getFileName(filePath),
            caption = "",
            description = "",
            alt = "",
            uploadState = null,
            markedLocallyAsFeatured = false
        )
    }
}
