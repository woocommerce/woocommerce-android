package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
@RawConstraints(
        "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE",
        "UNIQUE (REMOTE_PRODUCT_REVIEW_ID, REMOTE_PRODUCT_ID, LOCAL_SITE_ID) ON CONFLICT REPLACE"
)
data class WCProductReviewModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    companion object {
        private val json by lazy { Gson() }
    }

    @Column var localSiteId = 0

    /**
     * Remote unique identifier for this product review
     */
    @Column var remoteProductReviewId = 0L // The unique ID for this product review on the server

    /**
     * Unique identifier for the product this review belongs to
     */
    @Column var remoteProductId = 0L

    /**
     * The date the review was created, in UTC, ISO8601 formatted
     */
    @Column var dateCreated = ""

    /**
     * Status of the review. Options: approved, hold, spam, unspam, trash, and untrash.
     */
    @Column var status = ""

    /**
     * Name of the reviewer
     */
    @SerializedName("reviewer")
    @Column var reviewerName = ""

    /**
     * Reviewer email address
     */
    @Column var reviewerEmail = ""

    /**
     * The content of the review
     */
    @Column var review = ""

    /**
     * Review rating (0 to 5)
     */
    @Column var rating = 0

    /**
     * True if the reviewer purchased the product being reviewed, else false
     */
    @Column var verified = false

    @SerializedName("reviewer_avatar_urls")
    @Column var reviewerAvatarsJson = ""

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

    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = this.id

    enum class AvatarSize(val size: Int) {
        SMALL(24), MEDIUM(48), LARGE(96);

        companion object {
            fun getAvatarSizeForValue(size: Int): AvatarSize {
                return when (size) {
                    SMALL.size -> SMALL
                    MEDIUM.size -> MEDIUM
                    LARGE.size -> LARGE
                    else -> MEDIUM
                }
            }
        }
    }
}
