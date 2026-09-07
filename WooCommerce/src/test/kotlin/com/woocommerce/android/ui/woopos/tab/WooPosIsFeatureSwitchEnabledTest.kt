package com.woocommerce.android.ui.woopos.tab

import com.google.gson.Gson
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.WCSSRModelCachingFetcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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

    private val site = SiteModel().apply {
        id = 1
        siteId = 12345L
        selfHostedSiteId = 678L
    }
    private val selectedSite: SelectedSite = mock { on { getOrNull() } doReturn site }
    private val ssrFetcher: WCSSRModelCachingFetcher = mock()
    private val appPrefs: AppPrefs = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Before
    fun setup() {
        // Mockito answers a boxed Boolean with false, which would read as "the switch is stored as off".
        whenever(appPrefs.getPOSFeatureSwitchEnabledForSite(any(), any(), any())).thenReturn(null)
    }

    private val sut by lazy {
        WooPosIsFeatureSwitchEnabled(
            selectedSite = selectedSite,
            ssrFetcher = ssrFetcher,
            gson = Gson(),
            appPrefs = appPrefs,
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
        )
    }

    private fun ssrWithSettings(settingsJson: String?) =
        WooResult(WCSSRModel(remoteSiteId = 1, settings = settingsJson))

    private fun stubStoredValue(stored: Boolean?) {
        whenever(
            appPrefs.getPOSFeatureSwitchEnabledForSite(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(stored)
    }

    private fun verifyStored(enabled: Boolean) {
        verify(appPrefs).setPOSFeatureSwitchEnabledForSite(
            localSiteId = eq(site.id),
            remoteSiteId = eq(site.siteId),
            selfHostedSiteId = eq(site.selfHostedSiteId),
            enabled = eq(enabled)
        )
    }

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

    // --- Stored value ---

    @Test
    fun `given nothing is stored, when invoked, then the report is read and the value is stored`() = runTest {
        stubStoredValue(null)
        whenever(ssrFetcher.load(any(), any()))
            .thenReturn(ssrWithSettings("""{"enabled_features":["point_of_sale"]}"""))

        assertThat(sut(forceRefresh = false).getOrNull()).isTrue
        verifyStored(true)
    }

    @Test
    fun `given a stored value, when invoked, then it is returned without waiting on the report`() = runTest {
        stubStoredValue(false)
        whenever(ssrFetcher.load(any(), any()))
            .thenReturn(ssrWithSettings("""{"enabled_features":["point_of_sale"]}"""))

        // The stored "off" wins over the report, which says "on" — the report only updates the store.
        assertThat(sut(forceRefresh = false).getOrNull()).isFalse
    }

    @Test
    fun `given a stored value, when invoked, then the report is refreshed in the background`() = runTest {
        stubStoredValue(true)
        whenever(ssrFetcher.load(any(), any()))
            .thenReturn(ssrWithSettings("""{"enabled_features":["other_feature"]}"""))

        sut(forceRefresh = false)

        verifyStored(false)
    }

    @Test
    fun `given forceRefresh, when invoked, then the stored value is ignored`() = runTest {
        stubStoredValue(true)
        whenever(ssrFetcher.load(any(), any()))
            .thenReturn(ssrWithSettings("""{"enabled_features":["other_feature"]}"""))

        assertThat(sut(forceRefresh = true).getOrNull()).isFalse
    }

    @Test
    fun `given the report cannot be read, when invoked, then the stored value is left alone`() = runTest {
        stubStoredValue(null)
        whenever(ssrFetcher.load(any(), any())).thenReturn(
            WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR))
        )

        assertThat(sut(forceRefresh = false).isFailure).isTrue
        verify(appPrefs, never()).setPOSFeatureSwitchEnabledForSite(any(), any(), any(), any())
    }

    // --- Report handed over by the plugin check ---

    @Test
    fun `given settings from the same report, when invoked, then it is used without a fetch`() = runTest {
        val result = sut(forceRefresh = true, systemStatusSettings = """{"enabled_features":["point_of_sale"]}""")

        assertThat(result.getOrNull()).isTrue
        verify(ssrFetcher, never()).load(any(), any())
        verifyStored(true)
    }

    @Test
    fun `given settings from the same report without the switch, when invoked, then the switch is off`() = runTest {
        val result = sut(forceRefresh = true, systemStatusSettings = """{"enabled_features":["other_feature"]}""")

        assertThat(result.getOrNull()).isFalse
        verify(ssrFetcher, never()).load(any(), any())
    }

    @Test
    fun `given settings from the same report that omit the field, when invoked, then it fails`() = runTest {
        val result = sut(forceRefresh = true, systemStatusSettings = """{"other":1}""")

        assertThat(result.isFailure).isTrue
        verify(appPrefs, never()).setPOSFeatureSwitchEnabledForSite(any(), any(), any(), any())
    }
}
