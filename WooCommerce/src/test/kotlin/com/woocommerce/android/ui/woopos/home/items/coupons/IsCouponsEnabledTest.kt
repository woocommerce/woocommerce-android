package com.woocommerce.android.ui.woopos.home.items.coupons


import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class IsCouponsEnabledTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private lateinit var isCouponsEnabled: IsCouponsEnabled
    private lateinit var wooCommerceStore: WooCommerceStore
    private lateinit var selectedSite: SelectedSite
    private val siteModel = SiteModel().apply { id = 123 }

    @Before
    fun setup() {
        selectedSite = mock()
        wooCommerceStore = mock()
        isCouponsEnabled = IsCouponsEnabled(wooCommerceStore, selectedSite)

        whenever(selectedSite.get()).thenReturn(siteModel)
    }

    @Test
    fun `returns true when coupons are enabled in site settings`() {
        // Given
        val siteSettings = mock<WCSettingsModel> {
            on { couponsEnabled } doReturn true
        }
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(siteSettings)

        // When
        val result = isCouponsEnabled()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `returns false when coupons are disabled in site settings`() {
        // Given
        val siteSettings = mock<WCSettingsModel> {
            on { couponsEnabled } doReturn false
        }
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(siteSettings)

        // When
        val result = isCouponsEnabled()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when site settings are null`() {
        // Given
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(null)

        // When
        val result = isCouponsEnabled()

        // Then
        assertThat(result).isFalse()
    }
}
