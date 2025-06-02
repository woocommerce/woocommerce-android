package com.woocommerce.android.ui.woopos

import com.google.gson.Gson
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WCSSRModelCachingFetcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WooPOSIsRemotelyEnabledTest {
    private val selectedSite: SelectedSite = mock()
    private val siteModel: SiteModel = mock()
    private val ssrModel: WCSSRModel = mock()
    private val fetcher: WCSSRModelCachingFetcher = mock()
    private val cacheResult: WooResult<WCSSRModel> = mock()

    private lateinit var sut: WooPOSIsRemotelyEnabled

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(siteModel)
        sut = WooPOSIsRemotelyEnabled(selectedSite, fetcher, Gson())
    }

    @Test
    fun `given feature enabled remotely when invoked then returns true`() = runTest {
        // GIVEN
        val jsonSettings = """{"enabled_features": ["point_of_sale", "other_feature"]}"""
        whenever(ssrModel.settings).thenReturn(jsonSettings)
        whenever(cacheResult.isError).thenReturn(false)
        whenever(cacheResult.model).thenReturn(ssrModel)
        whenever(fetcher.load(siteModel)).thenReturn(cacheResult)

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
        whenever(fetcher.load(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertFalse(result)
    }

    @Test
    fun `given empty feature list when invoked then returns false`() = runTest {
        val jsonSettings = """{"enabled_features": []}"""
        whenever(ssrModel.settings).thenReturn(jsonSettings)
        whenever(cacheResult.isError).thenReturn(false)
        whenever(cacheResult.model).thenReturn(ssrModel)
        whenever(fetcher.load(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertFalse(result)
    }

    @Test
    fun `given null result when invoked then returns false`() = runTest {
        whenever(cacheResult.isError).thenReturn(false)
        whenever(cacheResult.model).thenReturn(null)
        whenever(fetcher.load(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertFalse(result)
    }

    @Test
    fun `given error result when invoked then returns false`() = runTest {
        whenever(cacheResult.isError).thenReturn(true)
        whenever(fetcher.load(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertFalse(result)
    }
}
