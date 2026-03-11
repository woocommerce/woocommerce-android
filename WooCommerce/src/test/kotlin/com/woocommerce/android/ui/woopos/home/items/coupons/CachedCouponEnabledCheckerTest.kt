package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.wc.settings.WCSettingsTestUtils

@ExperimentalCoroutinesApi
class CachedCouponEnabledCheckerTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private lateinit var cachedCouponEnabledChecker: CachedCouponEnabledChecker
    private lateinit var wooCommerceStore: WooCommerceStore
    private lateinit var selectedSite: SelectedSite
    private val siteModel = SiteModel().apply { id = 123 }

    @Before
    fun setup() {
        selectedSite = mock()
        wooCommerceStore = mock()
        cachedCouponEnabledChecker = CachedCouponEnabledChecker(wooCommerceStore, selectedSite)

        whenever(selectedSite.get()).thenReturn(siteModel)
    }

    @Test
    fun `when coupons are enabled in site settings, then returns true`() = runTest {
        // GIVEN
        val siteSettings = WCSettingsTestUtils.generateSettings(siteModel.localId()).copy(couponsEnabled = true)
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

        // WHEN
        val result = cachedCouponEnabledChecker.isEnabled()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `when coupons are disabled in site settings, then returns false`() = runTest {
        // GIVEN
        val siteSettings = WCSettingsTestUtils.generateSettings(siteModel.localId()).copy(couponsEnabled = false)
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

        // WHEN
        val result = cachedCouponEnabledChecker.isEnabled()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `when site settings are null, then returns false`() = runTest {
        // GIVEN
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(null)

        // WHEN
        val result = cachedCouponEnabledChecker.isEnabled()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given value is already cached, when checking again, then returns cached value`() = runTest {
        // GIVEN
        val siteSettings = WCSettingsTestUtils.generateSettings(siteModel.localId()).copy(couponsEnabled = true)
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

        // WHEN
        val firstResult = cachedCouponEnabledChecker.isEnabled()

        val newSettings = WCSettingsTestUtils.generateSettings(siteModel.localId()).copy(couponsEnabled = false)
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(newSettings)

        val secondResult = cachedCouponEnabledChecker.isEnabled()

        // THEN
        assertThat(firstResult).isTrue()
        assertThat(secondResult).isTrue() // Still true from cache, not false from new mock
    }

    @Test
    fun `given value is cached for one site, when site changes, then fetches fresh value`() = runTest {
        // GIVEN
        val siteSettings = WCSettingsTestUtils.generateSettings(siteModel.localId()).copy(couponsEnabled = true)
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)
        cachedCouponEnabledChecker.isEnabled()

        // WHEN
        val newSite = SiteModel().apply { id = 456 }
        val newSiteSettings = WCSettingsTestUtils.generateSettings(newSite.localId()).copy(couponsEnabled = false)
        whenever(selectedSite.get()).thenReturn(newSite)
        whenever(wooCommerceStore.getSiteSettingsAsync(newSite)).thenReturn(newSiteSettings)

        val result = cachedCouponEnabledChecker.isEnabled()

        // THEN
        assertThat(result).isFalse()
    }
}
