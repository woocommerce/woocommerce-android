package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.PushNotificationsRestClient.PushTokenIdResponse
import org.wordpress.android.fluxc.utils.PreferenceUtils
import org.wordpress.android.fluxc.utils.initCoroutineEngine

class PushNotificationsStoreTest {
    private val restClient: PushNotificationsRestClient = mock()
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper = mock()
    private val prefs: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()

    private lateinit var sut: PushNotificationsStore

    @Before
    fun setUp() {
        whenever(prefsWrapper.getFluxCPreferences()).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putStringSet(any(), any())).thenReturn(editor)

        sut = PushNotificationsStore(
            pushNotificationsRestClient = restClient,
            prefsWrapper = prefsWrapper,
            coroutineEngine = initCoroutineEngine()
        )
    }

    @Test
    fun `given no token, when registerPushToken succeeds, then saves new token`() {
        runBlocking {
            // GIVEN
            val site = SiteModel().apply { id = 123 }
            val newTokenId = "101"
            whenever(prefs.getString(eq("push_token_id"), anyOrNull())).thenReturn(null)
            whenever(restClient.registerPushToken(any(), any(), any(), any()))
                .thenReturn(WooPayload(PushTokenIdResponse(newTokenId)))

            // WHEN
            sut.registerPushToken(site, "token", "uuid")

            // THEN
            val captor = argumentCaptor<String>()
            verify(editor).putString(eq("push_token_id"), captor.capture())
            assertThat(captor.firstValue).isEqualTo(newTokenId)
        }
    }

    @Test
    fun `given existing token, when deletePushToken succeeds, then clears token`() {
        runBlocking {
            // GIVEN
            val site = SiteModel().apply { id = 123 }
            val existingTokenId = "100"
            whenever(
                prefs.getString(
                    eq("push_token_id"),
                    anyOrNull()
                )
            ).thenReturn(existingTokenId)
            whenever(restClient.deletePushToken(any(), any())).thenReturn(WooPayload(Unit))

            // WHEN
            sut.deletePushToken(site)

            // THEN
            verify(editor).remove(eq("push_token_id"))
        }
    }

    @Test
    fun `given no existing token, when deletePushToken called, then returns error`() {
        runBlocking {
            // GIVEN
            val site = SiteModel().apply { id = 123 }
            whenever(prefs.getString(eq("push_token_id"), anyOrNull())).thenReturn(null)

            // WHEN
            val result = sut.deletePushToken(site)

            // THEN
            assertThat(result.isError).isTrue()
        }
    }
}
