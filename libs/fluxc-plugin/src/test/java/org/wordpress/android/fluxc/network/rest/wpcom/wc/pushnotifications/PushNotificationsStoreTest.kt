package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.PushNotificationsRestClient.PushTokenIdResponse
import org.wordpress.android.fluxc.utils.initCoroutineEngine

class PushNotificationsStoreTest {
    private val restClient: PushNotificationsRestClient = mock()

    private lateinit var sut: WooPushNotificationsStore

    @Before
    fun setUp() {
        sut = WooPushNotificationsStore(
            pushNotificationsRestClient = restClient,
            coroutineEngine = initCoroutineEngine()
        )
    }

    @Test
    fun `when registerPushToken succeeds, then returns token id`() {
        runBlocking {
            val site = SiteModel().apply { id = 123 }
            val tokenId = "101"
            whenever(restClient.registerPushToken(any(), any()))
                .thenReturn(WooPayload(PushTokenIdResponse(tokenId)))

            val result = sut.registerPushToken(site, "token", "uuid", "en_US")

            assertThat(result.isError).isFalse()
            assertThat(result.model).isEqualTo(tokenId)
        }
    }

    @Test
    fun `when deletePushToken succeeds, then returns success`() {
        runBlocking {
            val site = SiteModel().apply { id = 123 }
            val pushTokenId = "100"
            whenever(restClient.deletePushToken(any(), any())).thenReturn(WooPayload(Unit))

            val result = sut.deletePushToken(site, pushTokenId)

            assertThat(result.isError).isFalse()
            verify(restClient).deletePushToken(site, pushTokenId)
        }
    }
}
