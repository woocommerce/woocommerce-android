package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosItemsTabsHelperTest {
    @Test
    fun `when helper initialized, then default tabs include products and coupons`() {
        // WHEN
        val tabsHelper = WooPosItemsTabsHelper()

        // THEN
        assertThat(tabsHelper.defaultTabs).hasSize(2)
        assertThat(tabsHelper.defaultTabs[0].stringId).isEqualTo(R.string.woopos_products_screen_title)
        assertThat(tabsHelper.defaultTabs[0].highlightLevel).isEqualTo(WooPosItemsToolbarViewState.Tab.HighlightLevel.Full)
        assertThat(tabsHelper.defaultTabs[1].stringId).isEqualTo(R.string.woopos_coupons_screen_title)
        assertThat(tabsHelper.defaultTabs[1].highlightLevel).isEqualTo(WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal)
    }

    @Test
    fun `when tab is selected, then selected tab has Full highlight level and others have Normal`() {
        // GIVEN
        val tabsHelper = WooPosItemsTabsHelper()
        val tabs = tabsHelper.defaultTabs
        val tabToSelect = tabs[1]

        // WHEN
        val updatedTabs = tabsHelper.selectTab(tabs, tabToSelect)

        // THEN
        assertThat(updatedTabs).hasSize(2)
        assertThat(updatedTabs[0].highlightLevel).isEqualTo(WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal)
        assertThat(updatedTabs[1].highlightLevel).isEqualTo(WooPosItemsToolbarViewState.Tab.HighlightLevel.Full)
    }

    @Test
    fun `when already selected tab is selected again, then highlight levels remain unchanged`() {
        // GIVEN
        val tabsHelper = WooPosItemsTabsHelper()
        val tabs = tabsHelper.defaultTabs
        val tabToSelect = tabs[0]

        // WHEN
        val updatedTabs = tabsHelper.selectTab(tabs, tabToSelect)

        // THEN
        assertThat(updatedTabs).hasSize(2)
        assertThat(updatedTabs[0].highlightLevel).isEqualTo(WooPosItemsToolbarViewState.Tab.HighlightLevel.Full)
        assertThat(updatedTabs[1].highlightLevel).isEqualTo(WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal)
    }
}
