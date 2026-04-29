package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class AIProductVariationsDataSourceTest {

    private val site: SiteModel = SiteModel().apply { id = 42 }
    private val selectedSite: SelectedSite = mock { on { get() }.thenReturn(site) }
    private val productStore: WCProductStore = mock()

    private val dataSource = AIProductVariationsDataSource(
        selectedSite = selectedSite,
        productStore = productStore,
    )

    private fun makeVariation(
        productId: Long = 100L,
        variationId: Long = 10L,
        price: String = "19.99",
    ) = WCProductVariationModel(
        localSiteId = LocalId(site.id),
        remoteProductId = RemoteId(productId),
        remoteVariationId = RemoteId(variationId),
        price = price,
    )

    @Test
    fun `given list mode, when fetchVariations is called, then fetchProductVariations uses requested page`() = runTest {
        val variation = makeVariation(productId = 100L, variationId = 11L)
        whenever(productStore.fetchProductVariations(any()))
            .thenReturn(variationsFetched(variation))

        val result = dataSource.fetchVariations(productId = 100L, page = 3, perPage = 10)

        assertThat(result.getOrThrow()).containsExactly(variation)
        verify(productStore).fetchProductVariations(
            argThatPayload(productId = 100L, pageSize = 10, offset = 20)
        )
        verify(productStore, never()).getVariationsForProduct(site, 100L)
    }

    @Test
    fun `given perPage above max, when fetchVariations is called, then page size is clamped to 50`() = runTest {
        whenever(productStore.fetchProductVariations(any()))
            .thenReturn(variationsFetched())

        dataSource.fetchVariations(productId = 100L, page = 1, perPage = 999)

        verify(productStore).fetchProductVariations(
            argThatPayload(productId = 100L, pageSize = 50, offset = 0)
        )
    }

    @Test
    fun `given fetchProductVariations error, when fetchVariations is called, then failure wraps OnChangedException`() =
        runTest {
            whenever(productStore.fetchProductVariations(any())).thenReturn(
                WooResult(WooError(WooErrorType.API_ERROR, GenericErrorType.SERVER_ERROR, "boom"))
            )

            val result = dataSource.fetchVariations(productId = 100L)

            assertThat(result.exceptionOrNull()).isInstanceOf(OnChangedException::class.java)
            verify(productStore, never()).getVariationsForProduct(site, 100L)
        }

    @Test
    fun `given variation is cached, when getVariation is called, then cached value is returned without fetch`() =
        runTest {
            val variation = makeVariation(productId = 100L, variationId = 10L)
            whenever(productStore.getVariationByRemoteId(site, 100L, 10L)).thenReturn(variation)

            val result = dataSource.getVariation(productId = 100L, variationId = 10L)

            assertThat(result.getOrThrow()).isEqualTo(variation)
            verify(productStore, never()).fetchSingleVariation(any(), any(), any())
        }

    @Test
    fun `given variation is not cached, when getVariation is called, then it fetches and returns cached variation`() =
        runTest {
            val variation = makeVariation(productId = 100L, variationId = 10L)
            whenever(productStore.getVariationByRemoteId(site, 100L, 10L)).thenReturn(null).thenReturn(variation)
            whenever(productStore.fetchSingleVariation(site, 100L, 10L))
                .thenReturn(WCProductStore.OnVariationChanged(remoteProductId = 100L, remoteVariationId = 10L))

            val result = dataSource.getVariation(productId = 100L, variationId = 10L)

            assertThat(result.getOrThrow()).isEqualTo(variation)
            verify(productStore).fetchSingleVariation(site, 100L, 10L)
        }

    @Test
    fun `given single variation fetch fails, when getVariation is called, then failure wraps OnChangedException`() =
        runTest {
            val errorEvent = WCProductStore.OnVariationChanged(remoteProductId = 100L, remoteVariationId = 10L).also {
                it.error = WCProductStore.ProductError(message = "missing")
            }
            whenever(productStore.getVariationByRemoteId(site, 100L, 10L)).thenReturn(null)
            whenever(productStore.fetchSingleVariation(site, 100L, 10L)).thenReturn(errorEvent)

            val result = dataSource.getVariation(productId = 100L, variationId = 10L)

            assertThat(result.exceptionOrNull()).isInstanceOf(OnChangedException::class.java)
        }

    private fun argThatPayload(
        productId: Long,
        pageSize: Int,
        offset: Int,
    ): WCProductStore.FetchProductVariationsPayload =
        org.mockito.kotlin.argThat {
            remoteProductId == productId && this.pageSize == pageSize && this.offset == offset && site == this.site
        }

    private fun variationsFetched(
        vararg variations: WCProductVariationModel,
        canLoadMore: Boolean = false,
    ) = WooResult(
        WCProductStore.ProductVariationsPage(
            variations = variations.toList(),
            canLoadMore = canLoadMore,
        )
    )
}
