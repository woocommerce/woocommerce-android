package org.wordpress.android.fluxc.model

import androidx.room.Entity
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

// todo: as soon as SiteModel is migrated to Room, add foreign key constraint
@Entity(
    tableName = "ProductReviewEntity",
    primaryKeys = ["localSiteId", "remoteProductReviewId", "remoteProductId"],
)
data class WCProductReviewModel(
    val localSiteId: LocalId = LocalId(0),
    /**
     * Remote unique identifier for this product review
     */
    val remoteProductReviewId: RemoteId = RemoteId(0L), // The unique ID for this product review on the server,

    /**
     * Unique identifier for the product this review belongs to
     */
    val remoteProductId: RemoteId = RemoteId(0L),

    /**
     * The date the review was created, in UTC, ISO8601 formatted
     */
    val dateCreated: String = "",

    /**
     * Status of the review. Options: approved, hold, spam, unspam, trash, and untrash.
     */
    val status: String = "",

    /**
     * Name of the reviewer
     */
    @SerializedName("reviewer")
    val reviewerName: String = "",

    /**
     * Reviewer email address
     */
    val reviewerEmail: String = "",

    /**
     * The content of the review
     */
    val review: String = "",

    /**
     * Review rating (0 to 5)
     */
    val rating: Int = 0,

    /**
     * True if the reviewer purchased the product being reviewed, else false
     */
    val verified: Boolean = false,

    @SerializedName("reviewer_avatar_urls")
    val reviewerAvatarsJson: String = "",
) {
    companion object {
        private val json by lazy { Gson() }
    }

    /**
     * A mapping of reviewer avatar URL's by their size:
     * <ul>
     *     <li>SMALL = 24</li>
     *     <li>MEDIUM = 48</li>
     *     <li>LARGE = 95</li>
     * </ul>
     */
    val reviewerAvatarUrlBySize: Map<AvatarSize, String> by lazy {
        val result = mutableMapOf<AvatarSize, String>()
        if (reviewerAvatarsJson.isNotEmpty()) {
            json.fromJson(reviewerAvatarsJson, JsonElement::class.java).asJsonObject.entrySet().forEach {
                result[AvatarSize.getAvatarSizeForValue(it.key.toInt())] = it.value.asString
            }
        }
        result
    }
}
