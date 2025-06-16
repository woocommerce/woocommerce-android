package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductSearchResult
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSearchByIdentifierRemoteTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule(UnconfinedTestDispatcher())

    private lateinit var sut: WooPosSearchByIdentifierRemote
    private val selectedSite: SelectedSite = mock()
    private val productsCache: WooPosProductsCache = mock {
        onBlocking { getProductById(any()) }.thenReturn(ProductTestUtils.generateWCProductModel().toAppModel())
    }
    private val productStore: WCProductStore = mock()
    private val variationsCache: WooPosVariationsLRUCache = mock()
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover =
        WooPosSearchByIdentifierCheckDigitRemover()

    private val testSite = SiteModel().apply { id = 1 }

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(testSite)
        sut = WooPosSearchByIdentifierRemote(
            selectedSite,
            productsCache,
            productStore,
            variationsCache,
            checkDigitRemover,
        )
    }

    @Test
    fun `given WooPosSearchByIdentifierRemote created, when onCleanup called, then no errors occur`() {
        // GIVEN
        // Remote searcher is created

        // WHEN
        sut.onCleanup()

        // THEN
        assertTrue(true)
    }

    @Test
    fun `given remote searcher initialized, when constructor called, then searcher is created successfully`() {
        // GIVEN & WHEN
        val remoteSearcher = WooPosSearchByIdentifierRemote(
            selectedSite,
            productsCache,
            productStore,
            variationsCache,
            checkDigitRemover,
        )

        // Test that object creation succeeds without exceptions
        assertEquals("WooPosSearchByIdentifierRemote", remoteSearcher::class.simpleName)
    }

    @Test
    fun `when invoke is called with identifier, searches both SKU and global unique ID`() = runTest {
        // GIVEN
        val identifier = "test-sku"
        val wcProductModel: WCProductModel = ProductTestUtils.generateWCProductModel()
        val searchResult = ProductSearchResult(listOf(wcProductModel), false)
        
        whenever(productStore.searchProducts(
            site = eq(testSite),
            searchString = eq(identifier),
            skuSearchOptions = eq(WCProductStore.SkuSearchOptions.ExactSearch),
            offset = eq(0),
            pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
            filterOptions = eq(emptyMap())
        )).thenReturn(WooResult(searchResult))

        whenever(productStore.searchProductsByGlobalUniqueId(
            site = eq(testSite),
            globalUniqueId = eq(identifier),
            offset = eq(0),
            pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
            filterOptions = eq(emptyMap())
        )).thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN)))

        // WHEN
        val result = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Success)
        assertTrue(
            wcProductModel.toAppModel()
                .isSameProduct((result as WooPosSearchByIdentifierResult.Success).product)
        )
    }

    @Test
    fun `given two invoke calls with same identifier, when results are returned, then both calls receive results`() =
        runTest {
            // GIVEN
            val identifier = "test-sku"
            val wcProductModel: WCProductModel = ProductTestUtils.generateWCProductModel()
            val searchResult = ProductSearchResult(listOf(wcProductModel), false)
            
            whenever(productStore.searchProducts(
                site = eq(testSite),
                searchString = eq(identifier),
                skuSearchOptions = any(),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap())
            )).thenReturn(WooResult(searchResult))

            var firstResult: WooPosSearchByIdentifierResult? = null
            var secondResult: WooPosSearchByIdentifierResult? = null

            // WHEN
            val job1 = launch {
                firstResult = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
            }

            val job2 = launch {
                secondResult = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
            }

            // THEN
            job1.join()
            job2.join()

            assertTrue(firstResult is WooPosSearchByIdentifierResult.Success)
            assertTrue(secondResult is WooPosSearchByIdentifierResult.Success)
            assertTrue(
                wcProductModel.toAppModel()
                    .isSameProduct((firstResult as WooPosSearchByIdentifierResult.Success).product)
            )
            assertTrue(
                wcProductModel.toAppModel()
                    .isSameProduct((secondResult as WooPosSearchByIdentifierResult.Success).product)
            )
        }

    @Test
    fun `given different identifiers, when invoke called for each, then searches for each identifier`() =
        runTest {
            // GIVEN
            val identifier1 = "test-sku-1"
            val identifier2 = "test-sku-2"
            val wcProductModel1 = ProductTestUtils.generateWCProductModel()
            val wcProductModel2 = ProductTestUtils.generateWCProductModel()
            val searchResult1 = ProductSearchResult(listOf(wcProductModel1), false)
            val searchResult2 = ProductSearchResult(listOf(wcProductModel2), false)
            
            whenever(productStore.searchProducts(
                site = eq(testSite),
                searchString = eq(identifier1),
                skuSearchOptions = any(),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap())
            )).thenReturn(WooResult(searchResult1))
            
            whenever(productStore.searchProductsByGlobalUniqueId(
                site = eq(testSite),
                globalUniqueId = eq(identifier1),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap())
            )).thenReturn(WooResult(searchResult1))
            
            whenever(productStore.searchProducts(
                site = eq(testSite),
                searchString = eq(identifier2),
                skuSearchOptions = any(),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap())
            )).thenReturn(WooResult(searchResult2))
            
            whenever(productStore.searchProductsByGlobalUniqueId(
                site = eq(testSite),
                globalUniqueId = eq(identifier2),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap())
            )).thenReturn(WooResult(searchResult2))

            // WHEN
            val result1 = sut.invoke(identifier1, WooPosBarcodeFormat.FormatUnknown)
            val result2 = sut.invoke(identifier2, WooPosBarcodeFormat.FormatUnknown)

            // THEN
            assertTrue(result1 is WooPosSearchByIdentifierResult.Success)
            assertTrue(result2 is WooPosSearchByIdentifierResult.Success)
            
            verify(productStore).searchProducts(
                site = eq(testSite),
                searchString = eq(identifier1),
                skuSearchOptions = any(),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap())
            )
            
            verify(productStore).searchProducts(
                site = eq(testSite),
                searchString = eq(identifier2),
                skuSearchOptions = any(),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap())
            )
        }

    @Test
    fun `given variable product found, when variations fetched, then returns first matching variation`() = runTest {
        // GIVEN
        val identifier = "VAR-SKU-123"
        val parentProductId = 456L
        val variationId = 789L
        
        val variableProduct = ProductTestUtils.generateWCProductModel().copy(
            remoteId = RemoteId(variationId),
            parentId = parentProductId,
            type = "variation"
        )
        
        val parentProduct = ProductTestUtils.generateWCProductModel().copy(
            remoteId = RemoteId(parentProductId),
            type = "variable"
        )
        
        val variation = WCProductVariationModel(
            localSiteId = LocalId(testSite.id),
            remoteProductId = RemoteId(parentProductId),
            remoteVariationId = RemoteId(variationId)
        )
        
        val searchResult = ProductSearchResult(listOf(variableProduct), false)
        
        whenever(productStore.searchProducts(
            site = eq(testSite),
            searchString = eq(identifier),
            skuSearchOptions = any(),
            offset = eq(0),
            pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
            filterOptions = eq(emptyMap())
        )).thenReturn(WooResult(searchResult))

        whenever(productStore.searchProductsByGlobalUniqueId(
            site = eq(testSite),
            globalUniqueId = eq(identifier),
            offset = eq(0),
            pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
            filterOptions = eq(emptyMap())
        )).thenReturn(WooResult(searchResult))
        
        whenever(productStore.fetchSingleVariation(
            eq(testSite),
            eq(parentProductId),
            eq(variationId)
        )).thenReturn(WCProductStore.OnVariationChanged(variationId))
        
        whenever(productStore.getVariationByRemoteId(
            eq(testSite),
            eq(parentProductId),
            eq(variationId)
        )).thenReturn(variation)
        
        whenever(productStore.fetchSingleProduct(any())).thenReturn(
            WCProductStore.OnProductChanged(parentProductId)
        )
        
        whenever(productStore.getProduct(eq(testSite), eq(parentProductId))).thenReturn(parentProduct)
        
        whenever(productsCache.getProductById(eq(parentProductId))).thenReturn(null)
        whenever(variationsCache.get(eq(variationId))).thenReturn(null)

        // WHEN
        val result = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
        val successResult = result as WooPosSearchByIdentifierResult.VariationSuccess
        assertEquals(parentProduct.toAppModel(), successResult.parentProduct)
        assertEquals(variation.toAppModel(), successResult.variation)
    }

    @Test
    fun `given search error, when invoke called, then returns network error`() = runTest {
        // GIVEN
        val identifier = "test-sku"
        
        whenever(productStore.searchProductsByGlobalUniqueId(
            site = eq(testSite),
            globalUniqueId = eq(identifier),
            offset = eq(0),
            pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
            filterOptions = eq(emptyMap())
        )).thenReturn(WooResult(WooError(WooErrorType.API_ERROR, GenericErrorType.NETWORK_ERROR)))
        
        whenever(productStore.searchProducts(
            site = eq(testSite),
            searchString = eq(identifier),
            skuSearchOptions = any(),
            offset = eq(0),
            pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
            filterOptions = eq(emptyMap())
        )).thenReturn(WooResult(WooError(WooErrorType.API_ERROR, GenericErrorType.NETWORK_ERROR)))

        // WHEN
        val result = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(
            WooPosSearchByIdentifierResult.Error.NetworkError,
            (result as WooPosSearchByIdentifierResult.Failure).error
        )
    }

    @Test
    fun `given no products found, when invoke called, then returns product not found`() = runTest {
        // GIVEN
        val identifier = "test-sku"
        val emptySearchResult = ProductSearchResult(emptyList(), false)
        
        whenever(productStore.searchProducts(
            site = eq(testSite),
            searchString = eq(identifier),
            skuSearchOptions = any(),
            offset = eq(0),
            pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
            filterOptions = eq(emptyMap())
        )).thenReturn(WooResult(emptySearchResult))

        whenever(productStore.searchProductsByGlobalUniqueId(
            site = eq(testSite),
            globalUniqueId = eq(identifier),
            offset = eq(0),
            pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
            filterOptions = eq(emptyMap())
        )).thenReturn(WooResult(emptySearchResult))

        // WHEN
        val result = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(
            WooPosSearchByIdentifierResult.Error.ProductNotFound,
            (result as WooPosSearchByIdentifierResult.Failure).error
        )
    }

    @Test
    fun `given global unique ID search, when product found with matching ID, then returns success`() = runTest {
        // GIVEN
        val globalUniqueId = "1234567890"
        val wcProductModel = ProductTestUtils.generateWCProductModel().copy(
            globalUniqueId = globalUniqueId
        )
        val searchResult = ProductSearchResult(listOf(wcProductModel), false)
        
        whenever(productStore.searchProductsByGlobalUniqueId(
            site = eq(testSite),
            globalUniqueId = eq(globalUniqueId),
            offset = eq(0),
            pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
            filterOptions = eq(emptyMap())
        )).thenReturn(WooResult(searchResult))
        
        whenever(productStore.searchProducts(
            site = eq(testSite),
            searchString = eq(globalUniqueId),
            skuSearchOptions = eq(WCProductStore.SkuSearchOptions.ExactSearch),
            offset = eq(0),
            pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
            filterOptions = eq(emptyMap())
        )).thenReturn(WooResult(ProductSearchResult(emptyList(), false)))

        // WHEN
        val result = sut.invoke(globalUniqueId, WooPosBarcodeFormat.FormatUnknown)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Success)
        assertTrue(
            wcProductModel.toAppModel()
                .isSameProduct((result as WooPosSearchByIdentifierResult.Success).product)
        )
    }
}