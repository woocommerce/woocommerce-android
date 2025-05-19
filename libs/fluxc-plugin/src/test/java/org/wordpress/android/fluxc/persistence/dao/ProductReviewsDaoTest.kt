package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.wc.product.ProductTestUtils

@RunWith(RobolectricTestRunner::class)
class ProductReviewsDaoTest {

    private lateinit var sut: ProductReviewsDao
    private lateinit var database: WCAndroidDatabase

    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sut = database.productReviewsDao
    }


    @Test
    fun testInsertOrUpdateProductReview() = runTest {
        val review = getProductReviews(site.id)[0]
        assertNotNull(review)

        // Test inserting a product review
        sut.upsertProductReview(review)
        var savedReview = sut.getProductReview(
            site.localId(), review.remoteProductReviewId
        )
        assertNotNull(savedReview)
        assertEquals(review.remoteProductReviewId, savedReview.remoteProductReviewId)
        assertEquals(review.verified, savedReview.verified)
        assertEquals(review.rating, savedReview.rating)
        assertEquals(review.reviewerEmail, savedReview.reviewerEmail)
        assertEquals(review.review, savedReview.review)
        assertEquals(review.reviewerName, savedReview.reviewerName)
        assertEquals(review.remoteProductId, savedReview.remoteProductId)
        assertEquals(review.dateCreated, savedReview.dateCreated)
        assertEquals(review.localSiteId, savedReview.localSiteId)
        assertEquals(review.reviewerAvatarsJson, savedReview.reviewerAvatarsJson)

        // Test updating the same product review
        val updatedReview = review.copy(verified = !review.verified)
        sut.upsertProductReview(updatedReview)
        savedReview = sut.getProductReview(
            site.localId(), review.remoteProductReviewId
        )
        assertNotNull(savedReview)
        assertEquals(updatedReview.verified, savedReview.verified)
    }

    @Test
    fun testInsertOrUpdateProductReviews() = runTest {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())

        // Insert all product reviews
        sut.upsertProductReviews(reviews)

        assertThat(
            sut.getProductReviews(siteId = site.localId())
        ).containsExactlyInAnyOrderElementsOf(reviews)
    }

    @Test
    fun testGetProductReviewsForSite() = runTest {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())

        // Insert all product reviews
        sut.upsertProductReviews(reviews)

        // Get all product reviews for site and verify
        val savedReviewsExists = sut.getProductReviews(siteId = site.localId())
        assertEquals(reviews.size, savedReviewsExists.size)

        // Get all product reviews for a site that does not exist
        val savedReviews = sut.getProductReviews(siteId = (SiteModel().apply { id = 400 }).localId())
        assertEquals(0, savedReviews.size)
    }

    @Test
    fun testGetProductReviewsForProduct() = runTest {
        val productId = 18L // should be 3 products in the test products json config
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())
        sut.upsertProductReviews(reviews)

        // Get all reviews for existing product
        val savedReviewsForProductExisting =
            sut.getProductReviews(siteId = site.localId(), productId = RemoteId(productId))
        assertEquals(3, savedReviewsForProductExisting.size)

        // Get all reviews for non-existing product
        val savedReviewsForProduct = sut.getProductReviews(siteId = site.localId(), productId = RemoteId(
            400
        )
        )
        assertEquals(0, savedReviewsForProduct.size)
    }

     @Test
     fun testDeleteAllProductReviewsForSite() = runTest {
         val reviews = getProductReviews(site.id)
         assertTrue(reviews.isNotEmpty())
         sut.upsertProductReviews(reviews)

         // Verify products inserted
         var savedReviews = sut.getProductReviews(siteId = site.localId())
         assertEquals(reviews.size, savedReviews.size)

         // Delete all reviews for site and verify
         sut.deleteProductReviewsForSite(site.localId())
         savedReviews = sut.getProductReviews(siteId = site.localId())
         assertEquals(0, savedReviews.size)
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
         SiteSqlUtils().deleteSite(site)
         savedReviews = sut.getProductReviews(siteId = site.localId())
         assertEquals(0, savedReviews.size)
     }

    private fun getProductReviews(localSiteId: Int): List<WCProductReviewModel> {
        val reviewJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/product-reviews.json")
        return ProductTestUtils.getProductReviewsFromJsonString(reviewJson, localSiteId)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }
}
