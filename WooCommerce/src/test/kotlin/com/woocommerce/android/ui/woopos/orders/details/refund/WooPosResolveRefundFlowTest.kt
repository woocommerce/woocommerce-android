package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class WooPosResolveRefundFlowTest {

    private val selectedSite: SelectedSite = mock()
    private val availabilityCache = WooPosServerRefundAvailabilityCache()

    // Defaults to a version that supports server refunds; version-gating tests override it.
    // An unknown (null) version fails closed to the local flow.
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock {
        on { invoke() } doReturn WooPosResolveRefundFlow.MIN_WC_VERSION_FOR_SERVER_REFUNDS
    }
    private val featureFlagRepository: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_SERVER_REFUNDS) } doReturn true
    }

    private val site = SiteModel().apply { id = LOCAL_SITE_ID }

    private val sut by lazy {
        whenever(selectedSite.get()).thenReturn(site)
        WooPosResolveRefundFlow(selectedSite, availabilityCache, getWooCoreVersion, featureFlagRepository)
    }

    @Test
    fun `given flag disabled, when resolved, then flow is local`() {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_SERVER_REFUNDS)).thenReturn(false)

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.LocalComputed)
    }

    @Test
    fun `given WC below 11_1_0, when resolved, then flow is local`() {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn("11.0.5")

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.LocalComputed)
    }

    @Test
    fun `given WC at exactly 11_1_0, when resolved, then flow is server`() {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn("11.1.0")

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed("11.1.0"))
    }

    @Test
    fun `given WC above 11_1_0, when resolved, then flow is server`() {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn("11.2.0")

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed("11.2.0"))
    }

    @Test
    fun `given WC version unknown, when resolved, then flow is local because preview alone must not unlock the create`() {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn(null)

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.LocalComputed)
    }

    @Test
    fun `given WC version unknown and availability cached true, when resolved, then flow is still local`() {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn(null)
        availabilityCache.markAvailable(LOCAL_SITE_ID, MIN_VERSION)

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.LocalComputed)
    }

    @Test
    fun `given server refunds known unavailable, when resolved, then flow is local`() {
        // GIVEN
        availabilityCache.markUnavailable(LOCAL_SITE_ID, MIN_VERSION)

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.LocalComputed)
    }

    @Test
    fun `given server refunds confirmed available, when resolved, then flow is server`() {
        // GIVEN
        availabilityCache.markAvailable(LOCAL_SITE_ID, MIN_VERSION)

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed(MIN_VERSION))
    }

    @Test
    fun `given a prerelease of the minimum version, when resolved, then flow is server`() {
        // GIVEN — trunk builds report `11.1.0-dev`, and betas `11.1.0-beta1`. Neither may read as
        // below 11.1.0, otherwise testers silently get the local flow.
        whenever(getWooCoreVersion.invoke()).thenReturn("11.1.0-dev")

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed("11.1.0-dev"))
    }

    @Test
    fun `given a verdict probed on an older version, when the store upgrades, then flow is server again`() {
        // GIVEN a store probed as unavailable while it ran an older WooCommerce
        availabilityCache.markUnavailable(LOCAL_SITE_ID, "11.0.5")

        // WHEN it now reports a supported version
        whenever(getWooCoreVersion.invoke()).thenReturn(MIN_VERSION)

        // THEN the stale verdict does not pin it to local calculation for the rest of the process
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed(MIN_VERSION))
    }

    @Test
    fun `given another site is unavailable, when resolved, then this site is still eligible`() {
        // GIVEN — the cache is keyed by local site id; another store's verdict must not bleed over.
        availabilityCache.markUnavailable(LOCAL_SITE_ID + 1, MIN_VERSION)

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed(MIN_VERSION))
    }

    private companion object {
        private const val LOCAL_SITE_ID = 11
        private const val MIN_VERSION = WooPosResolveRefundFlow.MIN_WC_VERSION_FOR_SERVER_REFUNDS
    }
}
