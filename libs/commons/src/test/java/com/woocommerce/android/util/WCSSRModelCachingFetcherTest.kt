package com.woocommerce.android.util

import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WCSSRModelCachingFetcherTest : BaseUnitTest() {
    private val wooCommerceStore: WooCommerceStore = mock()
    private val siteModel: SiteModel = mock()
    private val ssrModel = WCSSRModel(remoteSiteId = 123L)

    private val sut = WCSSRModelCachingFetcher(wooCommerceStore)

    @Test
    fun `given cached value, when load called again, then store is not called`() = testBlocking {
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(WooResult(ssrModel))
        sut.load(siteModel)

        val result = sut.load(siteModel)

        assertThat(result.model).isEqualTo(ssrModel)
        verify(wooCommerceStore, times(1)).fetchSSR(siteModel)
    }

    @Test
    fun `given empty cache, when load called, then fetches from remote`() = testBlocking {
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(WooResult(ssrModel))

        val result = sut.load(siteModel)

        assertThat(result.model).isEqualTo(ssrModel)
        verify(wooCommerceStore).fetchSSR(siteModel)
    }

    @Test
    fun `given remote failure, when load called, then returns error`() = testBlocking {
        val error = WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR)
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(WooResult(error))

        val result = sut.load(siteModel)

        assertThat(result.error).isNotNull()
        verify(wooCommerceStore).fetchSSR(siteModel)
    }
}
