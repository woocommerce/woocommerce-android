package org.wordpress.android.fluxc.wc.product

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
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
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
                        WCProductModel::class.java,
                        WCProductReviewModel::class.java,
                        WCProductCategoryModel::class.java,
                        WCProductShippingClassModel::class.java,
                        WCProductTagModel::class.java,
                        SiteModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()

        // Insert the site into the db so it's available later for product
        // reviews
        TestSiteSqlUtils.siteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun testInsertOrUpdateProduct() {
        val productModel = ProductTestUtils.generateSampleProduct(40)
        val site = SiteModel().apply { id = productModel.localSiteId }

        // Test inserting product
        ProductSqlUtils.insertOrUpdateProduct(productModel)
        val storedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(1, storedProductsCount)

        // Test updating product
        val storedProduct = ProductSqlUtils.getProductByRemoteId(site, productModel.remoteProductId)
        storedProduct?.apply {
            name = "Anitaa Test"
            virtual = true
        }
        storedProduct?.also {
            ProductSqlUtils.insertOrUpdateProduct(it)
        }

        val updatedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(1, updatedProductsCount)

        val updatedProduct = ProductSqlUtils.getProductByRemoteId(site, productModel.remoteProductId)
        assertEquals(storedProduct?.id, updatedProduct?.id)
        assertEquals(storedProduct?.name, updatedProduct?.name)
        assertEquals(storedProduct?.virtual, updatedProduct?.virtual)
    }

    @Test
    fun testInsertOrUpdateProductWithDecimalQuantity() {
        val productModel = ProductTestUtils.generateSampleProduct(
                remoteId = 42,
                stockQuantity = 4.2
        )
        val site = SiteModel().apply { id = productModel.localSiteId }

        // Test inserting product
        ProductSqlUtils.insertOrUpdateProduct(productModel)
        val storedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(1, storedProductsCount)

        // Test updating product
        val storedProduct = ProductSqlUtils.getProductByRemoteId(site, productModel.remoteProductId)
        storedProduct?.apply {
            stockQuantity = 4.0
        }
        storedProduct?.also {
            ProductSqlUtils.insertOrUpdateProduct(it)
        }

        val updatedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(1, updatedProductsCount)

        val updatedProduct = ProductSqlUtils.getProductByRemoteId(site, productModel.remoteProductId)
        assertEquals(storedProduct?.id, updatedProduct?.id)
        assertEquals(storedProduct?.name, updatedProduct?.name)
        assertEquals(storedProduct?.virtual, updatedProduct?.virtual)
    }

    @Test
    fun testInsertOrUpdateProducts() {
        val site = SiteModel().apply { id = 2 }
        val products = ArrayList<WCProductModel>().apply {
            this.add(ProductTestUtils.generateSampleProduct(40, siteId = site.id))
            this.add(ProductTestUtils.generateSampleProduct(41, siteId = site.id))
            this.add(ProductTestUtils.generateSampleProduct(42, siteId = site.id))
        }

        // Delete all products for this site, then test inserting the above products
        ProductSqlUtils.deleteProductsForSite(site)
        val insertedProductCount = ProductSqlUtils.insertOrUpdateProducts(products)
        assertEquals(3, insertedProductCount)
        val storedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(3, storedProductsCount)
    }

    @Test
    fun testGetProductsForSite() {
        // insert products for one site
        val site1 = SiteModel().apply { id = 2 }
        val product1 = ProductTestUtils.generateSampleProduct(40, siteId = site1.id)
        ProductSqlUtils.insertOrUpdateProduct(product1)

        // verify that it is stored
        val storedProduct = ProductSqlUtils.getProductByRemoteId(site1, product1.remoteProductId)
        assertEquals(product1.id, storedProduct?.id)
        assertEquals(product1.name, storedProduct?.name)
        assertEquals(product1.virtual, storedProduct?.virtual)

        // insert products for another site
        val site2 = SiteModel().apply { id = 10 }
        val product2 = ProductTestUtils.generateSampleProduct(43, siteId = site2.id)
        ProductSqlUtils.insertOrUpdateProduct(product2)

        // verify that it is stored
        val storedProduct2 = ProductSqlUtils.getProductByRemoteId(site2, product2.remoteProductId)
        assertEquals(product2.id, storedProduct2?.id)
        assertEquals(product2.name, storedProduct2?.name)
        assertEquals(product2.virtual, storedProduct2?.virtual)

        // add another product for site 1
        val product3 = ProductTestUtils.generateSampleProduct(43, siteId = site1.id)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        // verify that the site 2 product size is still the same
        val storedProductForSite2Count = ProductSqlUtils.getProductCountForSite(site2)
        assertEquals(1, storedProductForSite2Count)

        // verify that the site 1 product is increases by 1
        val storedProductForSite1Count = ProductSqlUtils.getProductCountForSite(site1)
        assertEquals(2, storedProductForSite1Count)
    }

    @Test
    fun testGetVirtualProductsForSite() {
        // insert products for one site
        val site1 = SiteModel().apply { id = 2 }
        val products = ArrayList<WCProductModel>().apply {
            this.add(ProductTestUtils.generateSampleProduct(40, siteId = site1.id, virtual = true))
            this.add(ProductTestUtils.generateSampleProduct(41, siteId = site1.id, virtual = false))
            this.add(ProductTestUtils.generateSampleProduct(42, siteId = site1.id, virtual = true))
        }

        ProductSqlUtils.insertOrUpdateProducts(products)

        // verify that the product list exists locally
        val remoteProductIds = products.map { it.remoteProductId }.toList()
        val storedProductCountForSite1 = ProductSqlUtils.getProductCountByRemoteIds(site1, remoteProductIds)
        assertEquals(remoteProductIds.size, storedProductCountForSite1)

        // verify that only 2 of the products are virtual
        assertEquals(2, ProductSqlUtils.getVirtualProductCountByRemoteIds(site1, remoteProductIds))

        // insert products for another site
        val site2 = SiteModel().apply { id = 10 }
        val products2 = ArrayList<WCProductModel>().apply {
            this.add(ProductTestUtils.generateSampleProduct(40, siteId = site2.id, virtual = true))
            this.add(ProductTestUtils.generateSampleProduct(41, siteId = site2.id, virtual = true))
            this.add(ProductTestUtils.generateSampleProduct(42, siteId = site2.id, virtual = true))
        }
        ProductSqlUtils.insertOrUpdateProducts(products2)

        // verify that it is stored
        val remoteProductIds2 = products2.map { it.remoteProductId }.toList()
        val storedProductCountForSite2 = ProductSqlUtils.getProductCountByRemoteIds(site2, remoteProductIds2)
        assertEquals(remoteProductIds2.size, storedProductCountForSite2)

        // verify that all of the products are virtual
        assertEquals(3, ProductSqlUtils.getVirtualProductCountByRemoteIds(site2, remoteProductIds2))

        // insert products for another site
        val site3 = SiteModel().apply { id = 11 }
        val products3 = ArrayList<WCProductModel>().apply {
            this.add(ProductTestUtils.generateSampleProduct(40, siteId = site3.id))
            this.add(ProductTestUtils.generateSampleProduct(41, siteId = site3.id))
            this.add(ProductTestUtils.generateSampleProduct(42, siteId = site3.id))
        }
        ProductSqlUtils.insertOrUpdateProducts(products3)

        // verify that it is stored
        val remoteProductIds3 = products3.map { it.remoteProductId }.toList()
        val storedProductCountForSite3 = ProductSqlUtils.getProductCountByRemoteIds(site3, remoteProductIds3)
        assertEquals(remoteProductIds3.size, storedProductCountForSite3)

        // verify that none of the products are virtual
        assertEquals(0, ProductSqlUtils.getVirtualProductCountByRemoteIds(site3, remoteProductIds3))
    }

    @Test
    fun testDeleteProduct() {
        val remoteProductId = 40L
        val productModel = ProductTestUtils.generateSampleProduct(remoteProductId)
        val site = SiteModel().apply { id = productModel.localSiteId }

        // Test inserting product
        ProductSqlUtils.insertOrUpdateProduct(productModel)
        val storedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(1, storedProductsCount)

        // Test deleting product
        val rowsAffected = ProductSqlUtils.deleteProduct(site, remoteProductId)
        assertEquals(1, rowsAffected)
        assertNull(ProductSqlUtils.getProductByRemoteId(site, remoteProductId))
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
    fun testGetProductsForSiteAndProductIds() {
        val productIds = listOf<Long>(40, 41, 2)

        val product1 = ProductTestUtils.generateSampleProduct(40)
        val product2 = ProductTestUtils.generateSampleProduct(41)
        val product3 = ProductTestUtils.generateSampleProduct(42)

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        val site = SiteModel().apply { id = product1.localSiteId }
        val products = ProductSqlUtils.getProductsByRemoteIds(site, productIds)
        assertEquals(2, products.size)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(40, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(41, siteId = 10)
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(2, siteId = 10)

        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct1)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct2)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct3)

        // verify that the products for the first site is still 2
        assertEquals(2, ProductSqlUtils.getProductsByRemoteIds(site, productIds).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId }
        val differentSiteProducts = ProductSqlUtils.getProductsByRemoteIds(site2, productIds)
        assertEquals(3, differentSiteProducts.size)
    }

    @Test
    fun testGetProductsWithFilterOptions() {
        val productFilterOptions = mapOf(
                ProductFilterOption.STOCK_STATUS to "instock",
                ProductFilterOption.STATUS to "publish",
                ProductFilterOption.TYPE to "simple"
        )

        val product1 = ProductTestUtils.generateSampleProduct(40)
        val product2 = ProductTestUtils.generateSampleProduct(41)
        val product3 = ProductTestUtils.generateSampleProduct(42, stockStatus = "onbackorder")

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        val site = SiteModel().apply { id = product1.localSiteId }
        val products = ProductSqlUtils.getProducts(site, productFilterOptions)
        assertEquals(2, products.size)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(40, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(
                41, siteId = 10, type = "grouped"
        )
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(
                2, siteId = 10, status = "pending"
        )

        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct1)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct2)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct3)

        // verify that the products for the first site is still 2
        assertEquals(2, ProductSqlUtils.getProducts(site, productFilterOptions).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId }
        val differentSiteProducts = ProductSqlUtils.getProducts(site2, productFilterOptions)
        assertEquals(1, differentSiteProducts.size)
    }

    @Test
    fun testGetProductsWithSearchQuery() {
        val product1 = ProductTestUtils.generateSampleProduct(40, name = "a",
                description = "1", shortDescription = "+")
        val product2 = ProductTestUtils.generateSampleProduct(41, name = "b",
                description = "2", shortDescription = "-")
        val product3 = ProductTestUtils.generateSampleProduct(42, name = "xyz ab piu",
                description = "xyz 12 piu", shortDescription = "xyz +- piu", stockStatus = "onbackorder")

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        val site = SiteModel().apply { id = product1.localSiteId }

        // Test search in name
        var products = ProductSqlUtils.getProducts(site, emptyMap(), searchQuery = "a")
        // Products 1 and 3
        assertEquals(2, products.size)

        products = ProductSqlUtils.getProducts(site, emptyMap(), searchQuery = "b")
        // Products 2 and 3
        assertEquals(2, products.size)

        // Product 3
        products = ProductSqlUtils.getProducts(site, emptyMap(), searchQuery = "ab")
        assertEquals(1, products.size)

        // Test search in description
        products = ProductSqlUtils.getProducts(site, emptyMap(), searchQuery = "1")
        // Products 1 and 3
        assertEquals(2, products.size)

        products = ProductSqlUtils.getProducts(site, emptyMap(), searchQuery = "2")
        // Products 2 and 3
        assertEquals(2, products.size)

        products = ProductSqlUtils.getProducts(site, emptyMap(), searchQuery = "12")
        // Product 3
        assertEquals(1, products.size)

        // Test search in short description
        products = ProductSqlUtils.getProducts(site, emptyMap(), searchQuery = "+")
        // Products 1 and 3
        assertEquals(2, products.size)

        products = ProductSqlUtils.getProducts(site, emptyMap(), searchQuery = "-")
        // Products 2 and 3
        assertEquals(2, products.size)

        products = ProductSqlUtils.getProducts(site, emptyMap(), searchQuery = "+-")
        // Product 3
        assertEquals(1, products.size)

        // Test search with filter options
        products = ProductSqlUtils.getProducts(site, mapOf(ProductFilterOption.STOCK_STATUS to "instock"),
                searchQuery = "a")
        // Product 1
        assertEquals(1, products.size)
    }

    @Test
    fun testGetProductsForSiteWithExcludedProductIds() {
        val excludedProductIds = listOf(40L)

        val product1 = ProductTestUtils.generateSampleProduct(40)
        val product2 = ProductTestUtils.generateSampleProduct(41)
        val product3 = ProductTestUtils.generateSampleProduct(42)

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        val site = SiteModel().apply { id = product1.localSiteId }
        val products = ProductSqlUtils.getProducts(
                site, filterOptions = emptyMap(), excludedProductIds = excludedProductIds
        )
        assertEquals(2, products.size)
        assertEquals(41, products.first().remoteProductId)
        assertEquals(42, products.last().remoteProductId)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(40, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(
                41, siteId = 10
        )
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(
                42, siteId = 10
        )

        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct1)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct2)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct3)

        // verify that the products for the first site is still 2
        assertEquals(2, ProductSqlUtils.getProducts(
                site, emptyMap(), excludedProductIds = excludedProductIds
        ).size)

        // verify that the products for the second site is also 2
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId }
        val differentSiteProducts = ProductSqlUtils.getProducts(
                site2, emptyMap(), excludedProductIds = listOf(40, 41)
        )
        assertEquals(1, differentSiteProducts.size)
        assertEquals(42, differentSiteProducts.first().remoteProductId)
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

    @Test
    fun testInsertOrUpdateProductCategory() {
        val category = ProductTestUtils.getProductCategories(site.id)[0]
        assertNotNull(category)

        // Test inserting a product category
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductCategory(category)
        assertEquals(1, rowsAffected)
        var savedCategory = ProductSqlUtils.getProductCategoryByRemoteId(
                site.id, category.remoteCategoryId
        )
        assertNotNull(savedCategory)
        assertEquals(category.remoteCategoryId, savedCategory.remoteCategoryId)
        assertEquals(category.name, savedCategory.name)
        assertEquals(category.slug, savedCategory.slug)
        assertEquals(category.parent, savedCategory.parent)
        assertEquals(category.localSiteId, savedCategory.localSiteId)

        // Test updating the same product category
        category.apply {
            name = "foo"
        }
        rowsAffected = ProductSqlUtils.insertOrUpdateProductCategory(category)
        assertEquals(1, rowsAffected)
        savedCategory = ProductSqlUtils.getProductCategoryByRemoteId(
                site.id, category.remoteCategoryId
        )
        assertNotNull(savedCategory)
        assertEquals(category.name, savedCategory.name)
    }

    @Test
    fun testInsertOrUpdateProductCategories() {
        val productCategories = ProductTestUtils.getProductCategories(site.id)
        assertTrue(productCategories.isNotEmpty())

        // Insert all product categories
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(productCategories)
        assertEquals(productCategories.size, rowsAffected)
    }

    @Test
    fun testGetProductCategoriesForSite() {
        val categories = ProductTestUtils.getProductCategories(site.id)
        assertTrue(categories.isNotEmpty())

        // Insert all product categories
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(categories)
        assertEquals(categories.size, rowsAffected)

        // Get all product categories for site and verify
        val savedCategoriesExist = ProductSqlUtils.getProductCategoriesForSite(site)
        assertEquals(categories.size, savedCategoriesExist.size)

        // Get all product categories for a site that do not exist
        val savedCategories = ProductSqlUtils.getProductCategoriesForSite(SiteModel().apply { id = 400 })
        assertEquals(0, savedCategories.size)
    }

    @Test
    fun testDeleteAllProductCategories() {
        val categories = ProductTestUtils.getProductCategories(site.id)
        assertTrue(categories.isNotEmpty())
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(categories)
        assertEquals(categories.size, rowsAffected)

        // Verify categories inserted
        var savedCategories = ProductSqlUtils.getProductCategoriesForSite(site)
        assertEquals(categories.size, savedCategories.size)

        // Delete all categories and verify
        rowsAffected = ProductSqlUtils.deleteAllProductCategories()
        assertEquals(categories.size, rowsAffected)
        savedCategories = ProductSqlUtils.getProductCategoriesForSite(site)
        assertEquals(0, savedCategories.size)
    }

    @Test
    fun testDeleteProductCategoriesForSite() {
        val categories = ProductTestUtils.getProductCategories(site.id)

        var rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(categories)
        assertEquals(categories.size, rowsAffected)

        // Verify categories inserted
        var savedCategories = ProductSqlUtils.getProductCategoriesForSite(site)
        assertEquals(categories.size, savedCategories.size)

        // Delete categories for site and verify
        rowsAffected = ProductSqlUtils.deleteAllProductCategoriesForSite(site)
        assertEquals(categories.size, rowsAffected)
        savedCategories = ProductSqlUtils.getProductCategoriesForSite(site)
        assertEquals(0, savedCategories.size)
    }

    @Test
    fun testInsertOrUpdateProductTag() {
        val tagModel = ProductTestUtils.generateProductTags(site.id)[0]
        assertNotNull(tagModel)

        // Test inserting a product tag
        var rowsAffected = ProductSqlUtils.insertOrUpdateProductTag(tagModel)
        assertEquals(1, rowsAffected)

        var savedTagList = ProductSqlUtils.getProductTagsForSite(site.id)
        assertEquals(savedTagList.size, 1)
        assertEquals(savedTagList[0].localSiteId, tagModel.localSiteId)
        assertEquals(savedTagList[0].remoteTagId, tagModel.remoteTagId)
        assertEquals(savedTagList[0].name, tagModel.name)
        assertEquals(savedTagList[0].slug, tagModel.slug)
        assertEquals(savedTagList[0].description, tagModel.description)

        // Test updating the same product tag
        tagModel.apply { name = "Tag update" }
        rowsAffected = ProductSqlUtils.insertOrUpdateProductTag(tagModel)
        assertEquals(1, rowsAffected)

        savedTagList = ProductSqlUtils.getProductTagsForSite(site.id)
        assertEquals(savedTagList.size, 1)
        assertEquals(savedTagList[0].localSiteId, tagModel.localSiteId)
        assertEquals(savedTagList[0].remoteTagId, tagModel.remoteTagId)
        assertEquals(savedTagList[0].name, tagModel.name)
        assertEquals(savedTagList[0].slug, tagModel.slug)
        assertEquals(savedTagList[0].description, tagModel.description)
    }

    @Test
    fun testInsertOrUpdateProductTagList() {
        val tagList = ProductTestUtils.generateProductTags(site.id)
        assertTrue(tagList.isNotEmpty())

        // Insert product tag list
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductTags(tagList)
        assertEquals(tagList.size, rowsAffected)
    }

    @Test
    fun testGetProductTagsForSite() {
        val tagList = ProductTestUtils.generateProductTags(site.id)
        assertTrue(tagList.isNotEmpty())

        // Insert product tag list
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductTags(tagList)
        assertEquals(tagList.size, rowsAffected)

        // Get tag list for site and verify
        val savedTagListExists = ProductSqlUtils.getProductTagsForSite(site.id)
        assertEquals(tagList.size, savedTagListExists.size)

        // Get tag list for a site that does not exist
        val nonExistingSite = SiteModel().apply { id = 400 }
        val savedTagList = ProductSqlUtils.getProductTagsForSite(nonExistingSite.id)
        assertEquals(0, savedTagList.size)
    }

    @Test
    fun testGetProductTagByName() {
        val tagList = ProductTestUtils.generateProductTags(site.id)
        assertTrue(tagList.isNotEmpty())

        // Insert product tag list
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductTags(tagList)
        assertEquals(tagList.size, rowsAffected)

        // Get tag by name and verify
        val savedTagExists = ProductSqlUtils.getProductTagByName(site.id, tagList[0].name)
        assertEquals(tagList[0].name, savedTagExists?.name)
        assertEquals(tagList[0].remoteTagId, savedTagExists?.remoteTagId)
        assertEquals(tagList[0].slug, savedTagExists?.slug)
        assertEquals(tagList[0].description, savedTagExists?.description)

        // Get tag for a name that does not exist
        val nonExistingTagName = "test"
        val savedTag = ProductSqlUtils.getProductTagByName(site.id, nonExistingTagName)
        assertNull(savedTag)
    }

    @Test
    fun testGetProductTagsByNames() {
        val tagList = ProductTestUtils.generateProductTags(site.id)
        assertTrue(tagList.isNotEmpty())

        // Insert product tag list
        val rowsAffected = ProductSqlUtils.insertOrUpdateProductTags(tagList)
        assertEquals(tagList.size, rowsAffected)

        // Get tags by list of name and verify
        val tagNames = tagList.map { it.name }.toList()
        val savedTagListExists = ProductSqlUtils.getProductTagsByNames(site.id, tagNames)
        assertEquals(tagList.size, savedTagListExists.size)
        assertEquals(tagList[0].name, savedTagListExists[0].name)
        assertEquals(tagList[1].name, savedTagListExists[1].name)
        assertEquals(tagList[2].name, savedTagListExists[2].name)

        // Get tags for a name that does not exist
        val monExistingTagList = ProductSqlUtils.getProductTagsByNames(
                site.id, listOf("test", "test1", "test2")
        )
        assertEquals(0, monExistingTagList.size)

        val savedTagList = ProductSqlUtils.getProductTagsByNames(
                site.id, listOf(tagNames[0], tagNames[1], "test")
        )
        assertEquals(2, savedTagList.size)
    }

    @Test
    fun testDeleteProductTagsForSite() {
        val tags = ProductTestUtils.generateProductTags(site.id)

        var rowsAffected = ProductSqlUtils.insertOrUpdateProductTags(tags)
        assertEquals(tags.size, rowsAffected)

        // Verify product tags inserted
        var savedTags = ProductSqlUtils.getProductTagsForSite(site.id)
        assertEquals(tags.size, savedTags.size)

        // Delete tags for site and verify
        rowsAffected = ProductSqlUtils.deleteProductTagsForSite(site)
        assertEquals(tags.size, rowsAffected)
        savedTags = ProductSqlUtils.getProductTagsForSite(site.id)
        assertEquals(0, savedTags.size)
    }

    @Test
    fun testDeleteSiteDeletesProductTags() {
        val tags = ProductTestUtils.generateProductTags(site.id)
        assertTrue(tags.isNotEmpty())

        val rowsAffected = ProductSqlUtils.insertOrUpdateProductTags(tags)
        assertEquals(tags.size, rowsAffected)

        // Verify tags inserted
        var savedTags = ProductSqlUtils.getProductTagsForSite(site.id)
        assertEquals(tags.size, savedTags.size)

        // Delete site and verify tags are deleted via foreign key constraint
        TestSiteSqlUtils.siteSqlUtils.deleteSite(site)
        savedTags = ProductSqlUtils.getProductTagsForSite(site.id)
        assertEquals(0, savedTags.size)
    }

    private fun getProductReviews(localSiteId: Int): List<WCProductReviewModel> {
        val reviewJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/product-reviews.json")
        return ProductTestUtils.getProductReviewsFromJsonString(reviewJson, localSiteId)
    }
}
