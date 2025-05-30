package com.woocommerce.android.ui.woopos

import com.woocommerce.android.cache.SSRCache
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WooPOSIsRemotelyEnabledTest {

    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val siteModel: SiteModel = mock()
    private val ssrModel: WCSSRModel = mock()
    private val cacheResult: WooResult<WCSSRModel> = mock()

    private lateinit var sut: WooPOSIsRemotelyEnabled

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(siteModel)
        sut = WooPOSIsRemotelyEnabled(selectedSite, wooCommerceStore)
        SSRCache.clear()
    }

    @Test
    fun `given feature enabled remotely when invoked then returns true`() = runTest {
        // GIVEN
        val jsonSettings = """{"enabled_features": ["point_of_sale", "other_feature"]}"""
        whenever(ssrModel.settings).thenReturn(jsonSettings)
        whenever(cacheResult.isError).thenReturn(false)
        whenever(cacheResult.model).thenReturn(ssrModel)
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(cacheResult)

        // WHEN
        val result = sut.invoke()

        // THEN
        assertTrue(result)
    }

    @Test
    fun `given feature not in list when invoked then returns false`() = runTest {
        val jsonSettings = """{"enabled_features": ["something_else"]}"""
        whenever(ssrModel.settings).thenReturn(jsonSettings)
        whenever(cacheResult.isError).thenReturn(false)
        whenever(cacheResult.model).thenReturn(ssrModel)
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertFalse(result)
    }

    @Test
    fun `given empty feature list when invoked then returns false`() = runTest {
        val jsonSettings = """{"enabled_features": []}"""
        whenever(ssrModel.settings).thenReturn(jsonSettings)
        whenever(cacheResult.isError).thenReturn(false)
        whenever(cacheResult.model).thenReturn(ssrModel)
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertFalse(result)
    }

    @Test
    fun `given null result when invoked then returns false`() = runTest {
        whenever(cacheResult.isError).thenReturn(false)
        whenever(cacheResult.model).thenReturn(null)
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertFalse(result)
    }

    @Test
    fun `given error result when invoked then returns false`() = runTest {
        whenever(cacheResult.isError).thenReturn(true)
        whenever(wooCommerceStore.fetchSSR(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertFalse(result)
    }
}
