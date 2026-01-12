package org.wordpress.android.fluxc.network.rest.wpcom.media

import android.text.TextUtils
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse
import org.wordpress.android.fluxc.store.MediaUploadState
import javax.inject.Inject

class MediaResponseUtils
@Inject constructor() {
    /**
     * Creates a [MediaModel] list from a WP.com REST response to a request for all media.
     */
    fun getMediaListFromRestResponse(
        from: MultipleMediaResponse,
        localSiteId: Int
    ): List<MediaModel> {
        return from.media.mapNotNull {
            getMediaFromRestResponse(it, localSiteId)
        }
    }

    /**
     * Creates a [MediaModel] from a WP.com REST response to a fetch request.
     */
    fun getMediaFromRestResponse(from: MediaWPComRestResponse, siteId: Int) = MediaModel(
        id = 0,
        localSiteId = siteId,
        mediaId = from.ID,
        postId = from.post_ID,
        uploadDate = from.date,
        url = from.URL,
        thumbnailUrl = from.thumbnails?.let {
            if (!TextUtils.isEmpty(it.fmt_std)) {
                it.fmt_std
            } else {
                it.thumbnail
            }
        },
        fileName = from.file,
        filePath = null,
        mimeType = from.mime_type,
        title = StringEscapeUtils.unescapeHtml4(from.title),
        caption = StringEscapeUtils.unescapeHtml4(from.caption),
        description = StringEscapeUtils.unescapeHtml4(from.description),
        alt = StringEscapeUtils.unescapeHtml4(from.alt),
        uploadState = if (MediaWPComRestResponse.DELETED_STATUS == from.status) {
            MediaUploadState.DELETED.toString()
        } else {
            MediaUploadState.UPLOADED.toString()
        },
        markedLocallyAsFeatured = false
    )
}
