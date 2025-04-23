package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsCouponsEnabled
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosItemsTabsHelperTest {
    private val isCouponsEnabled: WooPosIsCouponsEnabled = mock()

    @Test
    fun `given coupons feature is enabled, when helper initialized, then default tabs include products and coupons`() {
        // GIVEN
        whenever(isCouponsEnabled()).thenReturn(true)

        // WHEN
        val tabsHelper = WooPosItemsTabsHelper(isCouponsEnabled)

        // THEN
        assertThat(tabsHelper.defaultTabs).hasSize(2)
        assertThat(tabsHelper.defaultTabs[0].stringId).isEqualTo(R.string.woopos_products_screen_title)
        assertThat(tabsHelper.defaultTabs[0].highlightLevel).isEqualTo(WooPosItemsViewState.Tab.HighlightLevel.Full)
        assertThat(tabsHelper.defaultTabs[1].stringId).isEqualTo(R.string.woopos_coupons_screen_title)
        assertThat(tabsHelper.defaultTabs[1].highlightLevel).isEqualTo(WooPosItemsViewState.Tab.HighlightLevel.Normal)
    }

    @Test
    fun `given coupons feature is disabled, when helper initialized, then default tabs include only products`() {
        // GIVEN
        whenever(isCouponsEnabled()).thenReturn(false)

        // WHEN
        val tabsHelper = WooPosItemsTabsHelper(isCouponsEnabled)

        // THEN
        assertThat(tabsHelper.defaultTabs).hasSize(1)
        assertThat(tabsHelper.defaultTabs[0].stringId).isEqualTo(R.string.woopos_products_screen_title)
        assertThat(tabsHelper.defaultTabs[0].highlightLevel).isEqualTo(WooPosItemsViewState.Tab.HighlightLevel.Full)
    }

    @Test
    fun `when tab is selected, then selected tab has Full highlight level and others have Normal`() {
        // GIVEN
        whenever(isCouponsEnabled()).thenReturn(true)
        val tabsHelper = WooPosItemsTabsHelper(isCouponsEnabled)
        val tabs = tabsHelper.defaultTabs
        val tabToSelect = tabs[1]

        // WHEN
        val updatedTabs = tabsHelper.selectTab(tabs, tabToSelect)

        // THEN
        assertThat(updatedTabs).hasSize(2)
        assertThat(updatedTabs[0].highlightLevel).isEqualTo(WooPosItemsViewState.Tab.HighlightLevel.Normal)
        assertThat(updatedTabs[1].highlightLevel).isEqualTo(WooPosItemsViewState.Tab.HighlightLevel.Full)
    }

    @Test
    fun `when already selected tab is selected again, then highlight levels remain unchanged`() {
        // GIVEN
        whenever(isCouponsEnabled()).thenReturn(true)
        val tabsHelper = WooPosItemsTabsHelper(isCouponsEnabled)
        val tabs = tabsHelper.defaultTabs
        val tabToSelect = tabs[0]

        // WHEN
        val updatedTabs = tabsHelper.selectTab(tabs, tabToSelect)

        // THEN
        assertThat(updatedTabs).hasSize(2)
        assertThat(updatedTabs[0].highlightLevel).isEqualTo(WooPosItemsViewState.Tab.HighlightLevel.Full)
        assertThat(updatedTabs[1].highlightLevel).isEqualTo(WooPosItemsViewState.Tab.HighlightLevel.Normal)
    }
}
