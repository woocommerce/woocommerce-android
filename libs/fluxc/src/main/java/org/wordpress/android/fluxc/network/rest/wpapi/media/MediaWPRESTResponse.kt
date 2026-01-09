@file:Suppress("MatchingDeclarationName")

package org.wordpress.android.fluxc.network.rest.wpapi.media

import com.google.gson.annotations.SerializedName
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.network.rest.JsonObjectOrNull
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse
import org.wordpress.android.fluxc.store.MediaUploadState
import org.wordpress.android.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Locale

data class MediaWPRESTResponse(
    val id: Long,
    @SerializedName("date_gmt") val dateGmt: String,
    val status: String,
    val title: Attribute,
    val post: Long? = null,
    val description: Attribute,
    val caption: Attribute,
    @SerializedName("alt_text") val altText: String,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("media_details") val mediaDetails: MediaDetails?,
    @SerializedName("source_url") val sourceURL: String?
) {
    data class Attribute(
        val rendered: String
    )

    data class MediaDetails(
        val file: String?,
        val sizes: Sizes?
    ) : JsonObjectOrNull()

    data class Sizes(
        val thumbnail: ImageSize?
    ) : JsonObjectOrNull()

    data class ImageSize(
        @SerializedName("source_url") val sourceURL: String
    )
}

fun MediaWPRESTResponse.toMediaModel(localSiteId: Int) = MediaModel(
    id = 0,
    localSiteId = localSiteId,
    localPostId = 0,
    mediaId = id,
    postId = post ?: 0L,
    uploadDate = DateTimeUtils.iso8601FromDate(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).parse(dateGmt)
    ),
    url = sourceURL.orEmpty(),
    thumbnailUrl = mediaDetails?.sizes?.thumbnail?.sourceURL,
    fileName = mediaDetails?.file,
    filePath = null,
    mimeType = mimeType,
    title = StringEscapeUtils.unescapeHtml4(title.rendered),
    caption = StringEscapeUtils.unescapeHtml4(caption.rendered),
    description = StringEscapeUtils.unescapeHtml4(description.rendered),
    alt = StringEscapeUtils.unescapeHtml4(altText),
    uploadState = if (MediaWPComRestResponse.DELETED_STATUS == status) {
        MediaUploadState.DELETED.toString()
    } else {
        MediaUploadState.UPLOADED.toString()
    },
    markedLocallyAsFeatured = false
)
