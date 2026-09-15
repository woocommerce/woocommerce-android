package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
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
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.EnabledFeatures

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosIsFeatureSwitchEnabledTest {

    private val site = SiteModel().apply {
        id = 1
        siteId = 12345L
        selfHostedSiteId = 678L
    }
    private val selectedSite: SelectedSite = mock { on { getOrNull() } doReturn site }
    private val wooCommerceStore: WooCommerceStore = mock()
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
            wooCommerceStore = wooCommerceStore,
            appPrefs = appPrefs,
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
        )
    }

    private suspend fun stubReport(vararg enabledFeatures: String) {
        stubReport(EnabledFeatures.Known(enabledFeatures.toList()))
    }

    private suspend fun stubReport(enabledFeatures: EnabledFeatures) {
        whenever(wooCommerceStore.fetchSitePluginsAndSettings(any())).thenReturn(
            WooResult(
                WooCommerceStore.SitePluginsAndFeatures(
                    plugins = emptyList(),
                    enabledFeatures = enabledFeatures
                )
            )
        )
    }

    private suspend fun stubReportFailure() {
        whenever(wooCommerceStore.fetchSitePluginsAndSettings(any())).thenReturn(
            WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR))
        )
    }

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
        stubReport("point_of_sale", "other_feature")

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh).getOrNull()).isTrue
    }

    @Test
    fun `given point_of_sale is absent from enabled_features, when invoked, then returns false`() = runTest {
        stubReport("other_feature")

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh).getOrNull()).isFalse
    }

    @Test
    fun `given enabled_features is missing, when invoked, then the value could not be determined`() = runTest {
        // A missing field must not read as "off" — that would block every store on a shape change.
        stubReport(EnabledFeatures.Unknown)

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh).isFailure).isTrue
    }

    @Test
    fun `given the report request fails, when invoked, then the value could not be determined`() = runTest {
        stubReportFailure()

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh).isFailure).isTrue
    }

    @Test
    fun `given no site is selected, when invoked, then the value could not be determined`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(null)

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh).isFailure).isTrue
    }

    @Test
    fun `given ForceRefresh, when invoked, then the plugins and settings are fetched in one request`() = runTest {
        stubReport("point_of_sale")

        sut(WooPosLaunchabilityRefreshPolicy.ForceRefresh)

        verify(wooCommerceStore).fetchSitePluginsAndSettings(site)
    }

    // --- Stored value ---

    @Test
    fun `given nothing is stored, when invoked, then the report is read and the value is stored`() = runTest {
        stubStoredValue(null)
        stubReport("point_of_sale")

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh).getOrNull()).isTrue
        verifyStored(true)
    }

    @Test
    fun `given a stored value, when invoked, then it is returned without waiting on the report`() = runTest {
        stubStoredValue(false)
        stubReport("point_of_sale")

        // The stored "off" wins over the report, which says "on" — the report only updates the store.
        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh).getOrNull()).isFalse
    }

    @Test
    fun `given UseCacheAndRefresh and a stored value, when invoked, then the report is refreshed after`() = runTest {
        stubStoredValue(true)
        stubReport("other_feature")

        sut(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh)

        verifyStored(false)
    }

    @Test
    fun `given ForceRefresh, when invoked, then the stored value is ignored`() = runTest {
        stubStoredValue(true)
        stubReport("other_feature")

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.ForceRefresh).getOrNull()).isFalse
    }

    @Test
    fun `given the report cannot be read, when invoked, then the stored value is left alone`() = runTest {
        stubStoredValue(null)
        stubReportFailure()

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh).isFailure).isTrue
        verify(appPrefs, never()).setPOSFeatureSwitchEnabledForSite(any(), any(), any(), any())
    }

    // --- UseCache ---

    @Test
    fun `given UseCache and a stored value, when invoked, then it is returned and nothing is fetched`() = runTest {
        stubStoredValue(true)

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCache).getOrNull()).isTrue
        verify(wooCommerceStore, never()).fetchSitePluginsAndSettings(any())
    }

    @Test
    fun `given UseCache and nothing stored, when invoked, then it fails and nothing is fetched`() = runTest {
        stubStoredValue(null)

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCache).isFailure).isTrue
        verify(wooCommerceStore, never()).fetchSitePluginsAndSettings(any())
    }

    // --- Report handed over by the plugin check ---

    @Test
    fun `given a report from the plugin check, when invoked, then it is used without a fetch`() = runTest {
        val result = sut(
            WooPosLaunchabilityRefreshPolicy.ForceRefresh,
            report = WooPosSystemStatusReport(EnabledFeatures.Known(listOf("point_of_sale")))
        )

        assertThat(result.getOrNull()).isTrue
        verify(wooCommerceStore, never()).fetchSitePluginsAndSettings(any())
        verifyStored(true)
    }

    @Test
    fun `given a report without the switch, when invoked, then the switch is off`() = runTest {
        val result = sut(
            WooPosLaunchabilityRefreshPolicy.ForceRefresh,
            report = WooPosSystemStatusReport(EnabledFeatures.Known(listOf("other_feature")))
        )

        assertThat(result.getOrNull()).isFalse
        verify(wooCommerceStore, never()).fetchSitePluginsAndSettings(any())
    }

    @Test
    fun `given a report that omits the field, when invoked, then it fails and is not refetched`() = runTest {
        val result = sut(
            WooPosLaunchabilityRefreshPolicy.ForceRefresh,
            report = WooPosSystemStatusReport(EnabledFeatures.Unknown)
        )

        // A report that answered without the field is still an answer. Asking for the same report
        // again would return the same thing, so it must not be fetched twice.
        verify(wooCommerceStore, never()).fetchSitePluginsAndSettings(any())

        assertThat(result.isFailure).isTrue
        verify(appPrefs, never()).setPOSFeatureSwitchEnabledForSite(any(), any(), any(), any())
    }
}
