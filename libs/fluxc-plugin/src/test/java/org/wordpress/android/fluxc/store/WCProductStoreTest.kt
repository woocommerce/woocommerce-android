package org.wordpress.android.fluxc.store

import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonObject
import com.yarolegovich.wellsql.WellSql
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.BatchProductUpdateApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.BatchProductUpdateResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.BatchProductVariationsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.DuplicateProductApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductVariationApiResponse
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.ProductStorageHelper
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.persistence.dao.ProductVariationsDao
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.store.WCProductStore.BatchGenerateVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.BatchUpdateProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.BatchUpdateVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.IncludeType
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateVariationPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateVariationPayload
import org.wordpress.android.fluxc.utils.initCoroutineEngine
import org.wordpress.android.fluxc.wc.product.ProductTestUtils
import org.wordpress.android.fluxc.wc.utils.TestSiteSqlUtils
import kotlin.random.Random
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
class WCProductStoreTest {
    private val productRestClient: ProductRestClient = mock()
    private lateinit var productStore: WCProductStore
    private lateinit var productsDao: ProductsDao
    private lateinit var productsVariationsDao: ProductVariationsDao

    @Rule
    @JvmField
    val wcDatabaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        val config = SingleStoreWellSqlConfigForTests(
            ApplicationProvider.getApplicationContext(),
            SiteModel::class.java
        )
        WellSql.init(config)
        config.reset()

        productsDao = wcDatabaseRule.db.productsDao
        productsVariationsDao = wcDatabaseRule.db.productVariationsDao

