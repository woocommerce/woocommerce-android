package com.woocommerce.android.ui.woopos

import com.google.gson.Gson
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
import com.woocommerce.android.util.WCSSRModelCachingFetcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WooPOSIsRemotelyEnabledTest {
    private val selectedSite: SelectedSite = mock()
    private val siteModel: SiteModel = mock()
    private val fetcher: WCSSRModelCachingFetcher = mock()

    private lateinit var sut: WooPOSIsRemotelyEnabled

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(siteModel)
        sut = WooPOSIsRemotelyEnabled(selectedSite, fetcher, Gson())
    }

    @Test
    fun `given feature enabled remotely, when invoked, then returns success true`() = runTest {
        val jsonSettings = """{"enabled_features": ["point_of_sale", "other_feature"]}"""
        val ssrModel = WCSSRModel(remoteSiteId = 123L, settings = jsonSettings)
        val cacheResult = WooResult(ssrModel)
        whenever(fetcher.load(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `given feature not in list, when invoked, then returns success false`() = runTest {
        val jsonSettings = """{"enabled_features": ["something_else"]}"""
        val ssrModel = WCSSRModel(remoteSiteId = 123L, settings = jsonSettings)
        val cacheResult = WooResult(ssrModel)
        whenever(fetcher.load(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    @Test
    fun `given empty feature list, when invoked, then returns success false`() = runTest {
        val jsonSettings = """{"enabled_features": []}"""
        val ssrModel = WCSSRModel(remoteSiteId = 123L, settings = jsonSettings)
        val cacheResult = WooResult(ssrModel)
        whenever(fetcher.load(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    @Test
    fun `given null result, when invoked, then returns failure unknown`() = runTest {
        val cacheResult = WooResult<WCSSRModel>(model = null)
        whenever(fetcher.load(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WooPosCouldNotDetermineValueException)
    }

    @Test
    fun `given error result, when invoked, then returns failure unknown`() = runTest {
        val cacheResult = WooResult<WCSSRModel>(model = null).apply {
            error = WooError(
                type = WooErrorType.GENERIC_ERROR,
                original = BaseRequest.GenericErrorType.UNKNOWN
            )
        }
        whenever(fetcher.load(siteModel)).thenReturn(cacheResult)

        val result = sut.invoke()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WooPosCouldNotDetermineValueException)
    }
}
