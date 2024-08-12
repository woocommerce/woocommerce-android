package com.woocommerce.android.ui.woopos.home.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertFalse

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

    private val handler: ProductListHandler = mock()
    private val productStore: WCProductStore = mock()
    private val site: SelectedSite = mock {
        on { getOrNull() }.thenReturn(SiteModel())
    }

    @Test
    fun `given force refresh, when loadSimpleProducts called, then should wipe products table`() = runTest {
        // GIVEN
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(site.getOrNull()).thenReturn(SiteModel())
        whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
        val sut = WooPosProductsDataSource(handler, productStore, site)

        // WHEN
        sut.loadSimpleProducts(forceRefreshProducts = true).first()

        // THEN
        verify(productStore).deleteProductsForSite(anyOrNull())
    }

    @Test
    fun `given cached products, when loadSimpleProducts called, then should emit cached products first`() = runTest {
        // GIVEN
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(site.getOrNull()).thenReturn(SiteModel())
        whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
        whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.success(Unit))
        val sut = WooPosProductsDataSource(handler, productStore, site)

        // WHEN
        val result = sut.loadSimpleProducts(forceRefreshProducts = false).first()

        // THEN
        assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
        val cachedResult = result as WooPosProductsDataSource.ProductsResult.Cached
        assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
    }

    @Test
    fun `given remote load fails, when loadSimpleProducts called, then should emit error`() = runTest {
        // GIVEN
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
        val exception = Exception("Remote load failed")
        whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.failure(exception))
        val sut = WooPosProductsDataSource(handler, productStore, site)

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
    fun `given cached products, when loadSimpleProducts called, then should emit remote products after cached products`() = runTest {
        // GIVEN
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(site.getOrNull()).thenReturn(SiteModel())
        whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
        whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.success(Unit))
        val sut = WooPosProductsDataSource(handler, productStore, site)

        // WHEN
        val result = sut.loadSimpleProducts(forceRefreshProducts = false).first()

        // THEN
        assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
        val cachedResult = result as WooPosProductsDataSource.ProductsResult.Cached
        assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
    }

    @Test
    fun `given cached products, when loadSimpleProducts called, then filter in only published products`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(
                flowOf(
                    listOf(
                        ProductTestUtils.generateProduct(
                            productId = 1,
                            productName = "Product 1",
                            amount = "10.0",
                            productType = "simple",
                            customStatus = "private",
                            isDownloadable = false,
                        ),
                        ProductTestUtils.generateProduct(
                            productId = 2,
                            productName = "Product 2",
                            amount = "20.0",
                            productType = "simple",
                            isDownloadable = false,
                        ).copy(firstImageUrl = "https://test.com")
                    )
                )
            )
            whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler, productStore, site)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached

            assertFalse(cachedResult.products.any { it.remoteId == 1L })
        }

    @Test
    fun `given remote products, when loadSimpleProducts called, then filter in only published products`() = runTest {
        // GIVEN
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(site.getOrNull()).thenReturn(SiteModel())
        whenever(handler.productsFlow).thenReturn(
            flowOf(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 1,
                        productName = "Product 1",
                        amount = "10.0",
                        productType = "simple",
                        customStatus = "private",
                        isDownloadable = false,
                    ),
                    ProductTestUtils.generateProduct(
                        productId = 2,
                        productName = "Product 2",
                        amount = "20.0",
                        productType = "simple",
                        isDownloadable = false,
                    ).copy(firstImageUrl = "https://test.com")
                )
            )
        )
        val sut = WooPosProductsDataSource(handler, productStore, site)

        // WHEN
        val flow = sut.loadSimpleProducts(forceRefreshProducts = true).toList()

        // THEN
        val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote
        assertThat(remoteResult.productsResult.getOrNull()?.any { it.remoteId == 1L }).isFalse()
    }

    @Test
    fun `given cached products, when loadSimpleProducts called, then filter in only products that has price`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(
                flowOf(
                    listOf(
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
                )
            )
            whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler, productStore, site)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached

            assertFalse(cachedResult.products.any { it.remoteId == 1L })
        }

    @Test
    fun `given remote products, when loadSimpleProducts called, then filter in only products that has price`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(
                flowOf(
                    listOf(
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
                )
            )
            whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler, productStore, site)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = true).toList()

            // THEN
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote
            assertThat(remoteResult.productsResult.getOrNull()?.any { it.remoteId == 1L }).isFalse()
        }

    @Test
    fun `given cached products, when loadSimpleProducts called, then filter out virtual products`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(
                flowOf(
                    listOf(
                        ProductTestUtils.generateProduct(
                            productId = 1,
                            productName = "Product 1",
                            amount = "0",
                            productType = "simple",
                            isVirtual = true,
                            isDownloadable = false,
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
                )
            )
            whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler, productStore, site)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached

            assertFalse(cachedResult.products.any { it.remoteId == 1L })
        }

    @Test
    fun `given remote products, when loadSimpleProducts called, then filter out virtual products`() =
        runTest {
            // GIVEN
            whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
            whenever(handler.productsFlow).thenReturn(
                flowOf(
                    listOf(
                        ProductTestUtils.generateProduct(
                            productId = 1,
                            productName = "Product 1",
                            amount = "0",
                            productType = "simple",
                            isVirtual = true,
                            isDownloadable = false,
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
                )
            )
            whenever(handler.loadFromCacheAndFetch(any(), any(), any())).thenReturn(Result.success(Unit))
            val sut = WooPosProductsDataSource(handler, productStore, site)

            // WHEN
            val flow = sut.loadSimpleProducts(forceRefreshProducts = true).toList()

            // THEN
            // THEN
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote
            assertThat(remoteResult.productsResult.getOrNull()?.any { it.remoteId == 1L }).isFalse()
        }
}
