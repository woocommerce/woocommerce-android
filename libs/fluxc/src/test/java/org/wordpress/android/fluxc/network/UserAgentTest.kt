package org.wordpress.android.fluxc.network

import android.content.Context
import android.webkit.WebSettings
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.util.PackageUtils
import kotlin.test.assertEquals

private const val APP_NAME = "App Name"
private const val USER_AGENT = "Default User Agent"
private const val SYSTEM_USER_AGENT = "System User Agent"
private const val APP_VERSION = "1.0"

@RunWith(RobolectricTestRunner::class)
class UserAgentTest {
    @Before
    fun setup() {
        System.setProperty("http.agent", SYSTEM_USER_AGENT)
    }

    @Test
    fun `when UserAgent is created, then apiUserAgent should use system property with app version`() {
        withMockedPackageUtils {
            val userAgent = UserAgent(ApplicationProvider.getApplicationContext(), APP_NAME)

            assertEquals("$SYSTEM_USER_AGENT $APP_NAME/$APP_VERSION", userAgent.apiUserAgent)
        }
    }

    @Test
    fun `when UserAgent is created, then webViewUserAgent should use WebSettings with app version`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        withMockedPackageUtils {
            mockStatic(WebSettings::class.java).use {
                whenever(WebSettings.getDefaultUserAgent(context)).thenReturn(USER_AGENT)

                val userAgent = UserAgent(context, APP_NAME)

                assertEquals("$USER_AGENT $APP_NAME/$APP_VERSION", userAgent.webViewUserAgent)
            }
        }
    }

    @Test
    fun `when system property is null, then apiUserAgent should use empty string with app version`() {
        System.clearProperty("http.agent")

        withMockedPackageUtils {
            val userAgent = UserAgent(ApplicationProvider.getApplicationContext(), APP_NAME)

            assertEquals("$APP_NAME/$APP_VERSION", userAgent.apiUserAgent)
        }
    }

    @Test
    fun `when WebSettings throws exception, then webViewUserAgent should throw exception`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        withMockedPackageUtils {
            mockStatic(WebSettings::class.java).use {
                whenever(WebSettings.getDefaultUserAgent(context)).thenThrow(RuntimeException("WebView not available"))

                val userAgent = UserAgent(context, APP_NAME)

                assertEquals("$APP_NAME/$APP_VERSION", userAgent.webViewUserAgent)
            }
        }
    }

    private fun withMockedPackageUtils(test: () -> Unit) {
        mockStatic(PackageUtils::class.java).use { _ ->
            whenever(PackageUtils.getVersionName(ApplicationProvider.getApplicationContext())).thenReturn(APP_VERSION)
            test()
        }
    }
}
