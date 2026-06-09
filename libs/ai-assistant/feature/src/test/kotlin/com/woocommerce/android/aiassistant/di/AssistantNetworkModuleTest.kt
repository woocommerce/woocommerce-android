package com.woocommerce.android.aiassistant.di

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.network.UserAgent

class AssistantNetworkModuleTest {
    @Test
    fun `given base client has cookies, when assistant client is created, then cookies are disabled`() {
        val client = AssistantNetworkModule.provideAssistantOkHttpClient(
            base = OkHttpClient.Builder()
                .cookieJar(mock<CookieJar>())
                .build(),
            userAgent = mock<UserAgent>(),
        )

        assertThat(client.cookieJar).isSameAs(CookieJar.NO_COOKIES)
    }
}
