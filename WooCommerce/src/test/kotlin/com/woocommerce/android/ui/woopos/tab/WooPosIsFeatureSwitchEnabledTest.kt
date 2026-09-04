package com.woocommerce.android.ui.woopos.tab

import com.google.gson.Gson
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.WCSSRModelCachingFetcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosIsFeatureSwitchEnabledTest {

    private val site = SiteModel().apply { id = 1 }
    private val selectedSite: SelectedSite = mock { on { getOrNull() } doReturn site }
    private val ssrFetcher: WCSSRModelCachingFetcher = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val sut = WooPosIsFeatureSwitchEnabled(
        selectedSite = selectedSite,
        ssrFetcher = ssrFetcher,
        gson = Gson(),
    )

    private fun ssrWithSettings(settingsJson: String?) =
        WooResult(WCSSRModel(remoteSiteId = 1, settings = settingsJson))

    @Test
    fun `given point_of_sale is in enabled_features, when invoked, then returns true`() = runTest {
        whenever(ssrFetcher.load(any(), any()))
            .thenReturn(ssrWithSettings("""{"enabled_features":["point_of_sale","other_feature"]}"""))

        assertThat(sut(forceRefresh = false).getOrNull()).isTrue
    }

    @Test
    fun `given point_of_sale is absent from enabled_features, when invoked, then returns false`() = runTest {
        whenever(ssrFetcher.load(any(), any()))
            .thenReturn(ssrWithSettings("""{"enabled_features":["other_feature"]}"""))

        assertThat(sut(forceRefresh = false).getOrNull()).isFalse
    }

    @Test
    fun `given enabled_features is missing, when invoked, then the value could not be determined`() = runTest {
        // A missing field must not read as "off" — that would block every store on a shape change.
        whenever(ssrFetcher.load(any(), any())).thenReturn(ssrWithSettings("""{"currency":"USD"}"""))

        assertThat(sut(forceRefresh = false).isFailure).isTrue
    }

    @Test
    fun `given the settings payload is not valid json, when invoked, then the value could not be determined`() =
        runTest {
            whenever(ssrFetcher.load(any(), any())).thenReturn(ssrWithSettings("not json"))

            assertThat(sut(forceRefresh = false).isFailure).isTrue
        }

    @Test
    fun `given the SSR request fails, when invoked, then the value could not be determined`() = runTest {
        whenever(ssrFetcher.load(any(), any())).thenReturn(
            WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR))
        )

        assertThat(sut(forceRefresh = false).isFailure).isTrue
    }

    @Test
    fun `given no site is selected, when invoked, then the value could not be determined`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(null)

        assertThat(sut(forceRefresh = false).isFailure).isTrue
    }

    @Test
    fun `given forceRefresh, when invoked, then the report is fetched remotely`() = runTest {
        whenever(ssrFetcher.load(any(), any()))
            .thenReturn(ssrWithSettings("""{"enabled_features":["point_of_sale"]}"""))

        sut(forceRefresh = true)

        verify(ssrFetcher).load(site, true)
    }
}
