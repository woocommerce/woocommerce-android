package org.wordpress.android.fluxc.store

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackConnectionProvisionResponse
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackConnectionStatusResponse
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.OfflineMode
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.Test

@RunWith(MockitoJUnitRunner::class)
class JetpackStoreTest {
    private val jetpackWPAPIRestClient: JetpackWPAPIRestClient = mock()
    private val site: SiteModel = SiteModel()

    private val jetpackStore: JetpackStore = JetpackStore(
        jetpackWPAPIRestClient,
        initCoroutineEngine()
    )

    @Test
    fun `when registerSite is called, then call jetpackWPAPIRestClient`() {
        runBlocking {
            whenever(jetpackWPAPIRestClient.registerSite(site, useApplicationPasswords = true))
                .thenReturn(JetpackWPAPIRestClient.JetpackWPAPIPayload(1L))

            val result = jetpackStore.registerSite(site, useApplicationPasswords = true)

            verify(jetpackWPAPIRestClient).registerSite(site, useApplicationPasswords = true)
            assertThat(result).isEqualTo(JetpackStore.JetpackResult(1L))
        }
    }

    @Test
    fun `when fetchJetpackConnection is called, then call jetpackWPAPIRestClient and return the response`() {
        runBlocking {
            val response = JetpackConnectionStatusResponse(
                isActive = false,
                offlineMode = OfflineMode(isActive = true)
            )
            whenever(jetpackWPAPIRestClient.fetchJetpackConnection(site, useApplicationPasswords = true))
                .thenReturn(JetpackWPAPIRestClient.JetpackWPAPIPayload(response))

            val result = jetpackStore.fetchJetpackConnection(site, useApplicationPasswords = true)

            verify(jetpackWPAPIRestClient).fetchJetpackConnection(site, useApplicationPasswords = true)
            assertThat(result).isEqualTo(JetpackStore.JetpackResult(response))
        }
    }

    @Test
    fun `given the client returns an error, when fetchJetpackConnection is called, then return a JetpackError`() {
        runBlocking {
            val error = BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.SERVER_ERROR, "boom")
            whenever(jetpackWPAPIRestClient.fetchJetpackConnection(site, useApplicationPasswords = true))
                .thenReturn(JetpackWPAPIRestClient.JetpackWPAPIPayload(error))

            val result = jetpackStore.fetchJetpackConnection(site, useApplicationPasswords = true)

            assertThat(result.isError).isTrue()
            assertThat(result.data).isNull()
            assertThat(result.error?.message).isEqualTo("boom")
        }
    }

    @Test
    fun `when connectJetpackAccount is called, then call jetpackWPAPIRestClient`() {
        runBlocking {
            val blogId = 1L
            val provisionResponse = JetpackConnectionProvisionResponse(
                userId = 1L,
                secret = "secret",
                scope = "scope"
            )
            whenever(jetpackWPAPIRestClient.provisionConnection(site, useApplicationPasswords = true))
                .thenReturn(JetpackWPAPIRestClient.JetpackWPAPIPayload(provisionResponse))
            whenever(jetpackWPAPIRestClient.connectJetpackAccount(site, blogId, provisionResponse))
                .thenReturn(JetpackWPAPIRestClient.JetpackWPAPIPayload(Unit))

            val result = jetpackStore.connectJetpackAccount(site, blogId = blogId, useApplicationPasswords = true)

            verify(jetpackWPAPIRestClient).provisionConnection(site, useApplicationPasswords = true)
            verify(jetpackWPAPIRestClient).connectJetpackAccount(site, blogId, provisionResponse)
            assertThat(result).isEqualTo(JetpackStore.JetpackResult(Unit))
        }
    }
}
