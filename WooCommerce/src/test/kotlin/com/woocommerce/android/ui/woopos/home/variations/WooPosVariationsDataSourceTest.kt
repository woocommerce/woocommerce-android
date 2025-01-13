package com.woocommerce.android.ui.woopos.home.variations

import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.variations.selector.VariationListHandler
import com.woocommerce.android.ui.woopos.home.items.variations.FetchResult
import com.woocommerce.android.ui.woopos.home.items.variations.VariationsLRUCache
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsDataSource
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosVariationsDataSourceTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val sampleProducts = listOf(
        ProductTestUtils.generateProductVariation(
            productId = 1,
            variationId = 2,
            amount = "10.0",
            isDownloadable = false,
        ),
        ProductTestUtils.generateProductVariation(
            productId = 2,
            variationId = 3,
            amount = "20.0",
            isDownloadable = false,
        ),
        ProductTestUtils.generateProductVariation(
            productId = 3,
            variationId = 4,
            amount = "20.0",
            isDownloadable = false,
        )
    )

    private val additionalProducts = listOf(
        ProductTestUtils.generateProductVariation(
            productId = 4,
            variationId = 5,
            amount = "10.0",
            isDownloadable = false,
        ),
        ProductTestUtils.generateProductVariation(
            productId = 5,
            variationId = 6,
            amount = "20.0",
            isDownloadable = false,
        ),
    )

    private val handler: VariationListHandler = mock()
    private val variationsCache: VariationsLRUCache<Long, List<ProductVariation>> = mock()

    @Test
    fun `given force refresh, when fetchFirstPage called, then should clear cache`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(flowOf(sampleProducts))
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts)
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        sut.fetchFirstPage(productId, forceRefresh = true).first()
        assertThat(
            sut.fetchFirstPage(productId, forceRefresh = true).first()
        ).isInstanceOf(FetchResult.Cached::class.java)

        // WHEN
        sut.fetchFirstPage(productId, forceRefresh = true).first()

        // THEN
        // Ensure the cache is cleared (by checking that the cache was reloaded)
        val result = sut.fetchFirstPage(productId, forceRefresh = false).first()
        assertThat(result).isInstanceOf(FetchResult.Cached::class.java)
        val cachedResult = result as FetchResult.Cached
        assertThat(cachedResult.data).containsExactlyElementsOf(sampleProducts)
    }

    @Test
    fun `given cached products, when fetchFirstPage called, then should emit cached products first`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(flowOf(sampleProducts))
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts)
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        sut.fetchFirstPage(productId, forceRefresh = true).first()

        // WHEN
        val result = sut.fetchFirstPage(productId, forceRefresh = false).first()

        // THEN
        // Ensure the result is from cache
        assertThat(result).isInstanceOf(FetchResult.Cached::class.java)
        val cachedResult = result as FetchResult.Cached
        assertThat(cachedResult.data).containsExactlyElementsOf(sampleProducts)
    }

    @Test
    fun `given cached and remote variations, when fetchFirstPage called, then should emit remote variations after cached variations`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(flowOf(sampleProducts))
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts)
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        sut.fetchFirstPage(productId, forceRefresh = true).first()

        // WHEN
        val flow = sut.fetchFirstPage(productId, forceRefresh = false).toList()

        // THEN
        val cachedResult = flow[0] as FetchResult.Cached
        assertThat(cachedResult.data).containsExactlyElementsOf(sampleProducts)

        // Validate remote result
        val remoteResult = flow[1] as FetchResult.Remote
        assertThat(remoteResult.result.getOrNull()).isNotNull
        assertThat(remoteResult.result.getOrNull()).containsExactlyElementsOf(sampleProducts)
    }

    @Test
    fun `given remote load fails, when fetchFirstPage called, then should emit cached variations and then error`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(flowOf(sampleProducts))
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts)
        val exception = Exception("Remote load failed")
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        sut.fetchFirstPage(productId, forceRefresh = true).first()

        whenever(
            handler.fetchVariations(
                productId,
                forceRefresh = true,
                mapOf(WCProductStore.VariationFilterOption.STATUS to "publish")
            )
        )
            .thenReturn(Result.failure(exception))

        // WHEN
        val flow = sut.fetchFirstPage(productId, forceRefresh = false).toList()

        // THEN
        val cachedResult = flow[0] as FetchResult.Cached
        assertThat(cachedResult.data).containsExactlyElementsOf(sampleProducts)

        val remoteResult = flow[1] as FetchResult.Remote
        assertThat(remoteResult.result.getOrNull()).isNull()
        assertThat(remoteResult.result.exceptionOrNull()).isEqualTo(exception)
    }

    @Test
    fun `given successful loadMore, when loadMore called, then should add variations to cache and return them`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(
            flowOf(sampleProducts),
            flowOf(sampleProducts + additionalProducts)
        )
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts + additionalProducts)
        whenever(handler.loadMore(productId)).thenReturn(Result.success(Unit))
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        sut.fetchFirstPage(productId, forceRefresh = false).first()

        // WHEN
        val result = sut.loadMore(productId)

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).containsExactlyElementsOf(sampleProducts + additionalProducts)

        val cachedResult = sut.fetchFirstPage(productId, forceRefresh = false).first()
        assertThat(cachedResult).isInstanceOf(FetchResult.Cached::class.java)
        val cachedVariations = (cachedResult as FetchResult.Cached).data
        assertThat(cachedVariations).containsExactlyElementsOf(sampleProducts + additionalProducts)
    }

    @Test
    fun `given failed loadMore, when loadMore called, then should return error and cache remains unchanged`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(flowOf(sampleProducts))
        val exception = Exception("Load more failed")
        whenever(
            handler.loadMore(productId, mapOf(WCProductStore.VariationFilterOption.STATUS to "publish")),
        ).thenReturn(Result.failure(exception))
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts)
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        sut.fetchFirstPage(productId, forceRefresh = false).first()

        // WHEN
        val result = sut.loadMore(productId)

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)

        val cachedResult = sut.fetchFirstPage(productId, forceRefresh = false).first()
        assertThat(cachedResult).isInstanceOf(FetchResult.Cached::class.java)
        val cachedVariations = (cachedResult as FetchResult.Cached).data
        assertThat(cachedVariations).containsExactlyElementsOf(sampleProducts)
    }

    @Test
    fun `given no cached variations and remote load fails, when fetchFirstPage called, then should emit empty cache and then error`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(flowOf(emptyList()))
        val exception = Exception("Remote load failed")
        whenever(
            handler.fetchVariations(
                productId,
                forceRefresh = true,
                mapOf(WCProductStore.VariationFilterOption.STATUS to "publish")
            )
        ).thenReturn(Result.failure(exception))
        whenever(variationsCache.get(productId)).thenReturn(emptyList())

        val sut = WooPosVariationsDataSource(handler, variationsCache)

        // WHEN
        val flow = sut.fetchFirstPage(productId, forceRefresh = false).toList()

        // THEN
        val remoteResult = flow[0] as FetchResult.Remote
        assertThat(remoteResult.result.getOrNull()).isNull()
        assertThat(remoteResult.result.exceptionOrNull()).isEqualTo(exception)
    }

    @Test
    fun `given empty variations list from handler, when fetchFirstPage called, then should emit empty remote result`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(flowOf(emptyList()))
        whenever(handler.fetchVariations(productId, forceRefresh = false)).thenReturn(Result.success(Unit))
        whenever(variationsCache.get(productId)).thenReturn(emptyList())
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        // WHEN
        val flow = sut.fetchFirstPage(productId, forceRefresh = false).toList()

        // THEN
        val remoteResult = flow[0] as FetchResult.Remote
        assertThat(remoteResult.result.getOrNull()).isNotNull
        assertThat(remoteResult.result.getOrNull()).isEmpty()
    }

    @Test
    fun `given cached variations, when fetchFirstPage called, then filter in only variations that have price`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(
            flowOf(
                listOf(
                    ProductTestUtils.generateProductVariation(
                        variationId = 1,
                        amount = "0",
                    ),
                    ProductTestUtils.generateProductVariation(
                        variationId = 2,
                        amount = "20.0",
                    )
                )
            )
        )
        whenever(handler.fetchVariations(productId, forceRefresh = true)).thenReturn(Result.success(Unit))
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts)
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        // WHEN
        val flow = sut.fetchFirstPage(productId, forceRefresh = false).toList()

        // THEN
        val cachedResult = flow[0] as FetchResult.Cached

        assertFalse(cachedResult.data.any { it.remoteVariationId == 1L })
    }

    @Test
    fun `given remote variations, when fetchFirstPage called, then filter in only variations that have price`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(
            flowOf(
                listOf(
                    ProductTestUtils.generateProductVariation(
                        variationId = 1,
                        amount = "",
                    ),
                    ProductTestUtils.generateProductVariation(
                        variationId = 2,
                        amount = "20.0",
                    )
                )
            )
        )
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts)
        whenever(handler.fetchVariations(productId, forceRefresh = true)).thenReturn(Result.success(Unit))
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        // WHEN
        val flow = sut.fetchFirstPage(productId, forceRefresh = true).toList()

        // THEN
        val remoteResult = flow[1] as FetchResult.Remote

        assertThat(remoteResult.result.getOrNull()?.any { it.remoteVariationId == 1L }).isFalse()
    }

    @Test
    fun `given cached variations, when fetchFirstPage called, then filter out virtual variations`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(
            flowOf(
                listOf(
                    ProductTestUtils.generateProductVariation(
                        variationId = 1,
                        amount = "0",
                        isVirtual = true
                    ),
                    ProductTestUtils.generateProductVariation(
                        variationId = 2,
                        amount = "20.0",
                        isVirtual = false
                    )
                )
            )
        )
        whenever(handler.fetchVariations(productId, forceRefresh = true)).thenReturn(Result.success(Unit))
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts)
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        // WHEN
        val flow = sut.fetchFirstPage(productId, forceRefresh = false).toList()

        // THEN
        val cachedResult = flow[0] as FetchResult.Cached

        assertFalse(cachedResult.data.any { it.remoteVariationId == 1L })
    }

    @Test
    fun `given cached variations, when fetchFirstPage called, then filter out downloadable variations`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(false)
        whenever(handler.getVariationsFlow(productId)).thenReturn(
            flowOf(
                listOf(
                    ProductTestUtils.generateProductVariation(
                        variationId = 1,
                        amount = "0",
                        isDownloadable = true
                    ),
                    ProductTestUtils.generateProductVariation(
                        variationId = 2,
                        amount = "20.0",
                        isVirtual = false,
                        isDownloadable = false
                    )
                )
            )
        )
        whenever(handler.fetchVariations(productId, forceRefresh = false)).thenReturn(Result.success(Unit))
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts)
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        // WHEN
        val flow = sut.fetchFirstPage(productId, forceRefresh = false).toList()

        // THEN
        val cachedResult = flow[0] as FetchResult.Cached

        assertFalse(cachedResult.data.any { it.remoteVariationId == 1L })
    }

    @Test
    fun `given remote variations, when fetchFirstPage called, then filter out downloadable variations`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(handler.canLoadMore(5)).thenReturn(true)
        whenever(handler.getVariationsFlow(productId)).thenReturn(
            flowOf(
                listOf(
                    ProductTestUtils.generateProductVariation(
                        variationId = 1,
                        amount = "0",
                        isDownloadable = true
                    ),
                    ProductTestUtils.generateProductVariation(
                        variationId = 2,
                        amount = "20.0",
                        isVirtual = false,
                        isDownloadable = false
                    )
                )
            )
        )
        whenever(handler.fetchVariations(productId, forceRefresh = true)).thenReturn(Result.success(Unit))
        whenever(variationsCache.get(productId)).thenReturn(sampleProducts)
        val sut = WooPosVariationsDataSource(handler, variationsCache)

        // WHEN
        val flow = sut.fetchFirstPage(productId, forceRefresh = true).toList()

        // THEN
        val remoteResult = flow[1] as FetchResult.Remote

        assertThat(remoteResult.result.getOrNull()?.any { it.remoteVariationId == 1L }).isFalse()
    }
}
