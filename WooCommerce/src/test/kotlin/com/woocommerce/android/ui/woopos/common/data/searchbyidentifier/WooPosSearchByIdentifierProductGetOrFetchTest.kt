package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType

class WooPosSearchByIdentifierProductGetOrFetchTest {

    private lateinit var sut: WooPosSearchByIdentifierProductGetOrFetch
    private val selectedSite: SelectedSite = mock()
    private val productStore: WCProductStore = mock()
    private val productsCache: WooPosProductsCache = mock()
    private val site: SiteModel = mock()
    private val errorMapper: WooPosSearchByIdentifierProductErrorMapper = WooPosSearchByIdentifierProductErrorMapper()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierProductGetOrFetch(selectedSite, productStore, productsCache, errorMapper)
        whenever(selectedSite.get()).thenReturn(site)
    }

    @Test
    fun `given successful fetch with product, when invoke called, then return success and cache product`() = runTest {
        // GIVEN
        val productId = 123L
        val wcProduct = ProductTestUtils.generateWCProductModel()
        val product = wcProduct.toAppModel()
        val result: WCProductStore.OnProductChanged = mock {
            on { isError }.thenReturn(false)
        }

        whenever(
            productStore.fetchSingleProduct(any())
        ).thenReturn(result)

        whenever(productStore.getProduct(site, productId)).thenReturn(wcProduct)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertTrue(actualResult is WooPosSearchByIdentifierResult.Success)
        val successResult = actualResult as WooPosSearchByIdentifierResult.Success
        assertEquals(product.remoteId, successResult.product.remoteId)
        assertEquals(product.name, successResult.product.name)
    }

    @Test
    fun `given product found in cache, when invoke called, then return cached product without fetching`() = runTest {
        // GIVEN
        val productId = 123L
        val cachedProduct = ProductTestUtils.generateProduct(productId)
        whenever(productsCache.getProductById(productId)).thenReturn(cachedProduct)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertTrue(actualResult is WooPosSearchByIdentifierResult.Success)
        val successResult = actualResult as WooPosSearchByIdentifierResult.Success
        assertEquals(cachedProduct.remoteId, successResult.product.remoteId)
    }

    @Test
    fun `given successful fetch but product not found in store, when invoke called, then return failure with unknown error`() = runTest {
        // GIVEN
        val productId = 123L
        val result: WCProductStore.OnProductChanged = mock {
            on { isError }.thenReturn(false)
        }

        whenever(
            productStore.fetchSingleProduct(any())
        ).thenReturn(result)

        whenever(productStore.getProduct(site, productId)).thenReturn(null)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.UnknownError("Product not found for ID: $productId")
            ),
            actualResult
        )
    }

    @Test
    fun `given invalid product id error, when invoke called, then return failure with not found error`() = runTest {
        // GIVEN
        val productId = 123L
        val error = ProductError(ProductErrorType.INVALID_PRODUCT_ID, "Invalid product ID")
        val result = WCProductStore.OnProductChanged().apply {
            this.error = error
        }

        whenever(productsCache.getProductById(productId)).thenReturn(null)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(result)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound),
            actualResult
        )
    }

    @Test
    fun `given invalid param error, when invoke called, then return failure with server error`() = runTest {
        // GIVEN
        val productId = 123L
        val error = ProductError(ProductErrorType.INVALID_PARAM, "Invalid parameter")
        val result = WCProductStore.OnProductChanged().apply {
            this.error = error
        }

        whenever(productsCache.getProductById(productId)).thenReturn(null)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(result)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ServerError("Invalid parameter")
            ),
            actualResult
        )
    }

    @Test
    fun `given duplicate sku error, when invoke called, then return failure with server error`() = runTest {
        // GIVEN
        val productId = 123L
        val error = ProductError(ProductErrorType.DUPLICATE_SKU, "Duplicate SKU")
        val result = WCProductStore.OnProductChanged().apply {
            this.error = error
        }

        whenever(productsCache.getProductById(productId)).thenReturn(null)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(result)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ServerError("Duplicate SKU")
            ),
            actualResult
        )
    }

    @Test
    fun `given generic error, when invoke called, then return failure with unknown error`() = runTest {
        // GIVEN
        val productId = 123L
        val error = ProductError(ProductErrorType.GENERIC_ERROR, "Generic error occurred")
        val result = WCProductStore.OnProductChanged().apply {
            this.error = error
        }

        whenever(productsCache.getProductById(productId)).thenReturn(null)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(result)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.UnknownError("Generic error occurred")
            ),
            actualResult
        )
    }

    @Test
    fun `given parse error, when invoke called, then return failure with server error`() = runTest {
        // GIVEN
        val productId = 123L
        val error = ProductError(ProductErrorType.PARSE_ERROR, "Parse error occurred")
        val result = WCProductStore.OnProductChanged().apply {
            this.error = error
        }

        whenever(productsCache.getProductById(productId)).thenReturn(null)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(result)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ServerError("Parse error occurred")
            ),
            actualResult
        )
    }

    @Test
    fun `given invalid review id error, when invoke called, then return failure with not found error`() = runTest {
        // GIVEN
        val productId = 123L
        val error = ProductError(ProductErrorType.INVALID_REVIEW_ID, "Invalid review ID")
        val result = WCProductStore.OnProductChanged().apply {
            this.error = error
        }

        whenever(productsCache.getProductById(productId)).thenReturn(null)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(result)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound),
            actualResult
        )
    }

    @Test
    fun `given invalid image id error, when invoke called, then return failure with not found error`() = runTest {
        // GIVEN
        val productId = 123L
        val error = ProductError(ProductErrorType.INVALID_IMAGE_ID, "Invalid image ID")
        val result = WCProductStore.OnProductChanged().apply {
            this.error = error
        }

        whenever(productsCache.getProductById(productId)).thenReturn(null)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(result)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound),
            actualResult
        )
    }
}