        val productStorageHelper = ProductStorageHelper(
            productsDao = productsDao,
            metaDataDao = wcDatabaseRule.db.metaDataDao
        )
        productStore = WCProductStore(
            Dispatcher(),
            productRestClient,
            addonsDao = mock(),
            logger = mock(),
            productStorageHelper = productStorageHelper,
            coroutineEngine = initCoroutineEngine(),
            productsDao = productsDao,
            productVariationsDao = productsVariationsDao,
            productCategoriesDao = wcDatabaseRule.db.productCategoriesDao,
            productTagsDao = wcDatabaseRule.db.productTagsDao,
            productShippingClassesDao = wcDatabaseRule.db.productShippingClassesDao,
            productReviewsDao = wcDatabaseRule.db.productReviewsDao,
        )
    }

    @Test
    fun `given core duplication succeeds, when duplicating product, then duplicated product ID is returned`() = runTest {
        // GIVEN
        val site = SiteModel()
        whenever(productRestClient.duplicateProduct(site, 42L))
            .thenReturn(WooPayload(DuplicateProductApiResponse(84L)))

        // WHEN
        val result = productStore.duplicateProduct(site, 42L)

        // THEN
        assertThat(result.model).isEqualTo(84L)
    }

    @Test
    fun `given core duplication response has missing or invalid ID, when duplicating product, then invalid response is returned`() =
        runTest {
            // GIVEN
            val site = SiteModel()
            listOf(null, 0L, -1L).forEach { responseId ->
                whenever(productRestClient.duplicateProduct(site, 42L))
                    .thenReturn(WooPayload(DuplicateProductApiResponse(responseId)))

                // WHEN
                val result = productStore.duplicateProduct(site, 42L)

                // THEN
                assertThat(result.error.type).isEqualTo(INVALID_RESPONSE)
            }
        }

    @Test
    fun `given core duplication returns an error, when duplicating product, then full error is forwarded unchanged`() =
        runTest {
            // GIVEN
            val site = SiteModel()
            val expectedError = WooError(
                type = API_NOT_FOUND,
                original = NOT_FOUND,
                message = "No route was found matching the URL and request method",
                apiErrorCode = "rest_no_route",
                errorData = JSONObject().put("status", 404)
            )
            whenever(productRestClient.duplicateProduct(site, 42L))
                .thenReturn(WooPayload(expectedError))

            // WHEN
            val result = productStore.duplicateProduct(site, 42L)

            // THEN
            assertThat(result.error).isEqualTo(expectedError)
        }

    @Test
    fun testSimpleInsertionAndRetrieval() = runTest {
        val productModel = ProductTestUtils.generateSampleProduct(42)
        val site = SiteModel().apply { id = productModel.localSiteId.value }

        productsDao.upsertProduct(productModel)

        val storedProduct = productStore.getProductByRemoteId(site, productModel.remoteProductId)
        assertEquals(42, storedProduct?.remoteProductId)
        assertEquals(productModel, storedProduct)
    }

    @Test
    fun testGetProductsBySiteAndProductIds() = runTest {
        val productIds = listOf<Long>(30, 31, 2)

        val product1 = ProductTestUtils.generateSampleProduct(30)
        val product2 = ProductTestUtils.generateSampleProduct(31)
        val product3 = ProductTestUtils.generateSampleProduct(42)

        productsDao.upsertProducts(listOf(product1, product2, product3))

        val site = SiteModel().apply { id = product1.localSiteId.value }
        val products = productStore.getProductsByRemoteIds(site, productIds)
        assertEquals(2, products.size)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(10, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(11, siteId = 10)
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(2, siteId = 10)

        productsDao.upsertProducts(listOf(differentSiteProduct1, differentSiteProduct2, differentSiteProduct3))

        // verify that the products for the first site is still 2
        assertEquals(2, productStore.getProductsByRemoteIds(site, productIds).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId.value }
        val differentSiteProducts = productStore.getProductsByRemoteIds(site2, listOf(10, 11, 2))

        assertThat(differentSiteProducts.map { it.remoteProductId })
            .containsExactlyInAnyOrder(
                differentSiteProduct1.remoteProductId,
                differentSiteProduct2.remoteProductId,
                differentSiteProduct3.remoteProductId
            )
    }

    @Test
    fun testUpdateProduct() = runTest {
        val updatedDescription = "Updated description"
        val productModel = ProductTestUtils.generateSampleProduct(42).copy(
            name = "test product",
            description = "test description"
        )
        val site = SiteModel().apply { id = productModel.localSiteId.value }
        productsDao.upsertProduct(productModel)

        // Simulate incoming action with updated product model
        val payload = RemoteUpdateProductPayload(
            site,
            ProductWithMetaData(productModel.copy(description = updatedDescription))
        )
        productStore.onAction(WCProductActionBuilder.newUpdatedProductAction(payload))

        with(productStore.getProductByRemoteId(site, productModel.remoteProductId)) {
            // The version of the product model in the database should have the updated description
            assertEquals(updatedDescription, this?.description)
            // Other fields should not be altered by the update
            assertEquals(productModel.name, this?.name)
        }
    }

    @Test
    fun testUpdateVariation() = runTest {
        val variationModel = ProductTestUtils.generateSampleVariation(42, 24).copy(
            description = "test description"
        )
        val site = SiteModel().apply { id = variationModel.localSiteId.value }
        whenever(productRestClient.updateVariation(site, null, variationModel))
            .thenReturn(RemoteUpdateVariationPayload(site, variationModel))

        productStore.updateVariation(UpdateVariationPayload(site, variationModel))

        with(
            productStore.getVariationByRemoteId(
                site,
                variationModel.remoteProductId.value,
                variationModel.remoteVariationId.value
            )
        ) {
            // The version of the product model in the database should have the updated description
            assertEquals(variationModel.description, this?.description)
            // Other fields should not be altered by the update
            assertEquals(variationModel.status, this?.status)
        }
    }

    @Test
    fun testVerifySkuExistsLocally() = runTest {
        val sku = "woo-cap"
        val productModel = ProductTestUtils.generateSampleProduct(42).copy(
            name = "test product",
            description = "test description",
            sku = sku,
        )
        val site = SiteModel().apply { id = productModel.localSiteId.value }
        productsDao.upsertProduct(productModel)

        // verify if product with sku: woo-cap exists in local cache
        val skuAvailable = productStore.isProductExists(site, sku)
        assertTrue(skuAvailable)

        // verify if product with non existent sku returns false
        assertFalse(productStore.isProductExists(site, "woooo"))
    }

    @Test
    fun testGetProductsWithFilterOptions() = runTest {
        val filterOptions = mapOf(
            ProductFilterOption.TYPE to "simple",
            ProductFilterOption.STOCK_STATUS to "instock",
            ProductFilterOption.STATUS to "publish",
            ProductFilterOption.CATEGORY to "1337"
        )
        val product1 = ProductTestUtils.generateSampleProduct(3)
        val product2 = ProductTestUtils.generateSampleProduct(
            31, type = "variable", status = "draft"
        )
        val product3 = ProductTestUtils.generateSampleProduct(
            42, stockStatus = "onbackorder", status = "pending"
        )

        val product4 = ProductTestUtils.generateSampleProduct(
            43, categories = "[{\"id\":1337,\"name\":\"Clothing\",\"slug\":\"clothing\"}]"
        )

        productsDao.upsertProducts(listOf(product1, product2, product3, product4))

        val site = SiteModel().apply { id = product1.localSiteId.value }
        val products = productStore.getProducts(site, filterOptions)
        assertEquals(1, products.size)

        // insert products with the same product options but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(10, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(
            11, siteId = 10, type = "variable", status = "draft"
        )
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(
            12, siteId = 10, stockStatus = "onbackorder", status = "pending"
        )
        val differentSiteProduct4 = ProductTestUtils.generateSampleProduct(
            13,
            siteId = 10,
            categories = "[{\"id\":1337,\"name\":\"Clothing\",\"slug\":\"clothing\"}]"
        )

        productsDao.upsertProducts(
            listOf(
                differentSiteProduct1,
                differentSiteProduct2,
                differentSiteProduct3,
                differentSiteProduct4
            )
        )

        // verify that the products for the first site is still 1
        assertEquals(1, productStore.getProducts(site, filterOptions).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId.value }
        val filterOptions2 = mapOf(ProductFilterOption.STATUS to "draft")
        val differentSiteProducts = productStore.getProducts(site2, filterOptions2)
        assertEquals(1, differentSiteProducts.size)
        assertEquals(differentSiteProduct2.status, differentSiteProducts[0].status)

        val filterOptions3 = mapOf(ProductFilterOption.STOCK_STATUS to "onbackorder")
        val differentProductFilters = productStore.getProducts(site2, filterOptions3)
        assertEquals(1, differentProductFilters.size)
        assertEquals(differentSiteProduct3.stockStatus, differentProductFilters[0].stockStatus)

        val filterByCategory = mapOf(ProductFilterOption.CATEGORY to "1337")
        val productsFilteredByCategory = productStore.getProducts(site2, filterByCategory)
        assertEquals(1, productsFilteredByCategory.size)
        assertTrue(productsFilteredByCategory[0].categories.contains("1337"))
    }

    @Test
    fun `test AddProduct Action triggered correctly`() = runTest {
        // given
        val productModel = ProductTestUtils.generateSampleProduct(remoteId = 0).copy(
            name = "test new product",
            description = "test new description"
        )
        val site = SiteModel().apply { id = productModel.localSiteId.value }

        // when
        productsDao.upsertProduct(productModel)
        val payload = RemoteAddProductPayload(site, ProductWithMetaData(productModel))
        productStore.onAction(WCProductActionBuilder.newAddedProductAction(payload))

        // then
        with(productStore.getProductByRemoteId(site, productModel.remoteProductId)) {
            assertNotNull(this)
            assertEquals(productModel.description, this?.description)
            assertEquals(productModel.name, this?.name)
        }
    }

    @Test
    fun `test that product insert emits a flow event`() = runTest {
        // given
        val productModel = ProductTestUtils.generateSampleProduct(remoteId = 0).copy(
            name = "test new product",
            description = "test new description"
        )
        val productList = ProductTestUtils.generateProductList()
        productsDao.upsertProducts(productList)
        val site = SiteModel().apply { id = productModel.localSiteId.value }

        var observedProducts = productStore.observeProducts(site).first()
        assertThat(observedProducts).isEqualTo(productList)

        // when
        productsDao.upsertProduct(productModel)
        observedProducts = productStore.observeProducts(site).first()

        // then
        assertThat(observedProducts).isEqualTo(productList + productModel)
    }

    @Test
    fun `test that variation insert emits a flow event`() = runTest {
        // given
        val variation = ProductTestUtils.generateSampleVariation(
            remoteId = 0,
            variationId = 1
        ).copy(
            description = "test new description"
        )
        val site = SiteModel().apply { id = variation.localSiteId.value }
        val variations = ProductTestUtils.generateSampleVariations(
            number = 5,
            productId = variation.remoteProductId.value,
            siteId = site.id
        )
        productsVariationsDao.upsertProductVariations(variations)

        var observedVariations = productStore.observeVariations(site, variation.remoteProductId.value)
            .first()
        assertThat(observedVariations).containsExactlyInAnyOrderElementsOf(variations)

        // when
        productsVariationsDao.upsertProductVariation(variation)
        observedVariations = productStore.observeVariations(site, variation.remoteProductId.value).first()

        // then
        assertThat(observedVariations).containsExactlyInAnyOrderElementsOf(variations + variation)
    }

    @Test
    fun `when product variations are fetched, then fetched page is returned and saved`() = runTest {
        val site = SiteModel().apply { id = 42 }
        val productId = 100L
        val staleVariation = ProductTestUtils.generateSampleVariation(productId, 1L, site.id)
        val fetchedVariations = ProductTestUtils.generateSampleVariations(2, productId, site.id)
        productsVariationsDao.upsertProductVariation(staleVariation)
        whenever(
            productRestClient.fetchProductVariationsWithSyncRequest(
                site = site,
                productId = productId,
                pageSize = 2,
                offset = 0
            )
        ).thenReturn(WooPayload(fetchedVariations))

        val result = productStore.fetchProductVariations(
            FetchProductVariationsPayload(
                site = site,
                remoteProductId = productId,
                pageSize = 2,
                offset = 0
            )
        )

        assertThat(result.isError).isFalse
        assertThat(requireNotNull(result.model).variations).containsExactlyElementsOf(fetchedVariations)
        assertThat(requireNotNull(result.model).canLoadMore).isTrue
        assertThat(productsVariationsDao.getVariations(site.localId(), RemoteId(productId)))
            .containsExactlyInAnyOrderElementsOf(fetchedVariations)
    }

    @Test
    fun `given cached variations, when product variations page is fetched, then only fetched page is returned`() =
        runTest {
            val site = SiteModel().apply { id = 42 }
            val productId = 100L
            val cachedVariation = ProductTestUtils.generateSampleVariation(productId, 1L, site.id)
            val fetchedVariations = listOf(
                ProductTestUtils.generateSampleVariation(productId, 2L, site.id),
                ProductTestUtils.generateSampleVariation(productId, 3L, site.id)
            )
            productsVariationsDao.upsertProductVariation(cachedVariation)
            whenever(
                productRestClient.fetchProductVariationsWithSyncRequest(
                    site = site,
                    productId = productId,
                    pageSize = 2,
                    offset = 2
                )
            ).thenReturn(WooPayload(fetchedVariations))

            val result = productStore.fetchProductVariations(
                FetchProductVariationsPayload(
                    site = site,
                    remoteProductId = productId,
                    pageSize = 2,
                    offset = 2
                )
            )

            assertThat(result.isError).isFalse
            assertThat(requireNotNull(result.model).variations).containsExactlyElementsOf(fetchedVariations)
            assertThat(requireNotNull(result.model).canLoadMore).isTrue
            assertThat(productsVariationsDao.getVariations(site.localId(), RemoteId(productId)))
                .containsExactlyInAnyOrderElementsOf(listOf(cachedVariation) + fetchedVariations)
        }

    @Test
    fun `when product variations sync fetch succeeds, then page and more flag are returned`() = runTest {
        val site = SiteModel().apply { id = 42 }
        val productId = 100L
        val fetchedVariations = ProductTestUtils.generateSampleVariations(1, productId, site.id)
        whenever(
            productRestClient.fetchProductVariationsWithSyncRequest(
                site = any(),
                productId = any(),
                pageSize = any(),
                offset = any(),
                includedVariationIds = any(),
                searchQuery = anyOrNull(),
                excludedVariationIds = any(),
                filterOptions = anyOrNull(),
                orderCurrency = anyOrNull(),
                posProductsOnly = any()
            )
        ).thenReturn(WooPayload(fetchedVariations))

        val result = productStore.fetchProductVariations(
            site = site,
            productId = productId,
            offset = 25,
            pageSize = 25
        )

        assertThat(result.isError).isFalse
        assertThat(requireNotNull(result.model).variations).containsExactlyElementsOf(fetchedVariations)
        assertThat(requireNotNull(result.model).canLoadMore).isFalse
    }

    @Test
    fun `given product review exists, when fetch product review, then local database updated`() = runTest {
        val site = insertTestAccountAndSiteIntoDb()
        val productModel = ProductTestUtils.generateSampleProduct(remoteId = 1).copy(
            localSiteId = site.localId()
        )
        productsDao.upsertProduct(productModel)

        val reviewModel = ProductTestUtils.generateSampleProductReview(
            productModel.remoteProductId,
            123L,
            site.id
        )
        whenever(productRestClient.fetchProductReviewById(site, reviewModel.remoteProductReviewId.value))
            .thenReturn(RemoteProductReviewPayload(site, reviewModel))

        productStore.fetchSingleProductReview(FetchSingleProductReviewPayload(site, reviewModel.remoteProductReviewId.value))

        assertThat(productStore.getProductReviewByRemoteId(site.localId(), reviewModel.remoteProductReviewId)).isNotNull
        Unit
    }

    @Test
    fun `batch variations update should return positive result on successful backend request`() =
        runTest {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            val site = SiteModel().apply { id = product.localSiteId.value }
            val variations = ProductTestUtils.generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )

            // when
            val variationsIds = variations.map { it.remoteVariationId.value }
            val variationsUpdatePayload = BatchUpdateVariationsPayload.Builder(
                site,
                product.remoteProductId,
                variationsIds
            ).build()
            val response = BatchProductVariationsApiResponse(updatedVariations = emptyList())
            whenever(
                productRestClient.batchUpdateVariations(
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    anyOrNull()
                )
            ) doReturn WooPayload(response)
            val result = productStore.batchUpdateVariations(variationsUpdatePayload)

            // then
            assertThat(result.isError).isFalse
            assertThat(result.model).isNotNull
            Unit
        }

    @Test
    fun `batch variations update should return negative result on failed backend request`() =
        runTest {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            val site = SiteModel().apply { id = product.localSiteId.value }
            val variations = ProductTestUtils.generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )

            // when
            val variationsIds = variations.map { it.remoteVariationId.value }
            val variationsUpdatePayload =
                BatchUpdateVariationsPayload.Builder(site, product.remoteProductId, variationsIds)
                    .build()
            val errorResponse = WooError(GENERIC_ERROR, NETWORK_ERROR, "🔴")
            whenever(
                productRestClient.batchUpdateVariations(
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    anyOrNull()
                )
            ) doReturn WooPayload(errorResponse)
            val result = productStore.batchUpdateVariations(variationsUpdatePayload)

            // then
            assertThat(result.isError).isTrue
            Unit
        }

    @Test
    fun `batch variations update should update variations locally after successful backend request`() =
        runTest {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            productsDao.upsertProduct(product)
            val site = insertTestAccountAndSiteIntoDb()
            val variations = ProductTestUtils.generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )
            productsVariationsDao.upsertProductVariations(variations)

            // when
            val variationsIds = variations.map { it.remoteVariationId.value }
            val newRegularPrice = "1.234 💰"
            val newSalePrice = "0.234 💰"
            val variationsUpdatePayload =
                BatchUpdateVariationsPayload.Builder(site, product.remoteProductId, variationsIds)
                    .regularPrice(newRegularPrice)
                    .salePrice(newSalePrice)
                    .build()
            val variationsReturnedFromBackend = variations.map {
                ProductVariationApiResponse().apply {
                    id = it.remoteVariationId.value
                    regular_price = newRegularPrice
                    sale_price = newSalePrice
                }
            }
            val response = BatchProductVariationsApiResponse(updatedVariations = variationsReturnedFromBackend)
            whenever(
                productRestClient.batchUpdateVariations(
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    anyOrNull()
                )
            ) doReturn WooPayload(response)
            val result = productStore.batchUpdateVariations(variationsUpdatePayload)

            // then
            assertThat(result.isError).isFalse
            assertThat(result.model).isNotNull
            with(productsVariationsDao.getVariations(site.localId(), product.remoteId)) {
                forEach { variation ->
                    assertThat(variation.regularPrice).isEqualTo(newRegularPrice)
                    assertThat(variation.salePrice).isEqualTo(newSalePrice)
                }
            }
        }

    @Test
    fun `batch variations update should not update variations locally after failed backend request`() =
        runTest {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            productsDao.upsertProduct(product)
            val site = insertTestAccountAndSiteIntoDb()
            val variations = ProductTestUtils.generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )
            productsVariationsDao.upsertProductVariations(variations)

            // when
            val variationsIds = variations.map { it.remoteVariationId.value }
            val newRegularPrice = "1.234 💰"
            val newSalePrice = "0.234 💰"
            val variationsUpdatePayload =
                BatchUpdateVariationsPayload.Builder(site, product.remoteProductId, variationsIds)
                    .regularPrice(newRegularPrice)
                    .salePrice(newSalePrice)
                    .build()
            val errorResponse = WooError(GENERIC_ERROR, NETWORK_ERROR, "🔴")
            whenever(
                productRestClient.batchUpdateVariations(
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    anyOrNull()
                )
            ) doReturn WooPayload(errorResponse)
            val result = productStore.batchUpdateVariations(variationsUpdatePayload)

            // then
            assertThat(result.isError).isTrue
            with(productsVariationsDao.getVariations(site.localId(), product.remoteId)) {
                forEach { variation ->
                    assertThat(variation.regularPrice).isNotEqualTo(newRegularPrice)
                    assertThat(variation.salePrice).isNotEqualTo(newSalePrice)
                }
            }
        }

    @Test
    fun `BatchUpdateVariationsPayload_Builder should produce correct variations modifications map`() {
        // given
        val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
        val site = SiteModel().apply { id = 23 }
        val variations = ProductTestUtils.generateSampleVariations(
            number = 64,
            productId = product.remoteProductId,
            siteId = site.id
        )
        val builder = BatchUpdateVariationsPayload.Builder(
            site,
            product.remoteProductId,
            variations.map { it.remoteVariationId.value }
        )

        val modifiedRegularPrice = "11234.234"
        val modifiedSalePrice = "123,2.4"
        val modifiedStartOfSale = "tomorrow"
        val modifiedEndOfSale = "next week"
        val modifiedStockQuantity = 1234
        val modifiedStockStatus = CoreProductStockStatus.IN_STOCK
        val modifiedWeight = "1234 kg"
        val modifiedWidth = "5"
        val modifiedHeight = "10"
        val modifiedLength = "15"
        val modifiedShippingClassId = "1234"
        val modifiedShippingClassSlug = "DHL"

        // when
        builder.regularPrice(modifiedRegularPrice)
            .salePrice(modifiedSalePrice)
            .startOfSale(modifiedStartOfSale)
            .endOfSale(modifiedEndOfSale)
            .stockQuantity(modifiedStockQuantity)
            .stockStatus(modifiedStockStatus)
            .weight(modifiedWeight)
            .dimensions(length = modifiedLength, width = modifiedWidth, height = modifiedHeight)
            .shippingClassId(modifiedShippingClassId)
            .shippingClassSlug(modifiedShippingClassSlug)
        val payload = builder.build()

        // then
        with(payload.modifiedProperties) {
            assertThat(get("regular_price")).isEqualTo(modifiedRegularPrice)
            assertThat(get("sale_price")).isEqualTo(modifiedSalePrice)
            assertThat(get("date_on_sale_from")).isEqualTo(modifiedStartOfSale)
            assertThat(get("date_on_sale_to")).isEqualTo(modifiedEndOfSale)
            assertThat(get("stock_quantity")).isEqualTo(modifiedStockQuantity)
            assertThat(get("stock_status")).isEqualTo(modifiedStockStatus)
            assertThat(get("weight")).isEqualTo(modifiedWeight)
            with(get("dimensions") as JsonObject) {
                assertThat(get("length").asString).isEqualTo(modifiedLength)
                assertThat(get("height").asString).isEqualTo(modifiedHeight)
                assertThat(get("width").asString).isEqualTo(modifiedWidth)
            }
            assertThat(get("shipping_class_id")).isEqualTo(modifiedShippingClassId)
            assertThat(get("shipping_class")).isEqualTo(modifiedShippingClassSlug)
        }
    }

    @Test
    fun `when variation generation succeed, result is received`() =
        runTest {
            // given
            val productId = 6L
            val site = SiteModel()
            val firstVariationAttributes = listOf(
                ProductVariantOption(id = 1, name = "Size", option = "L"),
                ProductVariantOption(id = 2, name = "Color", option = "Blue"),
            )
            val secondVariationAttributes = listOf(
                ProductVariantOption(id = 1, name = "Size", option = "L"),
                ProductVariantOption(id = 2, name = "Color", option = "Red"),
            )

            val variations = listOf(firstVariationAttributes, secondVariationAttributes)

            // when API call succeed
            val variationsPayload = BatchGenerateVariationsPayload(
                site,
                productId,
                variations
            )

            val createdVariationsResponse = List(variationsPayload.variations.size) { index ->
                ProductVariationApiResponse().apply { id = index.toLong() }
            }

            val response = BatchProductVariationsApiResponse(
                createdVariations = createdVariationsResponse
            )

            whenever(
                productRestClient.batchUpdateVariations(
                    any(),
                    any(),
                    any(),
                    anyOrNull(),
                    anyOrNull()
                )
            ) doReturn WooPayload(response)

            val result = productStore.batchGenerateVariations(variationsPayload)

            // then result is expected
            assertThat(result.isError).isFalse
            assertThat(result.model).isNotNull

            // then result is saved in DB
            with(productsVariationsDao.getVariations(site.localId(), RemoteId(productId))) {
                assertThat(this.size).isEqualTo(createdVariationsResponse.size)
            }
        }

    @Test
    fun `when variation generation fails, error is received`() =
        runTest {
            // given
            val productId = 6L
            val site = SiteModel()
            val firstVariationAttributes = listOf(
                ProductVariantOption(id = 1, name = "Size", option = "L"),
                ProductVariantOption(id = 2, name = "Color", option = "Blue"),
            )
            val secondVariationAttributes = listOf(
                ProductVariantOption(id = 1, name = "Size", option = "L"),
                ProductVariantOption(id = 2, name = "Color", option = "Red"),
            )
            val variations = listOf(firstVariationAttributes, secondVariationAttributes)

            // when API call failed
            val variationsPayload = BatchGenerateVariationsPayload(
                site,
                productId,
                variations
            )

            val errorResponse = WooError(GENERIC_ERROR, NETWORK_ERROR, "🔴")
            whenever(
                productRestClient.batchUpdateVariations(
                    any(),
                    any(),
                    any(),
                    anyOrNull(),
                    anyOrNull()
                )
            ) doReturn WooPayload(errorResponse)

            val result = productStore.batchGenerateVariations(variationsPayload)

            // then result is error
            assertThat(result.isError).isTrue

            // then result is NOT saved in DB
            with(productsVariationsDao.getVariations(site.localId(), RemoteId(productId))) {
                assertThat(this.size).isEqualTo(0)
            }
        }

    @Test
    fun `when products bulk update is requested, correct existing and updated products are matched`(): Unit =
        runTest {
            // given
            val existingProducts = listOf(
                ProductTestUtils.generateSampleProduct(42),
                ProductTestUtils.generateSampleProduct(43),
                ProductTestUtils.generateSampleProduct(45),
                ProductTestUtils.generateSampleProduct(48),
            )
            productsDao.upsertProducts(existingProducts)
            val site = SiteModel().apply { id = existingProducts.first().localSiteId.value }
            val updatedProductsInDifferentOrder = existingProducts.sortedByDescending(WCProductModel::remoteProductId)
            val argumentCaptor = argumentCaptor<Map<WCProductModel, WCProductModel>>()
            whenever(
                productRestClient.batchUpdateProducts(
                    eq(site),
                    argumentCaptor.capture()
                )
            ).thenReturn(WooPayload())

            // when
            productStore.batchUpdateProducts(
                BatchUpdateProductsPayload(
                    site,
                    updatedProducts = updatedProductsInDifferentOrder
                )
            )

            // then
            assertThat(argumentCaptor.allValues).hasSize(1)
            assertThat(argumentCaptor.allValues.first()).hasSize(4)
            argumentCaptor.allValues.first().onEach { (existing, updated) ->
                assertThat(existing.remoteProductId).isEqualTo(updated.remoteProductId)
            }
        }

    @Test
    fun `when direct product bulk update returns mixed response, then updated and failed products are returned`(): Unit =
        runTest {
            // given
            val site = SiteModel().apply { id = 1 }
            val updateRequests = mapOf(
                42L to WCProductStore.UpdateProductRequest(name = "Updated"),
                43L to WCProductStore.UpdateProductRequest(name = "Updated"),
            )
            val argumentCaptor = argumentCaptor<Map<Long, WCProductStore.UpdateProductRequest>>()
            whenever(
                productRestClient.batchUpdateProductsPatch(
                    eq(site),
                    argumentCaptor.capture()
                )
            ).thenReturn(
                WooPayload(
                    BatchProductUpdateResult(
                        update = listOf(
                            BatchProductUpdateResult.ProductResponse.Success(
                                ProductWithMetaData(
                                    ProductTestUtils.generateSampleProduct(42).copy(
                                        localSiteId = LocalId(site.id),
                                        remoteId = RemoteId(42),
                                        name = "Updated",
                                    )
                                )
                            ),
                            BatchProductUpdateResult.ProductResponse.Error(
                                id = 43L,
                                error = BatchProductUpdateApiResponse.ErrorResponse(
                                    code = "woocommerce_rest_product_invalid_id",
                                    message = "Invalid ID.",
                                    data = BatchProductUpdateApiResponse.ErrorData(status = 400)
                                )
                            )
                        )
                    )
                )
            )

            // when
            val result = productStore.batchUpdateProducts(site, updateRequests)

            // then
            assertThat(result.isError).isFalse()
            assertThat(requireNotNull(result.model).updatedProducts).containsExactly(42L)
            assertThat(requireNotNull(result.model).failedProducts).hasSize(1)
            with(requireNotNull(result.model).failedProducts.single()) {
                assertThat(id).isEqualTo(43L)
                assertThat(errorCode).isEqualTo("woocommerce_rest_product_invalid_id")
                assertThat(errorMessage).isEqualTo("Invalid ID.")
                assertThat(errorStatus).isEqualTo(400)
            }
            assertThat(argumentCaptor.firstValue).isEqualTo(updateRequests)
        }

    @Test
    fun `when direct product bulk update succeeds, then updated products are saved to database`(): Unit =
        runTest {
            // given
            val site = SiteModel().apply { id = 1 }
            val product = ProductTestUtils.generateSampleProduct(42).copy(
                localSiteId = LocalId(site.id),
                remoteId = RemoteId(42),
                name = "Updated",
            )
            whenever(
                productRestClient.batchUpdateProductsPatch(
                    eq(site),
                    any()
                )
            ).thenReturn(
                WooPayload(
                    BatchProductUpdateResult(
                        update = listOf(
                            BatchProductUpdateResult.ProductResponse.Success(ProductWithMetaData(product))
                        )
                    )
                )
            )

            // when
            val result = productStore.batchUpdateProducts(
                site,
                mapOf(42L to WCProductStore.UpdateProductRequest(name = "Updated"))
            )

            // then
            assertThat(result.isError).isFalse()
            assertThat(productsDao.getProduct(site.id, 42L)?.name).isEqualTo("Updated")
        }

    @Test
    fun `when variation bulk creation, request creation on core`(): Unit =
        runTest {
            // given
            val site = SiteModel()
            val productId = RemoteId(123)
            val variationsToCreate = listOf(
                WCProductVariationModel(LocalId(5)).copy(description = "test1"),
                WCProductVariationModel(LocalId(6)).copy(description = "test2")
            )
            whenever(productRestClient.createVariations(any(), any(), any())) doReturn WooPayload()

            // when
            productStore.createVariations(site, productId, variationsToCreate)

            // then
            verify(productRestClient).createVariations(
                site = site, productId = productId, variations = listOf(
                    mapOf("description" to "test1"),
                    mapOf("description" to "test2")
                )
            )
        }

    @Test
    fun `when product variation bulk creation is successful, save results to database`(): Unit =
        runTest {
            // given
            val site = SiteModel()
            val productId = RemoteId(123)
            val createdVariations = listOf(
                ProductVariationApiResponse().apply { id = 5 },
                ProductVariationApiResponse().apply { id = 6 },
            )
            whenever(productRestClient.createVariations(any(), any(), any())) doReturn WooPayload(
                BatchProductVariationsApiResponse(createdVariations = createdVariations)
            )

            // when
            productStore.createVariations(
                site,
                productId,
                ProductTestUtils.generateSampleVariations(2, productId.value, site.id)
            )

            // then
            val storedVariations = productsVariationsDao.getVariations(site.localId(), productId)
            assertThat(storedVariations).hasSize(2).anyMatch {
                it.remoteVariationId == RemoteId(5L)
            }.anyMatch {
                it.remoteVariationId == RemoteId(6L)
            }
        }

    @Test
    fun `when fetching product with metadata, then save results to database`(): Unit = runTest {
        // given
        val site = SiteModel().apply { id = 1 }
        val product = ProductTestUtils.generateSampleProduct(remoteId = 123, siteId = site.id)
        val metadata = listOf(
            WCMetaData(0, "key1", "value1"),
            WCMetaData(1, "key2", "value2"),
        )
        whenever(productRestClient.fetchSingleProduct(site, product.remoteProductId)).thenReturn(
            RemoteProductPayload(ProductWithMetaData(product, metadata), site)
        )

        // when
        productStore.fetchSingleProduct(FetchSingleProductPayload(site, product.remoteProductId))

        // then
        val storedProduct = productStore.getProductWithMetaData(site, product.remoteProductId)
        assertThat(storedProduct).isNotNull
        assertThat(storedProduct?.product).isEqualTo(product)
        assertThat(storedProduct?.metaData).isEqualTo(metadata)
    }

    @Test
    fun `given include_type simple, then return type Simple`() {
        assertThat(IncludeType.fromValue("simple")).isEqualTo(IncludeType.Simple)
    }

    @Test
    fun `given include_type variable, then return type Variable`() {
        assertThat(
            IncludeType.fromValue("variable")
        ).isEqualTo(IncludeType.Variable)
    }

    @Test
    fun `given include_type external, then return type External`() {
        assertThat(
            IncludeType.fromValue("external")
        ).isEqualTo(IncludeType.External)
    }

    @Test
    fun `given include_type grouped, then return type Grouped`() {
        assertThat(
            IncludeType.fromValue("grouped")
        ).isEqualTo(IncludeType.Grouped)
    }

    @Test
    fun `given include_type empty, then return type null`() {
        assertThat(
            IncludeType.fromValue("")
        ).isNull()
    }

    @Test
    fun `given include_type invalid, then return type null`() {
        assertThat(
            IncludeType.fromValue("invalid")
        ).isNull()
    }

    /* HELPER */

    private fun insertTestAccountAndSiteIntoDb() = TestSiteSqlUtils.insertTestAccountAndSiteIntoDb(wpDatabaseRule.db)
}
