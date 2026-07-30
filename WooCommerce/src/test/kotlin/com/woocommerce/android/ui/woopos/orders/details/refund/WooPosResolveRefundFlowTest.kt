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
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_REFUND_V4) } doReturn true
    }

    private val site = SiteModel().apply { id = LOCAL_SITE_ID }

    private val sut by lazy {
        whenever(selectedSite.get()).thenReturn(site)
        WooPosResolveRefundFlow(selectedSite, availabilityCache, getWooCoreVersion, featureFlagRepository)
    }

    @Test
    fun `given flag disabled, when resolved, then flow is local`() {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_REFUND_V4)).thenReturn(false)

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
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed)
    }

    @Test
    fun `given WC above 11_1_0, when resolved, then flow is server`() {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn("11.2.0")

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed)
    }

    @Test
    fun `given WC version unknown, when resolved, then flow is server so the preview probe can settle it`() {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn(null)

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed)
    }

    @Test
    fun `given server refunds known unavailable, when resolved, then flow is local`() {
        // GIVEN
        availabilityCache.markUnavailable(LOCAL_SITE_ID)

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.LocalComputed)
    }

    @Test
    fun `given server refunds confirmed available, when resolved, then flow is server`() {
        // GIVEN
        availabilityCache.markAvailable(LOCAL_SITE_ID)

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed)
    }

    @Test
    fun `given another site is unavailable, when resolved, then this site is still eligible`() {
        // GIVEN — the cache is keyed by local site id; another store's verdict must not bleed over.
        availabilityCache.markUnavailable(LOCAL_SITE_ID + 1)

        // THEN
        assertThat(sut()).isEqualTo(WooPosRefundFlow.ServerComputed)
    }

    private companion object {
        private const val LOCAL_SITE_ID = 11
    }
}
