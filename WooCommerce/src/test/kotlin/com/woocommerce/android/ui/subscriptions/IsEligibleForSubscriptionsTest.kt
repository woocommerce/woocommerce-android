package com.woocommerce.android.ui.subscriptions

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class IsEligibleForSubscriptionsTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val site: SiteModel = mock()

    private lateinit var isEligibleForSubscriptions: IsEligibleForSubscriptions

    @Before
    fun setUp() {
        whenever(selectedSite.get()).thenReturn(site)

        isEligibleForSubscriptions = IsEligibleForSubscriptions(selectedSite, wooCommerceStore)
    }

    @Test
    fun `returns true when plugin is present and active`() = testBlocking {
        val plugin = spy(SitePluginModel().apply { slug = "woocommerce-subscriptions" })

        whenever(plugin.isActive).thenReturn(true)
        whenever(wooCommerceStore.getSitePlugins(site)).thenReturn(listOf(plugin))

        val result = isEligibleForSubscriptions.invoke()

        assertTrue(result)
    }

    @Test
    fun `returns false plugin is not present`() = testBlocking {
        whenever(wooCommerceStore.getSitePlugins(site)).thenReturn(emptyList())

        val result = isEligibleForSubscriptions.invoke()

        assertFalse(result)
    }

    @Test
    fun `returns false when plugin is present but not active`() = testBlocking {
        val plugin = spy(SitePluginModel().apply { slug = "woocommerce-subscriptions" })

        whenever(plugin.isActive).thenReturn(false)

        whenever(wooCommerceStore.getSitePlugins(site)).thenReturn(listOf(plugin))

        val result = isEligibleForSubscriptions.invoke()

        assertFalse(result)
    }
}
