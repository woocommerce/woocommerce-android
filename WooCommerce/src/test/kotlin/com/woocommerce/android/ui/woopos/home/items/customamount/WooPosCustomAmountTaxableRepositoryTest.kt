package com.woocommerce.android.ui.woopos.home.items.customamount

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class WooPosCustomAmountTaxableRepositoryTest {
    private val site = SiteModel().apply {
        id = 1
        siteId = 12345L
        selfHostedSiteId = 678L
    }
    private val selectedSite: SelectedSite = mock { on { getOrNull() } doReturn site }
    private val appPrefs: AppPrefs = mock()

    private val sut = WooPosCustomAmountTaxableRepository(
        selectedSite = selectedSite,
        appPrefs = appPrefs,
    )

    @Test
    fun `given a saved choice for the store, when read, then the saved choice is returned`() {
        // GIVEN
        whenever(appPrefs.getPOSCustomAmountLastTaxableForSite(1, 12345L, 678L)).thenReturn(true)

        // WHEN
        val result = sut.getLastIsTaxable()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given no selected store, when read, then charge taxes is off`() {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        val result = sut.getLastIsTaxable()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a selected store, when a choice is saved, then it is stored for that store`() {
        // WHEN
        sut.saveLastIsTaxable(true)

        // THEN
        verify(appPrefs).setPOSCustomAmountLastTaxableForSite(
            localSiteId = 1,
            remoteSiteId = 12345L,
            selfHostedSiteId = 678L,
            isTaxable = true,
        )
    }

    @Test
    fun `given no selected store, when a choice is saved, then nothing is stored`() {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        sut.saveLastIsTaxable(true)

        // THEN
        verify(appPrefs, never()).setPOSCustomAmountLastTaxableForSite(any(), any(), any(), any())
    }
}
