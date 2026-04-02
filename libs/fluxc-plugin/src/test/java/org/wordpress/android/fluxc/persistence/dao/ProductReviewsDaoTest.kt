package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.wc.product.ProductTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ProductReviewsDaoTest {
    @Rule
    @JvmField
    val wcDatabaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var sut: ProductReviewsDao
    private lateinit var siteSqlUtils: SiteSqlUtils

    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Before
    fun setUp() {
        sut = wcDatabaseRule.db.productReviewsDao
        siteSqlUtils = SiteSqlUtils()
    }

    @Test
    fun testInsertProductReview() = runTest {
        val review = getProductReviews(site.id)[0]

        sut.upsertProductReview(review)

        assertThat(
            sut.getProductReview(site.localId(), review.remoteProductReviewId)
        ).isEqualTo(review)
    }

    @Test
    fun testUpdateProductReview() = runTest {
        val review = getProductReviews(site.id)[0]
        sut.upsertProductReview(review)

        val updatedContent = "Updated content"
        val updatedReview = review.copy(review = updatedContent)
        sut.upsertProductReview(updatedReview)

        assertThat(
            sut.getProductReview(site.localId(), review.remoteProductReviewId)
        ).isEqualTo(updatedReview)
    }

    @Test
    fun testInsertAndQueryProductReviews() = runTest {
        val reviews = getProductReviews(site.id)

        sut.upsertProductReviews(reviews)

        assertThat(
            sut.getProductReviews(siteId = site.localId())
        ).containsExactlyInAnyOrderElementsOf(reviews)
    }

    @Test
    fun testQueryProductReviewsForNonExistingSite() = runTest {
        val reviews = getProductReviews(site.id)
        val nonExistingSiteId = LocalId(400)

        sut.upsertProductReviews(reviews)

        assertThat(
            sut.getProductReviews(siteId = nonExistingSiteId)
        ).isEmpty()
    }

    @Test
    fun testGetProductReviewsForProduct() = runTest {
        val productId = 18L
        val reviews = getProductReviews(site.id)

        sut.upsertProductReviews(reviews)

        assertThat(
            sut.getProductReviews(siteId = site.localId(), productId = RemoteId(productId))
        ).hasSize(3)
    }

    @Test
    fun testGetProductReviewsForNonExistingProduct() = runTest {
        val nonExistingProductId = RemoteId(400)
        val reviews = getProductReviews(site.id)

        sut.upsertProductReviews(reviews)

        assertThat(
            sut.getProductReviews(siteId = site.localId(), productId = nonExistingProductId)
        ).isEmpty()
    }

    @Test
    fun testDeleteAllProductReviewsForSite() = runTest {
        val reviews = getProductReviews(site.id)
        sut.upsertProductReviews(reviews)

        sut.deleteProductReviewsForSite(site.localId())

        assertThat(
            sut.getProductReviews(siteId = site.localId())
        ).isEmpty()
    }

    @Test
    @Ignore("This test is ignored until SiteModel is moved to Room and foreign key constraints are added")
    fun testDeleteSiteDeletesAllProductReviews() = runTest {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())
        sut.upsertProductReviews(reviews)

        // Verify products inserted
        var savedReviews = sut.getProductReviews(siteId = site.localId())
        assertEquals(reviews.size, savedReviews.size)

        // Delete site and verify reviews deleted via foreign key constraint
        siteSqlUtils.deleteSite(site)
        savedReviews = sut.getProductReviews(siteId = site.localId())
        assertEquals(0, savedReviews.size)
    }

    private fun getProductReviews(localSiteId: Int): List<WCProductReviewModel> {
        val reviewJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/product-reviews.json")
        return ProductTestUtils.getProductReviewsFromJsonString(reviewJson, localSiteId)
    }
}
