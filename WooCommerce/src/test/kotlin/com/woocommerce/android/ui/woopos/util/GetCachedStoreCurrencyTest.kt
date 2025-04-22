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
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class GetCachedStoreCurrencyTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val siteSettings: WCSettingsModel = mock()
    private val wooCommerceStore: WooCommerceStore = mock {
        on { getSiteSettings(any()) }.thenReturn(siteSettings)
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(mock())
    }

    private val getCachedStoreCurrency = GetCachedStoreCurrency(wooCommerceStore, selectedSite)

    @Test
    fun `given USD site, when invoked, then returns USD`() = runTest {
        // Given
        whenever(siteSettings.currencyCode).thenReturn("USD")

        // When
        val result = getCachedStoreCurrency()

        // Then
        assert(result == "USD")
    }

    @Test
    fun `given store currency cached, when invoked, then do not hit database`() = runTest {
        // Given
        whenever(siteSettings.currencyCode).thenReturn("USD")
        getCachedStoreCurrency()
        clearInvocations(wooCommerceStore)

        // When
        getCachedStoreCurrency()

        // Then
        verify(wooCommerceStore, never()).getSiteSettings(any())
    }
}
