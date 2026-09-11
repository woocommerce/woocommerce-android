package com.woocommerce.android.ui.jetpack

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackConnectionStatusResponse
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.OfflineMode
import org.wordpress.android.fluxc.store.JetpackStore

@OptIn(ExperimentalCoroutinesApi::class)
class IsJetpackInOfflineModeTest : BaseUnitTest() {
    private val jetpackStore: JetpackStore = mock()
    private val site: SiteModel = SiteModel()

    private val sut = IsJetpackInOfflineMode(jetpackStore)

    @Test
    fun `given offline mode is active, when invoked, then returns true`() = testBlocking {
        stubConnection(OfflineMode(isActive = true))

        val result = sut(site, useApplicationPasswords = true)

        assertThat(result).isTrue()
    }

    @Test
    fun `given offline mode is inactive, when invoked, then returns false`() = testBlocking {
        stubConnection(OfflineMode(isActive = false))

        val result = sut(site, useApplicationPasswords = true)

        assertThat(result).isFalse()
    }

    @Test
    fun `given offline mode isActive is null, when invoked, then returns false`() = testBlocking {
        stubConnection(OfflineMode(isActive = null))

        val result = sut(site, useApplicationPasswords = true)

        assertThat(result).isFalse()
    }

    @Test
    fun `given offline mode is missing, when invoked, then returns false`() = testBlocking {
        stubConnection(offlineMode = null)

        val result = sut(site, useApplicationPasswords = true)

        assertThat(result).isFalse()
    }

    @Test
    fun `given the connection fetch fails, when invoked, then returns false`() = testBlocking {
        whenever(jetpackStore.fetchJetpackConnection(any(), any()))
            .thenReturn(JetpackStore.JetpackResult(JetpackStore.JetpackError()))

        val result = sut(site, useApplicationPasswords = true)

        assertThat(result).isFalse()
    }

    private suspend fun stubConnection(offlineMode: OfflineMode?) {
        whenever(jetpackStore.fetchJetpackConnection(any(), any()))
            .thenReturn(
                JetpackStore.JetpackResult(
                    JetpackConnectionStatusResponse(isActive = false, offlineMode = offlineMode)
                )
            )
    }
}
