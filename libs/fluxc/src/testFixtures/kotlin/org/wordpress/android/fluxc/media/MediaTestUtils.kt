package org.wordpress.android.fluxc.media

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.utils.MimeTypes
import java.util.concurrent.atomic.AtomicInteger

object MediaTestUtils {
    private val nextId = AtomicInteger(1)
    private val mimeTypes = MimeTypes()

    @JvmStatic
    fun createLocalTestMedia(): LocalTestMediaBuilder {
        return LocalTestMediaBuilder()
    }

    @JvmStatic
    fun createRemoteTestMedia(): RemoteTestMediaBuilder {
        return RemoteTestMediaBuilder()
    }

    class LocalTestMediaBuilder internal constructor() {
        private var localSiteId: Int = 1
        private var mediaId: Long = 0L
        private var postId: Long = 0L
        private var uploadDate: String? = "2024-01-01T00:00:00+00:00"
        private var fileName: String? = "test-image.jpg"
        private var filePath: String? = "/test/test-image.jpg"
        private var mimeType: String? = "image/jpeg"
        private var title: String? = "Test Image"
        private var uploadState: MediaUploadState = MediaUploadState.UPLOADED

        fun localSiteId(value: Int) = apply { this.localSiteId = value }
        fun mediaId(value: Long) = apply { this.mediaId = value }
        fun postId(value: Long) = apply { this.postId = value }
        fun uploadDate(value: String?) = apply { this.uploadDate = value }
        fun fileName(value: String?) = apply { this.fileName = value }
        fun filePath(value: String?) = apply { this.filePath = value }
        fun mimeType(value: String?) = apply { this.mimeType = value }
        fun title(value: String?) = apply { this.title = value }
        fun uploadState(value: MediaUploadState) = apply { this.uploadState = value }

        fun build(): MediaModel {
            val media = MediaModel(
                this.localSiteId,
                this.uploadDate,
                this.fileName,
                this.filePath,
                this.mimeType,
                this.title,
                this.uploadState
            ).apply {
                setMediaId(this@LocalTestMediaBuilder.mediaId)
                setPostId(this@LocalTestMediaBuilder.postId)
            }
            media.id = nextId.getAndIncrement()
            return media
        }
    }

    class RemoteTestMediaBuilder internal constructor() {
        private var localSiteId: Int = 1
        private var mediaId: Long = 1L
        private var postId: Long = 0L
        private var uploadDate: String? = "2024-01-01T00:00:00+00:00"
        private var url: String = "https://example.com/test-image.jpg"
        private var fileName: String? = "test-image.jpg"
        private var mimeType: String? = "image/jpeg"
        private var title: String? = "Test Image"
        private var caption: String = ""
        private var description: String = ""
        private var alt: String = ""
        private var uploadState: MediaUploadState = MediaUploadState.UPLOADED

        fun localSiteId(value: Int) = apply { this.localSiteId = value }
        fun mediaId(value: Long) = apply { this.mediaId = value }
        fun postId(value: Long) = apply { this.postId = value }
        fun uploadDate(value: String?) = apply { this.uploadDate = value }
        fun url(value: String) = apply { this.url = value }
        fun fileName(value: String?) = apply { this.fileName = value }
        fun mimeType(value: String?) = apply { this.mimeType = value }
        fun title(value: String?) = apply { this.title = value }
        fun caption(value: String) = apply { this.caption = value }
        fun description(value: String) = apply { this.description = value }
        fun alt(value: String) = apply { this.alt = value }
        fun uploadState(value: MediaUploadState) = apply { this.uploadState = value }

        fun build(): MediaModel {
            val media = MediaModel(
                this.localSiteId,
                this.mediaId,
                this.postId,
                this.uploadDate,
                this.url,
                this.fileName,
                this.mimeType,
                this.title,
                this.caption,
                this.description,
                this.uploadState
            )
            media.id = nextId.getAndIncrement()
            return media
        }
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

        return if (description.isNullOrEmpty() && caption.isNullOrEmpty()) {
            createLocalTestMedia()
                .localSiteId(localSiteId)
                .mediaId(mediaId)
                .fileName(fileName)
                .filePath(filePath)
                .mimeType(mimeType)
                .title(title ?: fileName)
                .build()
        } else {
            createRemoteTestMedia()
                .localSiteId(localSiteId)
                .mediaId(mediaId)
                .fileName(fileName)
                .mimeType(mimeType)
                .title(title ?: fileName)
                .description(description ?: "")
                .caption(caption ?: "")
                .build()
        }
    }
}
