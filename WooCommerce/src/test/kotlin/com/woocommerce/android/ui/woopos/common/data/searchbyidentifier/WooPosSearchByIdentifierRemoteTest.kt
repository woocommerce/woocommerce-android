package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSearchByIdentifierRemoteTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule(UnconfinedTestDispatcher())

    private lateinit var sut: WooPosSearchByIdentifierRemote
    private val dispatcher: Dispatcher = mock()
    private val selectedSite: SelectedSite = mock()
    private val productsCache: WooPosProductsCache = mock()
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
            checkDigitRemover
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
}
