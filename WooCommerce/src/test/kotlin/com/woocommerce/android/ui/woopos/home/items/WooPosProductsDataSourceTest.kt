package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.selector.ProductListHandler
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCProductStore
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
class WooPosProductsDataSourceTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val sampleProducts = listOf(
        ProductTestUtils.generateProduct(
            productId = 1,
            productName = "Product 1",
            amount = "10.0",
            productType = "simple",
            isDownloadable = false,
        ),
        ProductTestUtils.generateProduct(
            productId = 2,
            productName = "Product 2",
            amount = "20.0",
            productType = "simple",
            isDownloadable = false,
        ).copy(firstImageUrl = "https://test.com"),
        ProductTestUtils.generateProduct(
            productId = 3,
            productName = "Product 3",
            amount = "20.0",
            productType = "simple",
            isDownloadable = false,
        ).copy(firstImageUrl = "https://test.com")
    )

    private val additionalProducts = listOf(
        ProductTestUtils.generateProduct(
            productId = 4,
            productName = "Product 4",
            amount = "10.0",
            productType = "simple",
            isDownloadable = false,
        ),
        ProductTestUtils.generateProduct(
            productId = 5,
            productName = "Product 5",
            amount = "20.0",
            productType = "simple",
            isDownloadable = false,
        ).copy(firstImageUrl = "https://test.com"),
    )

    private val handler: ProductListHandler = mock()
    private val productsCache: WooPosProductsCache = mock()

    @Test
    fun `given force refresh, when loadSimpleProducts called, then should clear cache`() = runTest {
        // GIVEN
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
        whenever(productsCache.getAll()).thenReturn(emptyList(), sampleProducts)
        val sut = WooPosProductsDataSource(handler, productsCache)

        // WHEN
        sut.loadSimpleProducts(forceRefreshProducts = true).first()

        // THEN
        verify(productsCache).clear()
    }

    @Test
    fun `given cached products, when loadSimpleProducts called, then should emit cached products first`() = runTest {
        // GIVEN
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
        whenever(
            handler.loadFromCacheAndFetch(any(), any(), any(), any(), any(), any())
        ).thenReturn(Result.success(Unit))
        whenever(productsCache.getAll()).thenReturn(sampleProducts)
        val sut = WooPosProductsDataSource(handler, productsCache)

        // WHEN
        val result = sut.loadSimpleProducts(forceRefreshProducts = false).first()

        // THEN
        assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
        val cachedResult = result as WooPosProductsDataSource.ProductsResult.Cached
        assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
    }

    @Test
    fun `given cached and remote products, when loadSimpleProducts called, then should emit remote products after cached products`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
            whenever(
                handler.loadFromCacheAndFetch(any(), any(), any(), any(), any(), any())
            ).thenReturn(
                Result.success(Unit)
            )
            whenever(productsCache.getAll()).thenReturn(sampleProducts)
            val sut = WooPosProductsDataSource(handler, productsCache)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
            assertThat(remoteResult.productsResult.isSuccess).isTrue()
            assertThat(remoteResult.productsResult.getOrNull()).containsExactlyElementsOf(sampleProducts)
            verify(productsCache).addAll(sampleProducts)
        }

    @Test
    fun `given error condition when loading products, then cached products are emitted first and error after`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
            val exception = Exception("Some error message")
            whenever(productsCache.getAll()).thenReturn(sampleProducts)
            whenever(
                handler.loadFromCacheAndFetch(any(), any(), any(), any(), any(), any())
            ).thenReturn(Result.failure(exception))

            val sut = WooPosProductsDataSource(handler, productsCache)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            assertThat(flow.size).isEqualTo(2)
            // First item should be Cached result
            assertThat(flow[0]).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            assertThat(cachedResult.products).isEqualTo(sampleProducts)
            // Second item should be Remote result with failure
            assertThat(flow[1]).isInstanceOf(WooPosProductsDataSource.ProductsResult.Remote::class.java)
        }

    @Test
    fun `given successful loadMore, when loadMore called, then should add products to cache and return them`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(additionalProducts))
            whenever(handler.loadMore()).thenReturn(Result.success(Unit))
            whenever(productsCache.getAll()).thenReturn(sampleProducts + additionalProducts)
            val sut = WooPosProductsDataSource(handler, productsCache)

            // WHEN
            val result = sut.loadMore()

            // THEN
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).containsExactlyElementsOf(sampleProducts + additionalProducts)
            verify(productsCache).addAll(additionalProducts)
        }

    @Test
    fun `given failed loadMore, when loadMore called, then should return error and cache remains unchanged`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
            val exception = Exception("Load more failed")
            whenever(
                handler.loadMore(
                    includeTypes = listOf(WCProductStore.IncludeType.Simple, WCProductStore.IncludeType.Variable),
                    orderCurrency = null
                )
            ).thenReturn(Result.failure(exception))
            val sut = WooPosProductsDataSource(handler, productsCache)

            // WHEN
            val result = sut.loadMore()

            // THEN
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isEqualTo(exception)
        }

    @Test
    fun `given no cached products and remote load fails, when loadSimpleProducts called, then should emit empty cache and then error`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(emptyList()))
            whenever(productsCache.getAll()).thenReturn(emptyList())
            val exception = Exception("Remote load failed")
            whenever(
                handler.loadFromCacheAndFetch(any(), any(), any(), any(), any(), eq(null))
            ).thenReturn(Result.failure(exception))

            val sut = WooPosProductsDataSource(handler, productsCache)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products).isEmpty()
            assertThat(remoteResult.productsResult.isFailure).isTrue()
            assertThat(remoteResult.productsResult.exceptionOrNull()).isEqualTo(exception)
        }

    @Test
    fun `given empty product list from handler, when loadSimpleProducts called, then should emit empty cache and empty remote result`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(emptyList()))
            whenever(productsCache.getAll()).thenReturn(emptyList())
            whenever(
                handler.loadFromCacheAndFetch(any(), any(), any(), any(), any(), any())
            ).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler, productsCache)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products).isEmpty()
            assertThat(remoteResult.productsResult.isSuccess).isTrue()
            assertThat(remoteResult.productsResult.getOrNull()).isEmpty()
            verify(productsCache).addAll(emptyList())
        }

    @Test
    fun `given cached products, when loadSimpleProducts called, then filter in only products that has price`() =
        runTest {
            // GIVEN
            val productsWithoutPrice = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "0",
                    productType = "simple",
                    isDownloadable = false,
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "simple",
                    isDownloadable = false
                ).copy(firstImageUrl = "https://test.com")
            )
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(productsWithoutPrice))
            whenever(productsCache.getAll()).thenReturn(productsWithoutPrice.filter { it.remoteId == 2L })
            whenever(
                handler.loadFromCacheAndFetch(any(), any(), any(), any(), any(), any())
            ).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler, productsCache)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached

            assertFalse(cachedResult.products.any { it.remoteId == 1L })
        }

    @Test
    fun `given cached products, when loadSimpleProducts called, then filter out downloadable products`() =
        runTest {
            // GIVEN
            val mixedProducts = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "0",
                    productType = "simple",
                    isDownloadable = true,
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "simple",
                    isVirtual = false,
                    isDownloadable = false
                ).copy(firstImageUrl = "https://test.com")
            )
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(mixedProducts))
            whenever(productsCache.getAll()).thenReturn(mixedProducts.filter { it.remoteId == 2L })
            whenever(
                handler.loadFromCacheAndFetch(any(), any(), any(), any(), any(), any())
            ).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler, productsCache)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached

            assertFalse(cachedResult.products.any { it.remoteId == 1L })
        }

    @Test
    fun `given remote products, when loadSimpleProducts called, then do not filter out variable products even if price is null `() =
        runTest {
            // GIVEN
            val mixedProducts = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "",
                    productType = "variable",
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "simple",
                    isVirtual = false,
                    isDownloadable = false
                ).copy(firstImageUrl = "https://test.com")
            )
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(mixedProducts))
            whenever(productsCache.getAll()).thenReturn(mixedProducts)
            whenever(
                handler.loadFromCacheAndFetch(any(), any(), any(), any(), any(), any())
            ).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler, productsCache)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(remoteResult.productsResult.getOrNull()).hasSize(2)
        }
}
