package com.woocommerce.android.ui.woopos.home.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.selector.ProductListHandler
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosProductsDataSourceTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val sampleProducts = listOf(
        mock<Product> { on { price }.thenReturn(BigDecimal(1)) },
        mock<Product> { on { price }.thenReturn(BigDecimal(2)) },
        mock<Product> { on { price }.thenReturn(BigDecimal(3)) },
    )

    private val additionalProducts = listOf(
        mock<Product> { on { price }.thenReturn(BigDecimal(4)) },
        mock<Product> { on { price }.thenReturn(BigDecimal(5)) }
    )

    private val handler: ProductListHandler = mock()

    @Test
    fun `given force refresh, when loadSimpleProducts called, then should clear cache`() = runTest {
        // GIVEN
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
        val sut = WooPosProductsDataSource(handler)

        // Pre-populate the cache
        sut.loadSimpleProducts(forceRefreshProducts = false).first()
        assertThat(
            sut.loadSimpleProducts(forceRefreshProducts = false).first()
        ).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)

        // WHEN
        sut.loadSimpleProducts(forceRefreshProducts = true).first()

        // THEN
        // Ensure the cache is cleared (by checking that the cache was reloaded)
        val result = sut.loadSimpleProducts(forceRefreshProducts = false).first()
        assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
        val cachedResult = result as WooPosProductsDataSource.ProductsResult.Cached
        assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
    }

    @Test
    fun `given cached products, when loadSimpleProducts called, then should emit cached products first`() = runTest {
        // GIVEN
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
        whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.success(Unit))
        val sut = WooPosProductsDataSource(handler)

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
            whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
            assertThat(remoteResult.productsResult.isSuccess).isTrue()
            assertThat(remoteResult.productsResult.getOrNull()).containsExactlyElementsOf(sampleProducts)
        }

    @Test
    fun `given remote load fails, when loadSimpleProducts called, then should emit cached products and then error`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
            val exception = Exception("Remote load failed")
            whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.success(Unit))

            val sut = WooPosProductsDataSource(handler)

            // Prepopulate the cache by calling loadSimpleProducts once
            sut.loadSimpleProducts(forceRefreshProducts = false).first()

            whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.failure(exception))

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
            assertThat(remoteResult.productsResult.isFailure).isTrue()
            assertThat(remoteResult.productsResult.exceptionOrNull()).isEqualTo(exception)
        }

    @Test
    fun `given successful loadMore, when loadMore called, then should add products to cache and return them`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(
                flowOf(sampleProducts),
                flowOf(sampleProducts + additionalProducts)
            )
            whenever(handler.loadMore()).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler)

            sut.loadSimpleProducts(forceRefreshProducts = false).first()

            // WHEN
            val result = sut.loadMore()

            // THEN
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).containsExactlyElementsOf(sampleProducts + additionalProducts)

            val cachedResult = sut.loadSimpleProducts(forceRefreshProducts = false).first()
            assertThat(cachedResult).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
            val cachedProducts = (cachedResult as WooPosProductsDataSource.ProductsResult.Cached).products
            assertThat(cachedProducts).containsExactlyElementsOf(sampleProducts + additionalProducts)
        }

    @Test
    fun `given failed loadMore, when loadMore called, then should return error and cache remains unchanged`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
            val exception = Exception("Load more failed")
            whenever(handler.loadMore()).thenReturn(Result.failure(exception))
            val sut = WooPosProductsDataSource(handler)

            sut.loadSimpleProducts(forceRefreshProducts = false).first()

            // WHEN
            val result = sut.loadMore()

            // THEN
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isEqualTo(exception)

            val cachedResult = sut.loadSimpleProducts(forceRefreshProducts = false).first()
            assertThat(cachedResult).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
            val cachedProducts = (cachedResult as WooPosProductsDataSource.ProductsResult.Cached).products
            assertThat(cachedProducts).containsExactlyElementsOf(sampleProducts)
        }
}
