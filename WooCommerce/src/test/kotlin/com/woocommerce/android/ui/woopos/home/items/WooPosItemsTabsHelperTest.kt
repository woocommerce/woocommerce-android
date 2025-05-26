package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.HighlightLevel
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class WooPosItemsTabsHelperTest {
    private val resourceProvider: ResourceProvider = mock() {
        on { getString(R.string.woopos_products_screen_title) }.thenReturn("Products")
        on { getString(R.string.woopos_coupons_screen_title) }.thenReturn("Coupons")
    }

    @Test
    fun `when helper initialized, then default tabs include products and coupons`() {
        // WHEN
        val tabsHelper = WooPosItemsTabsHelper(resourceProvider)

        // THEN
        assertThat(tabsHelper.defaultTabs).hasSize(2)
        assertThat(tabsHelper.defaultTabs[0].name).isEqualTo("Products")
        assertThat(tabsHelper.defaultTabs[0].highlightLevel).isEqualTo(HighlightLevel.Full)
        assertThat(tabsHelper.defaultTabs[1].name).isEqualTo("Coupons")
        assertThat(tabsHelper.defaultTabs[1].highlightLevel).isEqualTo(HighlightLevel.Normal)
    }

    @Test
    fun `when tab is selected, then selected tab has Full highlight level and others have Normal`() {
        // GIVEN
        val tabsHelper = WooPosItemsTabsHelper(resourceProvider)
        val tabs = tabsHelper.defaultTabs
        val tabToSelect = tabs[1]

        // WHEN
        val updatedTabs = tabsHelper.selectTab(tabs, tabToSelect)

        // THEN
        assertThat(updatedTabs).hasSize(2)
        assertThat(updatedTabs[0].highlightLevel).isEqualTo(HighlightLevel.Normal)
        assertThat(updatedTabs[1].highlightLevel).isEqualTo(HighlightLevel.Full)
    }

    @Test
    fun `when already selected tab is selected again, then highlight levels remain unchanged`() {
        // GIVEN
        val tabsHelper = WooPosItemsTabsHelper(resourceProvider)
        val tabs = tabsHelper.defaultTabs
        val tabToSelect = tabs[0]

        // WHEN
        val updatedTabs = tabsHelper.selectTab(tabs, tabToSelect)

        // THEN
        assertThat(updatedTabs).hasSize(2)
        assertThat(updatedTabs[0].highlightLevel).isEqualTo(HighlightLevel.Full)
        assertThat(updatedTabs[1].highlightLevel).isEqualTo(HighlightLevel.Normal)
    }
}
