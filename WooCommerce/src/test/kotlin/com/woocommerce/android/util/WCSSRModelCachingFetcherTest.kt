package com.woocommerce.android.util

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

class WCSSRModelCachingFetcherTest {
    private val wooCommerceStore: WooCommerceStore = mock()
    private val siteModel: SiteModel = mock()
    private val ssrModel: WCSSRModel = mock()

    @Before
    fun setUp() {
        WCSSRModelCachingFetcher.clear()
    }

    @After
    fun tearDown() {
        WCSSRModelCachingFetcher.clear()
    }

    @Test
    fun `given cached value when load called again then store is not called`() = runTest {
        // GIVEN
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(WooResult(ssrModel))
        WCSSRModelCachingFetcher.load(siteModel, wooCommerceStore) // Populate cache

        // WHEN
        val result = WCSSRModelCachingFetcher.load(siteModel, wooCommerceStore)

        // THEN
        assertEquals(ssrModel, result.model)
        verify(wooCommerceStore, times(1)).fetchSSR(siteModel)
    }

    @Test
    fun `given empty cache when load called then fetches from remote`() = runTest {
        // GIVEN
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(WooResult(ssrModel))

        // WHEN
        val result = WCSSRModelCachingFetcher.load(siteModel, wooCommerceStore)

        // THEN
        assertEquals(ssrModel, result.model)
        verify(wooCommerceStore).fetchSSR(siteModel)
    }

    @Test
    fun `given remote failure when load called then returns error`() = runTest {
        // GIVEN
        val type = WooErrorType.API_ERROR
        val error = WooError(type, BaseRequest.GenericErrorType.NETWORK_ERROR)
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(WooResult(error))

        // WHEN
        val result = WCSSRModelCachingFetcher.load(siteModel, wooCommerceStore)

        // THEN
        assertNotNull(result.error)
        verify(wooCommerceStore).fetchSSR(siteModel)
    }
}
