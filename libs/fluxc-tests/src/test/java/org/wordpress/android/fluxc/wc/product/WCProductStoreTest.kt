package org.wordpress.android.fluxc.wc.product

import androidx.room.Room
import com.google.gson.JsonObject
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.BatchProductVariationsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductVariationApiResponse
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.ProductStorageHelper
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.BatchGenerateVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.BatchUpdateProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.BatchUpdateVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateVariationPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateVariationPayload
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.wc.utils.SiteTestUtils
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
class WCProductStoreTest {
    private val productRestClient: ProductRestClient = mock()
    private lateinit var productStore: WCProductStore

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        WCProductModel::class.java,
                        WCProductVariationModel::class.java,
                        WCProductCategoryModel::class.java,
                        WCProductReviewModel::class.java,
                        SiteModel::class.java,
                        AccountModel::class.java
                ),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        val roomDb = Room.inMemoryDatabaseBuilder(appContext, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val productStorageHelper = ProductStorageHelper(
            productSqlUtils = ProductSqlUtils,
            metaDataDao = roomDb.metaDataDao
        )
        productStore = WCProductStore(
            Dispatcher(),
            productRestClient,
            addonsDao = mock(),
            logger = mock(),
            productStorageHelper = productStorageHelper,
            coroutineEngine = initCoroutineEngine()
        )
    }

    @Test
    fun testSimpleInsertionAndRetrieval() {
        val productModel = ProductTestUtils.generateSampleProduct(42)
        val site = SiteModel().apply { id = productModel.localSiteId }

        ProductSqlUtils.insertOrUpdateProduct(productModel)

        val storedProduct = productStore.getProductByRemoteId(site, productModel.remoteProductId)
        assertEquals(42, storedProduct?.remoteProductId)
        assertEquals(productModel, storedProduct)
    }

    @Test
    fun testGetProductsBySiteAndProductIds() {
        val productIds = listOf<Long>(30, 31, 2)

        val product1 = ProductTestUtils.generateSampleProduct(30)
        val product2 = ProductTestUtils.generateSampleProduct(31)
        val product3 = ProductTestUtils.generateSampleProduct(42)

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        val site = SiteModel().apply { id = product1.localSiteId }
        val products = productStore.getProductsByRemoteIds(site, productIds)
        assertEquals(2, products.size)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(10, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(11, siteId = 10)
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(2, siteId = 10)

        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct1)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct2)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct3)

        // verify that the products for the first site is still 2
        assertEquals(2, productStore.getProductsByRemoteIds(site, productIds).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId }
        val differentSiteProducts = productStore.getProductsByRemoteIds(site2, listOf(10, 11, 2))
        assertEquals(3, differentSiteProducts.size)
        assertEquals(differentSiteProduct1.remoteProductId, differentSiteProducts[0].remoteProductId)
        assertEquals(differentSiteProduct2.remoteProductId, differentSiteProducts[1].remoteProductId)
        assertEquals(differentSiteProduct3.remoteProductId, differentSiteProducts[2].remoteProductId)
    }

    @Test
    fun testUpdateProduct() {
        val productModel = ProductTestUtils.generateSampleProduct(42).apply {
            name = "test product"
            description = "test description"
        }
        val site = SiteModel().apply { id = productModel.localSiteId }
        ProductSqlUtils.insertOrUpdateProduct(productModel)

        // Simulate incoming action with updated product model
        val payload = RemoteUpdateProductPayload(site,
            ProductWithMetaData(
                productModel.apply {
                    description = "Updated description"
                }
            )
        )
        productStore.onAction(WCProductActionBuilder.newUpdatedProductAction(payload))

        with(productStore.getProductByRemoteId(site, productModel.remoteProductId)) {
            // The version of the product model in the database should have the updated description
            assertEquals(productModel.description, this?.description)
            // Other fields should not be altered by the update
            assertEquals(productModel.name, this?.name)
        }
    }

    @Test
    fun testUpdateVariation() = runBlocking {
        val variationModel = ProductTestUtils.generateSampleVariation(42, 24).apply {
            description = "test description"
        }
        val site = SiteModel().apply { id = variationModel.localSiteId }
        whenever(productRestClient.updateVariation(site, null, variationModel))
            .thenReturn(RemoteUpdateVariationPayload(site, variationModel))

        productStore.updateVariation(UpdateVariationPayload(site, variationModel))

        with(productStore.getVariationByRemoteId(
                site,
                variationModel.remoteProductId,
                variationModel.remoteVariationId
        )) {
            // The version of the product model in the database should have the updated description
            assertEquals(variationModel.description, this?.description)
            // Other fields should not be altered by the update
            assertEquals(variationModel.status, this?.status)
        }
    }

    @Test
    fun testVerifySkuExistsLocally() {
        val sku = "woo-cap"
        val productModel = ProductTestUtils.generateSampleProduct(42).apply {
            name = "test product"
            description = "test description"
            this.sku = sku
        }
        val site = SiteModel().apply { id = productModel.localSiteId }
        ProductSqlUtils.insertOrUpdateProduct(productModel)

        // verify if product with sku: woo-cap exists in local cache
        val skuAvailable = ProductSqlUtils.getProductExistsBySku(site, sku)
        assertTrue(skuAvailable)

        // verify if product with non existent sku returns false
        assertFalse(ProductSqlUtils.getProductExistsBySku(site, "woooo"))
    }

    @Test
    fun testGetProductsWithFilterOptions() {
        val filterOptions = mapOf<ProductFilterOption, String>(
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
                43, categories = "[{\"id\":1337,\"name\":\"Clothing\",\"slug\":\"clothing\"}]")

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)
        ProductSqlUtils.insertOrUpdateProduct(product4)

        val site = SiteModel().apply { id = product1.localSiteId }
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
                13, siteId = 10, categories = "[{\"id\":1337,\"name\":\"Clothing\",\"slug\":\"clothing\"}]")

        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct1)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct2)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct3)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct4)

        // verify that the products for the first site is still 1
        assertEquals(1, productStore.getProducts(site, filterOptions).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId }
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
    fun `test AddProduct Action triggered correctly`() {
        // given
        val productModel = ProductTestUtils.generateSampleProduct(remoteId = 0).apply {
            name = "test new product"
            description = "test new description"
        }
        val site = SiteModel().apply { id = productModel.localSiteId }

        // when
        ProductSqlUtils.insertOrUpdateProduct(productModel)
        val payload = RemoteAddProductPayload(site, ProductWithMetaData(productModel))
        productStore.onAction(WCProductActionBuilder.newAddedProductAction(payload))

        // then
        with(productStore.getProductByRemoteId(site, productModel.remoteProductId)) {
            assertNotNull(this)
            assertEquals(productModel.description, this.description)
            assertEquals(productModel.name, this.name)
        }
    }

    @Test
    fun `test that product insert emits a flow event`() = test {
        // given
        val productModel = ProductTestUtils.generateSampleProduct(remoteId = 0).apply {
            name = "test new product"
            description = "test new description"
        }
        val productList = ProductTestUtils.generateProductList()
        ProductSqlUtils.insertOrUpdateProducts(productList)
        val site = SiteModel().apply { id = productModel.localSiteId }

        var observedProducts = productStore.observeProducts(site).first()
        assertThat(observedProducts).isEqualTo(productList)

        // when
        ProductSqlUtils.insertOrUpdateProduct(productModel)
        observedProducts = productStore.observeProducts(site).first()

        // then
        assertThat(observedProducts).isEqualTo(productList + productModel)
    }

    @Test
    fun `test that variation insert emits a flow event`() = test {
        // given
        val variation = ProductTestUtils.generateSampleVariation(
            remoteId = 0,
            variationId = 1
        ).apply {
            description = "test new description"
        }
        val site = SiteModel().apply { id = variation.localSiteId }
        val variations = ProductTestUtils.generateSampleVariations(
            number = 5,
            productId = variation.remoteProductId,
            siteId = site.id
        )
        ProductSqlUtils.insertOrUpdateProductVariations(variations)

        var observedVariations = productStore.observeVariations(site, variation.remoteProductId)
            .first()
        assertThat(observedVariations).isEqualTo(variations)

        // when
        ProductSqlUtils.insertOrUpdateProductVariation(variation)
        observedVariations = productStore.observeVariations(site, variation.remoteProductId).first()

        // then
        assertThat(observedVariations).isEqualTo(variations + variation)
    }

    @Test
    fun `given product review exists, when fetch product review, then local database updated`() = runBlocking {
        val site = SiteTestUtils.insertTestAccountAndSiteIntoDb()
        val productModel = ProductTestUtils.generateSampleProduct(remoteId = 1).apply {
            localSiteId = site.id
        }
        ProductSqlUtils.insertOrUpdateProduct(productModel)

        val reviewModel = ProductTestUtils.generateSampleProductReview(
                productModel.remoteProductId,
                123L,
                site.id
        )
        whenever(productRestClient.fetchProductReviewById(site, reviewModel.remoteProductReviewId))
                .thenReturn(RemoteProductReviewPayload(site, reviewModel))

        productStore.fetchSingleProductReview(FetchSingleProductReviewPayload(site, reviewModel.remoteProductReviewId))

        assertThat(productStore.getProductReviewByRemoteId(site.id, reviewModel.remoteProductReviewId)).isNotNull
        Unit
    }

    @Test
    fun `batch variations update should return positive result on successful backend request`() =
        runBlocking {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            val site = SiteModel().apply { id = product.localSiteId }
            val variations = ProductTestUtils.generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )

            // when
            val variationsIds = variations.map { it.remoteVariationId }
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
        runBlocking {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            val site = SiteModel().apply { id = product.localSiteId }
            val variations = ProductTestUtils.generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )

            // when
            val variationsIds = variations.map { it.remoteVariationId }
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
        runBlocking {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            ProductSqlUtils.insertOrUpdateProduct(product)
            val site = SiteTestUtils.insertTestAccountAndSiteIntoDb()
            val variations = ProductTestUtils.generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )
            ProductSqlUtils.insertOrUpdateProductVariations(variations)

            // when
            val variationsIds = variations.map { it.remoteVariationId }
            val newRegularPrice = "1.234 💰"
            val newSalePrice = "0.234 💰"
            val variationsUpdatePayload =
                BatchUpdateVariationsPayload.Builder(site, product.remoteProductId, variationsIds)
                    .regularPrice(newRegularPrice)
                    .salePrice(newSalePrice)
                    .build()
            val variationsReturnedFromBackend = variations.map {
                ProductVariationApiResponse().apply {
                    id = it.remoteVariationId
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
            with(ProductSqlUtils.getVariationsForProduct(site, product.remoteProductId)) {
                forEach { variation ->
                    assertThat(variation.regularPrice).isEqualTo(newRegularPrice)
                    assertThat(variation.salePrice).isEqualTo(newSalePrice)
                }
            }
            Unit
        }

    @Test
    fun `batch variations update should not update variations locally after failed backend request`() =
        runBlocking {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            ProductSqlUtils.insertOrUpdateProduct(product)
            val site = SiteTestUtils.insertTestAccountAndSiteIntoDb()
            val variations = ProductTestUtils.generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )
            ProductSqlUtils.insertOrUpdateProductVariations(variations)

            // when
            val variationsIds = variations.map { it.remoteVariationId }
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
            with(ProductSqlUtils.getVariationsForProduct(site, product.remoteProductId)) {
                forEach { variation ->
                    assertThat(variation.regularPrice).isNotEqualTo(newRegularPrice)
                    assertThat(variation.salePrice).isNotEqualTo(newSalePrice)
                }
            }
            Unit
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
            variations.map { it.remoteVariationId }
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
        test {
            // given
            val productId = 6L
            val site = SiteModel()
            val firstVariationAttributes = listOf(
                ProductVariantOption( id = 1, name = "Size", option = "L"),
                ProductVariantOption( id = 2, name = "Color", option = "Blue"),
            )
            val secondVariationAttributes = listOf(
                ProductVariantOption( id = 1, name = "Size", option = "L"),
                ProductVariantOption( id = 2, name = "Color", option = "Red"),
            )

            val variations = listOf(firstVariationAttributes,secondVariationAttributes)

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
            with(ProductSqlUtils.getVariationsForProduct(site, productId)) {
                assertThat(this.size).isEqualTo(createdVariationsResponse.size)
            }
        }

    @Test
    fun `when variation generation fails, error is received`() =
        test {
            // given
            val productId = 6L
            val site = SiteModel()
            val firstVariationAttributes = listOf(
                ProductVariantOption( id = 1, name = "Size", option = "L"),
                ProductVariantOption( id = 2, name = "Color", option = "Blue"),
            )
            val secondVariationAttributes = listOf(
                ProductVariantOption( id = 1, name = "Size", option = "L"),
                ProductVariantOption( id = 2, name = "Color", option = "Red"),
            )
            val variations = listOf(firstVariationAttributes,secondVariationAttributes)

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
            with(ProductSqlUtils.getVariationsForProduct(site, productId)) {
                assertThat(this.size).isEqualTo(0)
            }
        }

    @Test
    fun `when products bulk update is requested, correct existing and updated products are matched`(): Unit =
        runBlocking {
            // given
            val existingProducts = listOf(
                ProductTestUtils.generateSampleProduct(42),
                ProductTestUtils.generateSampleProduct(43),
                ProductTestUtils.generateSampleProduct(45),
                ProductTestUtils.generateSampleProduct(48),
            )
            ProductSqlUtils.insertOrUpdateProducts(existingProducts)
            val site = SiteModel().apply { id = existingProducts.first().localSiteId }
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
            argumentCaptor.allValues.first().onEach {(existing, updated) ->
                assertThat(existing.remoteProductId).isEqualTo(updated.remoteProductId)
            }
        }

    @Test
    fun `when variation bulk creation, request creation on core`(): Unit =
        runBlocking {
            // given
            val site = SiteModel()
            val productId = RemoteId(123)
            val variationsToCreate = listOf(
                WCProductVariationModel(5).apply { description = "test$id" },
                WCProductVariationModel(6).apply { description = "test$id" }
            )
            whenever(productRestClient.createVariations(any(), any(), any())) doReturn WooPayload()

            // when
            productStore.createVariations(site, productId, variationsToCreate)

            // then
            verify(productRestClient).createVariations(
                site = site, productId = productId, variations = listOf(
                mapOf("description" to "test5"),
                mapOf("description" to "test6")
            )
            )
        }

    @Test
    fun `when product variation bulk creation is successful, save results to database`(): Unit =
        runBlocking {
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
            val storedVariations = ProductSqlUtils.getVariationsForProduct(site, productId.value)
            assertThat(storedVariations).hasSize(2).anyMatch {
                it.remoteVariationId == 5L
            }.anyMatch {
                it.remoteVariationId == 6L
            }
        }

    @Test
    fun `when fetching product with metadata, then save results to database`(): Unit = runBlocking {
        // given
        val site = SiteModel().apply { id = 1 }
        val product = ProductTestUtils.generateSampleProduct(remoteId = 123, siteId = site.id)
        val metadata = listOf(
            WCMetaData(0, "key1", "value1"),
            WCMetaData(1, "key2", "value2"),
        )
        whenever(productRestClient.fetchSingleProduct(site, product.remoteProductId)).thenReturn(
            WCProductStore.RemoteProductPayload(ProductWithMetaData(product, metadata), site)
        )

        // when
        productStore.fetchSingleProduct(WCProductStore.FetchSingleProductPayload(site, product.remoteProductId))

        // then
        val storedProduct = productStore.getProductWithMetaData(site, product.remoteProductId)
        assertThat(storedProduct).isNotNull
        assertThat(storedProduct?.product).isEqualTo(product)
        assertThat(storedProduct?.metaData).isEqualTo(metadata)
    }

    @Test
    fun `given include_type simple, then return type Simple` () {
        assertThat(WCProductStore.IncludeType.fromValue("simple")).isEqualTo(WCProductStore.IncludeType.Simple)
    }

    @Test
    fun `given include_type variable, then return type Variable` () {
        assertThat(
            WCProductStore.IncludeType.fromValue("variable")
        ).isEqualTo(WCProductStore.IncludeType.Variable)
    }

    @Test
    fun `given include_type external, then return type External` () {
        assertThat(
            WCProductStore.IncludeType.fromValue("external")
        ).isEqualTo(WCProductStore.IncludeType.External)
    }

    @Test
    fun `given include_type grouped, then return type Grouped` () {
        assertThat(
            WCProductStore.IncludeType.fromValue("grouped")
        ).isEqualTo(WCProductStore.IncludeType.Grouped)
    }

    @Test
    fun `given include_type empty, then return type null` () {
        assertThat(
            WCProductStore.IncludeType.fromValue("")
        ).isNull()
    }

    @Test
    fun `given include_type invalid, then return type null` () {
        assertThat(
            WCProductStore.IncludeType.fromValue("invalid")
        ).isNull()
    }
}
