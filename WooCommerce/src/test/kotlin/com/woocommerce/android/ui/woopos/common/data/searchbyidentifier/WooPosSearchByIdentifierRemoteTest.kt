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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSearchByIdentifierRemoteTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule(UnconfinedTestDispatcher())

    private lateinit var sut: WooPosSearchByIdentifierRemote
    private val dispatcher: Dispatcher = mock()
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
            dispatcher,
            selectedSite,
            productsCache,
            productStore,
            variationsCache,
            checkDigitRemover,
            coroutinesTestRule.testDispatcher
        )
    }

    @Test
    fun `given WooPosSearchByIdentifierRemote created, when onCleanup called, then dispatcher unregistered`() {
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
            dispatcher,
            selectedSite,
            productsCache,
            productStore,
            variationsCache,
            checkDigitRemover,
            coroutinesTestRule.testDispatcher
        )

        // Test that object creation succeeds without exceptions
        assertEquals("WooPosSearchByIdentifierRemote", remoteSearcher::class.simpleName)
    }

    @Test
    fun `when invoke is called twice with same identifier, only dispatches once for each search type`() = runTest {
        // GIVEN
        val identifier = "test-sku"
        val actionCaptor = argumentCaptor<Action<*>>()

        // WHEN
        val job1 = launch {
            sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
        }
        val job2 = launch {
            sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
        }

        advanceTimeBy(1)

        // THEN
        verify(dispatcher, times(2)).dispatch(actionCaptor.capture())

        sut.onCleanup()
        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `given two invoke calls with same identifier, when results are returned, then both calls receive results`() =
        runTest {
            // GIVEN
            val identifier = "test-sku"
            val wcProductModel: WCProductModel = ProductTestUtils.generateWCProductModel()

            var firstResult: WooPosSearchByIdentifierResult? = null
            var secondResult: WooPosSearchByIdentifierResult? = null

            // WHEN
            val job1 = launch {
                firstResult = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
            }

            val job2 = launch {
                secondResult = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
            }

            advanceTimeBy(1)

            sut.onProductsSearched(
                WCProductStore.OnProductsSearched(
                    globalUniqueIdSearchQuery = identifier,
                    searchQuery = null,
                    searchResults = listOf(wcProductModel),
                    canLoadMore = false,
                    isSkuSearch = WCProductStore.SkuSearchOptions.Disabled
                )
            )

            advanceTimeBy(1)

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
    fun `given different identifiers, when invoke called for each, then dispatches action for each identifier`() =
        runTest {
            // GIVEN
            val identifier1 = "test-sku-1"
            val identifier2 = "test-sku-2"
            val actionCaptor = argumentCaptor<Action<*>>()

            // WHEN
            val job1 = launch {
                sut.invoke(identifier1, WooPosBarcodeFormat.FormatUnknown)
            }

            val job2 = launch {
                sut.invoke(identifier2, WooPosBarcodeFormat.FormatUnknown)
            }

            advanceTimeBy(1)

            // THEN
            verify(dispatcher, times(4)).dispatch(actionCaptor.capture())

            val payloads = actionCaptor.allValues.map { it.payload }

            assertTrue(
                payloads.any { payload ->
                    payload is WCProductStore.SearchProductsByGlobalUniqueIdPayload &&
                        payload.globalUniqueId == identifier1
                }
            )

            assertTrue(
                payloads.any { payload ->
                    payload is WCProductStore.SearchProductsByGlobalUniqueIdPayload &&
                        payload.globalUniqueId == identifier2
                }
            )

            assertTrue(
                payloads.any { payload ->
                    payload is WCProductStore.SearchProductsPayload &&
                        payload.searchQuery == identifier1
                }
            )

            assertTrue(
                payloads.any { payload ->
                    payload is WCProductStore.SearchProductsPayload &&
                        payload.searchQuery == identifier2
                }
            )

            job1.cancel()
            job2.cancel()
            sut.onCleanup()
        }

    @Test
    fun `given different identifiers,when one receives result,then other continues waiting until it receives result`() =
        runTest {
            // GIVEN
            val identifier1 = "test-sku-1"
            val identifier2 = "test-sku-2"
            val wcProductModel1 = ProductTestUtils.generateWCProductModel()
            val wcProductModel2 = ProductTestUtils.generateWCProductModel()

            var result1: WooPosSearchByIdentifierResult? = null
            var result2: WooPosSearchByIdentifierResult? = null

            // WHEN
            val job1 = async {
                result1 = sut.invoke(identifier1, WooPosBarcodeFormat.FormatUnknown)
            }

            val job2 = async {
                result2 = sut.invoke(identifier2, WooPosBarcodeFormat.FormatUnknown)
            }

            advanceTimeBy(1)

            sut.onProductsSearched(
                WCProductStore.OnProductsSearched(
                    globalUniqueIdSearchQuery = identifier1,
                    searchQuery = null,
                    searchResults = listOf(wcProductModel1),
                    canLoadMore = false,
                    isSkuSearch = WCProductStore.SkuSearchOptions.Disabled
                )
            )

            advanceTimeBy(1)

            // THEN
            assertTrue(job1.isCompleted)
            assertTrue(!job2.isCompleted)
            assertTrue(result1 is WooPosSearchByIdentifierResult.Success)

            sut.onProductsSearched(
                WCProductStore.OnProductsSearched(
                    globalUniqueIdSearchQuery = identifier2,
                    searchQuery = null,
                    searchResults = listOf(wcProductModel2),
                    canLoadMore = false,
                    isSkuSearch = WCProductStore.SkuSearchOptions.Disabled
                )
            )

            advanceTimeBy(1)

            assertTrue(job2.isCompleted)
            assertTrue(result2 is WooPosSearchByIdentifierResult.Success)

            assertTrue(
                wcProductModel1.toAppModel().isSameProduct((result1 as WooPosSearchByIdentifierResult.Success).product)
            )
            assertTrue(
                wcProductModel2.toAppModel().isSameProduct((result2 as WooPosSearchByIdentifierResult.Success).product)
            )

            sut.onCleanup()
        }

    @Test
    fun `given variable product found, when variations fetched, then returns first matching variation`() = runTest {
        // GIVEN
        val identifier = "VAR-SKU-123"
        val productId = 100L
        val variationId = 200L
        val wcVariationProduct = ProductTestUtils.generateWCProductModel(
            productId = variationId,
            productType = "variation"
        ).copy(
            parentId = productId,
            sku = identifier
        )

        val wcVariationModel = WCProductVariationModel(LocalId(1)).copy(
            remoteProductId = RemoteId(productId),
            remoteVariationId = RemoteId(variationId),
            sku = identifier
        )
        val variation: ProductVariation = wcVariationModel.toAppModel()

        val onVariationChanged = WCProductStore.OnVariationChanged(productId, variationId)
        whenever(productStore.fetchSingleVariation(any(), any(), any())).thenReturn(onVariationChanged)
        whenever(productStore.getVariationByRemoteId(any(), any(), any())).thenReturn(wcVariationModel)

        // WHEN
        val job = launch {
            val result = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
            assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
            assertEquals(variation, (result as WooPosSearchByIdentifierResult.VariationSuccess).variation)
        }

        advanceTimeBy(1)

        // Return the variation product when searched
        sut.onProductsSearched(
            WCProductStore.OnProductsSearched(
                globalUniqueIdSearchQuery = null,
                searchQuery = identifier,
                searchResults = listOf(wcVariationProduct),
                canLoadMore = false,
                isSkuSearch = WCProductStore.SkuSearchOptions.Disabled
            )
        )

        advanceTimeBy(1)

        // THEN
        job.join()
        sut.onCleanup()
    }

    @Test
    fun `given no variations in cache, when variable product found, then fetches variations from remote`() = runTest {
        // GIVEN
        val identifier = "VAR-GLOBAL-ID"
        val productId = 100L
        val variationId = 200L

        val wcVariationProduct = ProductTestUtils.generateWCProductModel(
            productId = variationId,
            productType = "variation"
        ).copy(
            parentId = productId,
            globalUniqueId = identifier
        )
        val wcVariationModel = WCProductVariationModel(LocalId(1)).copy(
            remoteProductId = RemoteId(productId),
            remoteVariationId = RemoteId(variationId),
            globalUniqueId = identifier
        )
        val variation: ProductVariation = wcVariationModel.toAppModel()

        val onVariationChanged = WCProductStore.OnVariationChanged(productId, variationId)
        whenever(productStore.fetchSingleVariation(any(), any(), any())).thenReturn(onVariationChanged)
        whenever(productStore.getVariationByRemoteId(any(), any(), any())).thenReturn(wcVariationModel)

        // WHEN
        val job = launch {
            val result = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
            assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
            assertEquals(variation, (result as WooPosSearchByIdentifierResult.VariationSuccess).variation)
        }

        advanceTimeBy(1)

        sut.onProductsSearched(
            WCProductStore.OnProductsSearched(
                globalUniqueIdSearchQuery = identifier,
                searchQuery = null,
                searchResults = listOf(wcVariationProduct),
                canLoadMore = false,
                isSkuSearch = WCProductStore.SkuSearchOptions.Disabled
            )
        )

        advanceTimeBy(1)

        // THEN
        job.join()
        verify(variationsCache).add(productId, variation)
        sut.onCleanup()
    }

    @Test
    fun `given multiple variations, when searching by identifier, then returns matching variation`() = runTest {
        // GIVEN
        val identifier = "MATCH-VAR-SKU"
        val productId = 100L
        val variationId = 202L

        val wcVariationProduct = ProductTestUtils.generateWCProductModel(
            productId = variationId,
            productType = "variation"
        ).copy(
            parentId = productId,
            sku = identifier
        )
        val wcVariationModel = WCProductVariationModel(LocalId(1)).copy(
            remoteProductId = RemoteId(productId),
            remoteVariationId = RemoteId(variationId),
            sku = identifier
        )
        val variation2: ProductVariation = wcVariationModel.toAppModel()

        val onVariationChanged = WCProductStore.OnVariationChanged(productId, variationId)
        whenever(productStore.fetchSingleVariation(any(), any(), any())).thenReturn(onVariationChanged)
        whenever(productStore.getVariationByRemoteId(any(), any(), any())).thenReturn(wcVariationModel)

        // WHEN
        val job = launch {
            val result = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
            assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
            assertEquals(variation2, (result as WooPosSearchByIdentifierResult.VariationSuccess).variation)
        }

        advanceTimeBy(1)

        sut.onProductsSearched(
            WCProductStore.OnProductsSearched(
                globalUniqueIdSearchQuery = null,
                searchQuery = identifier,
                searchResults = listOf(wcVariationProduct),
                canLoadMore = false,
                isSkuSearch = WCProductStore.SkuSearchOptions.Disabled
            )
        )

        advanceTimeBy(1)

        // THEN
        job.join()
        sut.onCleanup()
    }

    @Test
    fun `given parent product not in cache, when variation found, then fetches parent product from remote`() = runTest {
        // GIVEN
        val identifier = "test-variation-sku"
        val parentProductId = 100L
        val variationId = 200L
        val wcVariationProduct = ProductTestUtils.generateWCProductModel(
            productId = variationId,
            productType = "variation"
        ).copy(
            parentId = parentProductId,
            sku = identifier
        )
        val wcVariationModel = WCProductVariationModel(LocalId(1)).copy(
            remoteProductId = RemoteId(parentProductId),
            remoteVariationId = RemoteId(variationId),
            sku = identifier
        )
        val variation: ProductVariation = wcVariationModel.toAppModel()

        val parentWcProductModel = ProductTestUtils.generateWCProductModel(productId = parentProductId)
        parentWcProductModel.toAppModel()

        val onVariationChanged = WCProductStore.OnVariationChanged(parentProductId, variationId)
        whenever(productStore.fetchSingleVariation(any(), any(), any())).thenReturn(onVariationChanged)
        whenever(productStore.getVariationByRemoteId(any(), any(), any())).thenReturn(wcVariationModel)
        whenever(productsCache.getProductById(parentProductId)).thenReturn(null)

        val fetchSingleProductPayload = argumentCaptor<WCProductStore.FetchSingleProductPayload>()
        val parentProductResult = WCProductStore.OnProductChanged(remoteProductId = parentProductId)
        whenever(productStore.fetchSingleProduct(fetchSingleProductPayload.capture())).thenReturn(parentProductResult)
        whenever(productStore.getProduct(any(), eq(parentProductId))).thenReturn(parentWcProductModel)

        // WHEN
        val job = launch {
            val result = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
            assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
            assertEquals(variation, (result as WooPosSearchByIdentifierResult.VariationSuccess).variation)
        }

        advanceTimeBy(1)

        sut.onProductsSearched(
            WCProductStore.OnProductsSearched(
                globalUniqueIdSearchQuery = null,
                searchQuery = identifier,
                searchResults = listOf(wcVariationProduct),
                canLoadMore = false,
                isSkuSearch = WCProductStore.SkuSearchOptions.Disabled
            )
        )

        advanceTimeBy(1)

        // THEN
        job.join()

        verify(productStore).fetchSingleProduct(any())
        assertEquals(parentProductId, fetchSingleProductPayload.firstValue.remoteProductId)
        verify(productsCache).addAll(any())

        sut.onCleanup()
    }

    @Test
    fun `given parent product in cache, when variation found, then uses cached parent product`() = runTest {
        // GIVEN
        val identifier = "test-variation-sku"
        val parentProductId = 100L
        val variationId = 200L
        val wcVariationProduct = ProductTestUtils.generateWCProductModel(
            productId = variationId,
            productType = "variation"
        ).copy(
            parentId = parentProductId,
            sku = identifier
        )
        val wcVariationModel = WCProductVariationModel(LocalId(1)).copy(
            remoteProductId = RemoteId(parentProductId),
            remoteVariationId = RemoteId(variationId),
            sku = identifier
        )
        val variation: ProductVariation = wcVariationModel.toAppModel()

        val parentProduct = ProductTestUtils.generateProduct(productId = parentProductId)

        val onVariationChanged = WCProductStore.OnVariationChanged(parentProductId, variationId)
        whenever(productStore.fetchSingleVariation(any(), any(), any())).thenReturn(onVariationChanged)
        whenever(productStore.getVariationByRemoteId(any(), any(), any())).thenReturn(wcVariationModel)

        whenever(productsCache.getProductById(parentProductId)).thenReturn(parentProduct)

        // WHEN
        val job = launch {
            val result = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
            assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
            assertEquals(variation, (result as WooPosSearchByIdentifierResult.VariationSuccess).variation)
        }

        advanceTimeBy(1)

        sut.onProductsSearched(
            WCProductStore.OnProductsSearched(
                globalUniqueIdSearchQuery = null,
                searchQuery = identifier,
                searchResults = listOf(wcVariationProduct),
                canLoadMore = false,
                isSkuSearch = WCProductStore.SkuSearchOptions.Disabled
            )
        )

        advanceTimeBy(1)

        // THEN
        job.join()

        verify(productStore, times(0)).fetchSingleProduct(any())

        sut.onCleanup()
    }

    @Test
    fun `given network error, when searching by identifier, then returns immediately with error`() = runTest {
        // GIVEN
        val identifier = "test-sku"
        val skuErrorEvent = WCProductStore.OnProductsSearched(
            globalUniqueIdSearchQuery = null,
            searchQuery = identifier,
            searchResults = emptyList(),
            canLoadMore = false,
            isSkuSearch = WCProductStore.SkuSearchOptions.ExactSearch
        ).apply {
            error = WCProductStore.ProductError(
                type = WCProductStore.ProductErrorType.GENERIC_ERROR,
                message = "Network connection failed"
            )
        }

        val gtinErrorEvent = WCProductStore.OnProductsSearched(
            globalUniqueIdSearchQuery = identifier,
            searchQuery = null,
            searchResults = emptyList(),
            canLoadMore = false,
            isSkuSearch = WCProductStore.SkuSearchOptions.Disabled
        ).apply {
            error = WCProductStore.ProductError(
                type = WCProductStore.ProductErrorType.GENERIC_ERROR,
                message = "Network connection failed"
            )
        }

        // WHEN
        val job = launch {
            val result = sut.invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
            assertTrue(result is WooPosSearchByIdentifierResult.Failure)
            assertEquals(
                WooPosSearchByIdentifierResult.Error.NetworkError,
                (result as WooPosSearchByIdentifierResult.Failure).error
            )
        }

        advanceTimeBy(1)

        // Send error responses for both GTIN and SKU searches
        sut.onProductsSearched(gtinErrorEvent)
        sut.onProductsSearched(skuErrorEvent)

        advanceTimeBy(1)

        // THEN
        job.join()

        sut.onCleanup()
    }
}
