package com.woocommerce.android.tools

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore

class ResolveSiteBySiteIdTest {
    private val selectedSite: SelectedSite = mock()
    private val siteStore: SiteStore = mock()

    private val sut = ResolveSiteBySiteId(
        selectedSite = selectedSite,
        siteStore = siteStore
    )

    @Test
    fun `given app-password connection, when resolving, then return selected site without hitting site store`() {
        val payloadSiteId = 7777L
        val selected: SiteModel = mock()
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
        whenever(selectedSite.getOrNull()).thenReturn(selected)

        val result = sut(payloadSiteId)

        assertThat(result).isEqualTo(selected)
        verify(siteStore, never()).getSiteBySiteId(payloadSiteId)
    }

    @Test
    fun `given wpcom connection, when resolving, then look the site up by its wpcom id`() {
        val payloadSiteId = 12345L
        val wpcomSite: SiteModel = mock()
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.Jetpack)
        whenever(siteStore.getSiteBySiteId(payloadSiteId)).thenReturn(wpcomSite)

        val result = sut(payloadSiteId)

        assertThat(result).isEqualTo(wpcomSite)
    }
}
