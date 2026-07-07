package com.woocommerce.android.network

import com.woocommerce.android.tools.SelectedSite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class StoreConnectionErrorMonitorTest {
    private val selectedSite: SelectedSite = mock()
    private val sut = StoreConnectionErrorMonitor(selectedSite)

    private fun site(id: Long) = SiteModel().apply { siteId = id }

    @Test
    fun `when invalid signature is detected, then expose the affected site id`() {
        sut.onInvalidSignatureDetected(site(123L))

        assertThat(sut.invalidSignatureDetected.value).isEqualTo(123L)
    }

    @Test
    fun `given affected site, when a request to it succeeds, then clear the state`() {
        sut.onInvalidSignatureDetected(site(123L))

        sut.onSuccessfulConnection(site(123L))

        assertThat(sut.invalidSignatureDetected.value).isNull()
    }

    @Test
    fun `given affected site, when a request to a different site succeeds, then keep the state`() {
        sut.onInvalidSignatureDetected(site(123L))

        sut.onSuccessfulConnection(site(456L))

        assertThat(sut.invalidSignatureDetected.value).isEqualTo(123L)
    }

    @Test
    fun `given selected site is affected, when checking selected site, then return true`() {
        whenever(selectedSite.getOrNull()).thenReturn(site(123L))
        sut.onInvalidSignatureDetected(site(123L))

        assertThat(sut.isDetectedForSelectedSite()).isTrue()
    }

    @Test
    fun `given a different site is affected, when checking selected site, then return false`() {
        whenever(selectedSite.getOrNull()).thenReturn(site(123L))
        sut.onInvalidSignatureDetected(site(456L))

        assertThat(sut.isDetectedForSelectedSite()).isFalse()
    }
}
