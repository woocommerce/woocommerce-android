package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WooCommerceStore

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
    fun `returns true when coupons are enabled in site settings`() = runTest {
        // GIVEN
        val siteSettings = mock<WCSettingsModel> {
            on { couponsEnabled } doReturn true
        }
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

        // WHEN
        val result = cachedCouponEnabledChecker.isEnabled()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `returns false when coupons are disabled in site settings`() = runTest {
        // GIVEN
        val siteSettings = mock<WCSettingsModel> {
            on { couponsEnabled } doReturn false
        }
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

        // WHEN
        val result = cachedCouponEnabledChecker.isEnabled()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when site settings are null`() = runTest {
        // GIVEN
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(null)

        // WHEN
        val result = cachedCouponEnabledChecker.isEnabled()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `uses cached value on subsequent calls`() = runTest {
        // GIVEN
        val siteSettings = mock<WCSettingsModel> {
            on { couponsEnabled } doReturn true
        }
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

        // WHEN
        val firstResult = cachedCouponEnabledChecker.isEnabled()

        val newSettings = mock<WCSettingsModel> {
            on { couponsEnabled } doReturn false
        }
        whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(newSettings)

        val secondResult = cachedCouponEnabledChecker.isEnabled()

        // THEN
        assertThat(firstResult).isTrue()
        assertThat(secondResult).isTrue() // Still true from cache, not false from new mock
    }
}
