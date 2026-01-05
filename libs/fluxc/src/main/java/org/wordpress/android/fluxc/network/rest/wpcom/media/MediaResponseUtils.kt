package org.wordpress.android.fluxc.network.rest.wpcom.media

import android.text.TextUtils
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse
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
        siteId,
        from.ID,
        from.post_ID,
        from.date,
        from.URL,
        from.thumbnails?.let {
            if (!TextUtils.isEmpty(it.fmt_std)) {
                it.fmt_std
            } else {
                it.thumbnail
            }
        },
        from.file,
        from.mime_type,
        StringEscapeUtils.unescapeHtml4(from.title),
        StringEscapeUtils.unescapeHtml4(from.caption),
        StringEscapeUtils.unescapeHtml4(from.description),
        StringEscapeUtils.unescapeHtml4(from.alt),
        if (MediaWPComRestResponse.DELETED_STATUS == from.status) {
            MediaUploadState.DELETED
        } else {
            MediaUploadState.UPLOADED
        }
    )
}
