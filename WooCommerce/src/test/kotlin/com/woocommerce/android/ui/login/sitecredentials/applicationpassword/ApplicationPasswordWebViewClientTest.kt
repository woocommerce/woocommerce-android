package com.woocommerce.android.ui.login.sitecredentials.applicationpassword

import android.net.Uri
import android.webkit.WebResourceRequest
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApplicationPasswordWebViewClientTest {
    @Test
    fun `given main frame navigation, when URL is requested, then delegate URL and result`() {
        listOf(true, false).forEach { recoveryHandled ->
            var requestedUrl: String? = null
            val client = ApplicationPasswordWebViewClient {
                requestedUrl = it
                recoveryHandled
            }

            // WHEN
            val intercepted = client.shouldOverrideUrlLoading(
                view = null,
                request = givenRequest(ADMIN_URL, isForMainFrame = true)
            )

            // THEN
            assertThat(intercepted).isEqualTo(recoveryHandled)
            assertThat(requestedUrl).isEqualTo(ADMIN_URL)
        }
    }

    @Test
    fun `given subframe navigation, when URL is requested, then do not invoke recovery`() {
        var recoveryInvoked = false
        val client = ApplicationPasswordWebViewClient {
            recoveryInvoked = true
            true
        }

        // WHEN
        val intercepted = client.shouldOverrideUrlLoading(
            view = null,
            request = givenRequest(ADMIN_URL, isForMainFrame = false)
        )

        // THEN
        assertThat(intercepted).isFalse()
        assertThat(recoveryInvoked).isFalse()
    }

    @Test
    fun `given Jetpack temporary redirect, when URL is requested, then retain shared client handling`() {
        var recoveryInvoked = false
        val client = ApplicationPasswordWebViewClient {
            recoveryInvoked = true
            false
        }

        // WHEN
        val intercepted = client.shouldOverrideUrlLoading(
            view = null,
            request = givenRequest(
                WebViewAuthenticator.JETPACK_SSO_TEMP_REDIRECT_URL,
                isForMainFrame = true
            )
        )

        // THEN
        assertThat(intercepted).isTrue()
        assertThat(recoveryInvoked).isFalse()
    }

    private fun givenRequest(url: String, isForMainFrame: Boolean): WebResourceRequest = mock {
        on { this.url } doReturn Uri.parse(url)
        on { this.isForMainFrame } doReturn isForMainFrame
    }

    private companion object {
        const val ADMIN_URL = "https://site.example/wp-admin/"
    }
}
