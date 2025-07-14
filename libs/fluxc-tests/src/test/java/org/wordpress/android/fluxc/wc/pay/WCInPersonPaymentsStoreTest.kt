package org.wordpress.android.fluxc.wc.pay

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.inperson.ConnectionTokenApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.inperson.InPersonPaymentsRestClient
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class WCInPersonPaymentsStoreTest {
    private val restClient = mock<InPersonPaymentsRestClient>()

    private lateinit var store: WCInPersonPaymentsStore

    @Before
    fun setUp() {
        store = WCInPersonPaymentsStore(initCoroutineEngine(), restClient)
    }

    @Test
    fun `given server returns valid token, when fetchConnectionToken, then result contains the token`() = test {
        val token = "valid token"
        whenever(restClient.fetchConnectionToken(any(), any())).thenReturn(
                WooPayload(ConnectionTokenApiResponse(token, false))
        )

        val result = store.fetchConnectionToken(mock(), mock())

        assertThat(result.model?.token).isEqualTo(token)
    }

    @Test
    fun `given server response is testMode=true, when fetchConnectionToken, then testMode=true returned`() = test {
        val isTestMode = true
        whenever(restClient.fetchConnectionToken(any(), any())).thenReturn(
                WooPayload(ConnectionTokenApiResponse("", isTestMode))
        )

        val result = store.fetchConnectionToken(mock(), mock())

        assertThat(result.model?.isTestMode).isEqualTo(isTestMode)
    }

    @Test
    fun `given server response is error, when fetchConnectionToken, then WooError returned`() = test {
        whenever(restClient.fetchConnectionToken(any(), any())).thenReturn(
                WooPayload(mock())
        )

        val result = store.fetchConnectionToken(mock(), mock())

        assertThat(result.error).isNotNull
    }
}
