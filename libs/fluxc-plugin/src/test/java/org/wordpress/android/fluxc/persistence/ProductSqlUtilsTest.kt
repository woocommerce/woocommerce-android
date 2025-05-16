package org.wordpress.android.fluxc.persistence

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.TestSiteSqlUtils
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.wc.product.ProductTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("LargeClass")
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ProductSqlUtilsTest {
    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        WCProductReviewModel::class.java,
                        WCProductShippingClassModel::class.java,
                        SiteModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()

        // Insert the site into the db so it's available later for product
        // reviews
        TestSiteSqlUtils.siteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun testInsertOrUpdateProductShippingClass() {
        val shippingClass = ProductTestUtils.generateProductShippingClassList(site.id)[0]
        assertNotNull(shippingClass)

        // Test inserting a product shipping class
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClass(shippingClass)
        assertEquals(1, rowsAffected)
        var savedShippingClassList = ProductSqlUtils
                .getProductShippingClassListForSite(site.id)
        assertEquals(savedShippingClassList.size, 1)
        assertEquals(savedShippingClassList[0].localSiteId, shippingClass.localSiteId)
        assertEquals(savedShippingClassList[0].remoteShippingClassId, shippingClass.remoteShippingClassId)
        assertEquals(savedShippingClassList[0].name, shippingClass.name)
        assertEquals(savedShippingClassList[0].slug, shippingClass.slug)
        assertEquals(savedShippingClassList[0].description, shippingClass.description)

        // Test updating the same product shipping class
        shippingClass.apply {
            name = "Test shipping class"
        }
        rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClass(shippingClass)
        assertEquals(1, rowsAffected)
        savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(savedShippingClassList.size, 1)
        assertEquals(savedShippingClassList[0].localSiteId, shippingClass.localSiteId)
        assertEquals(savedShippingClassList[0].remoteShippingClassId, shippingClass.remoteShippingClassId)
        assertEquals(savedShippingClassList[0].name, shippingClass.name)
        assertEquals(savedShippingClassList[0].slug, shippingClass.slug)
        assertEquals(savedShippingClassList[0].description, shippingClass.description)
    }

    @Test
    fun testInsertOrUpdateProductShippingClassList() {
        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)
        assertTrue(shippingClassList.isNotEmpty())

        // Insert product shipping class list
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
        assertEquals(shippingClassList.size, rowsAffected)
    }

    @Test
    fun testGetProductShippingClassListForSite() {
        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)
        assertTrue(shippingClassList.isNotEmpty())

        // Insert product shipping class list
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
        assertEquals(shippingClassList.size, rowsAffected)

        // Get shipping class list for site and verify
        val savedShippingClassListExists = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(shippingClassList.size, savedShippingClassListExists.size)

        // Get shipping class list for a site that does not exist
        val nonExistingSite = SiteModel().apply { id = 400 }
        val savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(nonExistingSite.id)
        assertEquals(0, savedShippingClassList.size)
    }

    @Test
    fun testGetProductShippingClassByRemoteShippingId() {
        val shippingClass = ProductTestUtils.generateSampleProductShippingClass(
            remoteId = 40, siteId = site.id
        )

        // Insert product shipping class list
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClass(shippingClass)
        assertEquals(1, rowsAffected)

        // Get shipping class for site and remoteId and verify
        val savedShippingClassExists = ProductSqlUtils.getProductShippingClassByRemoteId(
                shippingClass.remoteShippingClassId, site.id
        )
        assertEquals(shippingClass.remoteShippingClassId, savedShippingClassExists?.remoteShippingClassId)
        assertEquals(shippingClass.name, savedShippingClassExists?.name)
        assertEquals(shippingClass.description, savedShippingClassExists?.description)
        assertEquals(shippingClass.slug, savedShippingClassExists?.slug)
        assertEquals(shippingClass.localSiteId, savedShippingClassExists?.localSiteId)

        // Get shipping class for a site that does not exist
        val nonExistingSite = SiteModel().apply { id = 400 }
        val savedShippingClass = ProductSqlUtils.getProductShippingClassByRemoteId(
                25, nonExistingSite.id
        )
        assertNull(savedShippingClass)

        // Get shipping class for a site that does not exist
        val nonExistingRemoteId = 25L
        val nonExistentShippingClass = ProductSqlUtils.getProductShippingClassByRemoteId(
                nonExistingRemoteId, site.id
        )
        assertNull(nonExistentShippingClass)
    }

    @Test
    fun testDeleteProductShippingListForSite() {
        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)

        var rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
        assertEquals(shippingClassList.size, rowsAffected)

        // Verify products inserted
        var savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(shippingClassList.size, savedShippingClassList.size)

        // Delete shipping class list for site and verify
        rowsAffected = ProductSqlUtils.deleteProductShippingClassListForSite(site)
        assertEquals(shippingClassList.size, rowsAffected)
        savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(0, savedShippingClassList.size)
    }

    @Test
    fun testDeleteSiteDeletesProductShippingClassList() {
        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)
        assertTrue(shippingClassList.isNotEmpty())

        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
        assertEquals(shippingClassList.size, rowsAffected)

        // Verify products inserted
        var savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(shippingClassList.size, savedShippingClassList.size)

        // Delete site and verify shipping class list  deleted via foreign key constraint
        TestSiteSqlUtils.siteSqlUtils.deleteSite(site)
        savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
        assertEquals(0, savedShippingClassList.size)
    }

    @Test
    fun testInsertOrUpdateProductReview() {
        val review = getProductReviews(site.id)[0]
        assertNotNull(review)

        // Test inserting a product review
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductReview(review)
        assertEquals(1, rowsAffected)
        var savedReview = ProductSqlUtils.getProductReviewByRemoteId(
                site.id, review.remoteProductReviewId
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
        review.apply {
            verified = !verified
        }
        rowsAffected = ProductSqlUtils.insertOrUpdateProductReview(review)
        assertEquals(1, rowsAffected)
        savedReview = ProductSqlUtils.getProductReviewByRemoteId(
                site.id, review.remoteProductReviewId
        )
        assertNotNull(savedReview)
        assertEquals(review.verified, savedReview.verified)
    }

    @Test
    fun testInsertOrUpdateProductReviews() {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())

        // Insert all product reviews
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)
    }

    @Test
    fun testGetProductReviewsForSite() {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())

        // Insert all product reviews
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)

        // Get all product reviews for site and verify
        val savedReviewsExists = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(reviews.size, savedReviewsExists.size)

        // Get all product reviews for a site that does not exist
        val savedReviews = ProductSqlUtils.getProductReviewsForSite(SiteModel().apply { id = 400 })
        assertEquals(0, savedReviews.size)
    }

    @Test
    fun testGetProductReviewsForProduct() {
        val productId = 18L // should be 3 products in the test products json config
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)

        // Get all reviews for existing product
        val savedReviewsForProductExisting = ProductSqlUtils
                .getProductReviewsForProductAndSiteId(site.id, productId)
        assertEquals(3, savedReviewsForProductExisting.size)

        // Get all reviews for non-existing product
        val savedReviewsForProduct = ProductSqlUtils
                .getProductReviewsForProductAndSiteId(site.id, 400)
        assertEquals(0, savedReviewsForProduct.size)
    }

    @Test
    fun testDeleteAllProductReviewsForSite() {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)

        // Verify products inserted
        var savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(reviews.size, savedReviews.size)

        // Delete all reviews for site and verify
        rowsAffected = ProductSqlUtils.deleteAllProductReviewsForSite(site)
        assertEquals(reviews.size, rowsAffected)
        savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(0, savedReviews.size)
    }

    @Test
    fun testDeleteSiteDeletesAllProductReviews() {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)

        // Verify products inserted
        var savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(reviews.size, savedReviews.size)

        // Delete site and verify reviews deleted via foreign key constraint
        TestSiteSqlUtils.siteSqlUtils.deleteSite(site)
        savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(0, savedReviews.size)
    }

    @Test
    fun testDeleteAllProductReviews() {
        val reviews = getProductReviews(site.id)
        assertTrue(reviews.isNotEmpty())
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(reviews)
        assertEquals(reviews.size, rowsAffected)

        // Verify products inserted
        var savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(reviews.size, savedReviews.size)

        // Delete all reviews and verify
        rowsAffected = ProductSqlUtils.deleteAllProductReviews()
        assertEquals(reviews.size, rowsAffected)
        savedReviews = ProductSqlUtils.getProductReviewsForSite(site)
        assertEquals(0, savedReviews.size)
    }

    private fun getProductReviews(localSiteId: Int): List<WCProductReviewModel> {
        val reviewJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/product-reviews.json")
        return ProductTestUtils.getProductReviewsFromJsonString(reviewJson, localSiteId)
    }
}
