package org.wordpress.android.fluxc.wc.product

import org.junit.Test
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.WCProductReviewModel.AvatarSize.LARGE
import org.wordpress.android.fluxc.model.WCProductReviewModel.AvatarSize.MEDIUM
import org.wordpress.android.fluxc.model.WCProductReviewModel.AvatarSize.SMALL
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WCProductReviewModelTest {
    private val testLocalSiteId = 1

    @Test
    fun testDeserializeProductReviewModel() {
        val reviewJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/product-reviews.json")
        val reviews = ProductTestUtils.getProductReviewsFromJsonString(reviewJson, testLocalSiteId)
        assertTrue(reviews.isNotEmpty(), "Reviews should not be empty")

        val review = reviews[0]
        assertEquals(0, review.id)
        assertEquals("2019-07-09T15:48:07Z", review.dateCreated)
        assertEquals(18, review.remoteProductId)
        assertEquals("Johnny", review.reviewerName)
        assertEquals("johnny@gmail.com", review.reviewerEmail)
        assertEquals("<p>What a lovely cap!</p>\n", review.review)
        assertEquals(4, review.rating)
        assertEquals(false, review.verified)
        assertEquals(5499, review.remoteProductReviewId)
        assertNotNull(review.reviewerAvatarsJson)
        assertNotEquals("", review.reviewerAvatarsJson)
    }

    @Test
    fun testGetReviewerAvatarBySizeMap() {
        val reviewJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/product-reviews.json")
        val reviews = ProductTestUtils.getProductReviewsFromJsonString(reviewJson, testLocalSiteId)
        assertTrue(reviews.isNotEmpty(), "Reviews should not be empty")

        val review = reviews[0]
        assertEquals(
                "https://secure.gravatar.com/avatar/136c70acb946f0f37b12bb6fbfe56f2c?s=24&d=mm&r=g",
                review.reviewerAvatarUrlBySize[SMALL])
        assertEquals(
                "https://secure.gravatar.com/avatar/136c70acb946f0f37b12bb6fbfe56f2c?s=48&d=mm&r=g",
                review.reviewerAvatarUrlBySize[MEDIUM])
        assertEquals(
                "https://secure.gravatar.com/avatar/136c70acb946f0f37b12bb6fbfe56f2c?s=96&d=mm&r=g",
                review.reviewerAvatarUrlBySize[LARGE])
    }
}
