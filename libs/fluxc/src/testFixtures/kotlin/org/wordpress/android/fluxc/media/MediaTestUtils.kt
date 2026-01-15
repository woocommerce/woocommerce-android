package org.wordpress.android.fluxc.media

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.utils.MimeTypes
import java.util.concurrent.atomic.AtomicInteger

object MediaTestUtils {
    private val nextId = AtomicInteger(1)
    private val mimeTypes = MimeTypes()

    @JvmStatic
    @JvmOverloads
    fun createTestMedia(
        localSiteId: Int = 1,
        mediaId: Long = 0L,
        postId: Long = 0L,
        uploadDate: String? = null,
        url: String = "",
        thumbnailUrl: String? = null,
        fileName: String? = null,
        filePath: String? = null,
        mimeType: String? = null,
        title: String? = null,
        caption: String = "",
        description: String = "",
        alt: String = "",
        uploadState: MediaUploadState = MediaUploadState.UPLOADED
    ): MediaModel {
        val media = MediaModel(
            localSiteId,
            mediaId,
            postId,
            uploadDate,
            url,
            thumbnailUrl,
            fileName,
            mimeType,
            title,
            caption,
            description,
            alt,
            uploadState
        )
        media.id = nextId.getAndIncrement()
        if (filePath != null) {
            media.filePath = filePath
        }
        return media
    }

    @JvmStatic
    @JvmOverloads
    fun generateMediaFromPath(
        localSiteId: Int,
        mediaId: Long,
        filePath: String,
        title: String? = null,
        description: String? = null,
        caption: String? = null
    ): MediaModel {
        val fileName = filePath.substringAfterLast('/')
        val extension = fileName.substringAfterLast('.', "")
        val mimeType = mimeTypes.getMimeTypeForExtension(extension)

        return createTestMedia(
            localSiteId = localSiteId,
            mediaId = mediaId,
            fileName = fileName,
            filePath = filePath,
            mimeType = mimeType,
            title = title ?: fileName,
            description = description ?: "",
            caption = caption ?: ""
        )
    }
}
