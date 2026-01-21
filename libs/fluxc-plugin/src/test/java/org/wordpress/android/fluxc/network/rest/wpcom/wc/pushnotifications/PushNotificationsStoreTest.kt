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
            whenever(prefs.getStringSet(eq("push_token_ids"), anyOrNull())).thenReturn(emptySet())
            whenever(restClient.registerPushToken(any(), any(), any(), any()))
                .thenReturn(WooPayload(PushTokenIdResponse(newTokenId)))

            // WHEN
            sut.registerPushToken(site, "token", "uuid")

            // THEN
            val captor = argumentCaptor<Set<String>>()
            verify(editor).putStringSet(eq("push_token_ids"), captor.capture())
            assertThat(captor.firstValue).containsExactly("${site.id}:$newTokenId")
        }
    }

    @Test
    fun `given existing token, when registerPushToken succeeds, then updates existing token`() {
        runBlocking {
            // GIVEN
            val site = SiteModel().apply { id = 123 }
            val oldTokenId = "100"
            val newTokenId = "101"
            whenever(prefs.getStringSet(eq("push_token_ids"), anyOrNull())).thenReturn(setOf("${site.id}:$oldTokenId"))
            whenever(restClient.registerPushToken(any(), any(), any(), any()))
                .thenReturn(WooPayload(PushTokenIdResponse(newTokenId)))

            // WHEN
            sut.registerPushToken(site, "token", "uuid")

            // THEN
            val captor = argumentCaptor<Set<String>>()
            verify(editor).putStringSet(eq("push_token_ids"), captor.capture())
            assertThat(captor.firstValue).containsExactly("${site.id}:$newTokenId")
        }
    }

    @Test
    fun `given existing token, when deletePushToken succeeds, then clears token`() {
        runBlocking {
            // GIVEN
            val site = SiteModel().apply { id = 123 }
            val existingTokenId = "100"
            val otherToken = "999:200"
            whenever(
                prefs.getStringSet(
                    eq("push_token_ids"),
                    anyOrNull()
                )
            ).thenReturn(setOf("${site.id}:$existingTokenId", otherToken))
            whenever(restClient.deletePushToken(any(), any())).thenReturn(WooPayload(Unit))

            // WHEN
            sut.deletePushToken(site)

            // THEN
            val captor = argumentCaptor<Set<String>>()
            verify(editor).putStringSet(eq("push_token_ids"), captor.capture())
            assertThat(captor.firstValue).containsExactly(otherToken)
        }
    }

    @Test
    fun `given no existing token, when deletePushToken called, then returns error`() {
        runBlocking {
            // GIVEN
            val site = SiteModel().apply { id = 123 }
            whenever(prefs.getStringSet(eq("push_token_ids"), anyOrNull())).thenReturn(emptySet())

            // WHEN
            val result = sut.deletePushToken(site)

            // THEN
            assertThat(result.isError).isTrue()
        }
    }
}
