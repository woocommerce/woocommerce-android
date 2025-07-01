package com.woocommerce.android.ui.woopos.util

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.wc.settings.WCSettingsTestUtils

@ExperimentalCoroutinesApi
class WooPosGetCachedStoreCurrencyTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1))
    private val wooCommerceStore: WooCommerceStore = mock {
        on { getSiteSettings(any()) }.thenReturn(siteSettings)
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(mock())
    }

    private val getCachedStoreCurrency = WooPosGetCachedStoreCurrency(wooCommerceStore, selectedSite)

    @Test
    fun `given USD site, when invoked, then returns USD`() = runTest {
        // When
        val result = getCachedStoreCurrency()

        // Then
        assert(result == "USD")
    }

    @Test
    fun `given store currency cached, when invoked, then do not hit database`() = runTest {
        // Given
        getCachedStoreCurrency()
        clearInvocations(wooCommerceStore)

        // When
        getCachedStoreCurrency()

        // Then
        verify(wooCommerceStore, never()).getSiteSettings(any())
    }
}
