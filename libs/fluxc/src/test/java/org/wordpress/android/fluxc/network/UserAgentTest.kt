package org.wordpress.android.fluxc.network

import android.webkit.WebSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.utils.PreferenceUtils.getFluxCPreferences
import org.wordpress.android.util.PackageUtils
import kotlin.test.assertEquals

private const val APP_NAME = "App Name"
private const val USER_AGENT = "Default User Agent"
private const val SYSTEM_USER_AGENT = "System User Agent"
private const val CACHED_USER_AGENT = "Cached User Agent"
private const val APP_VERSION = "1.0"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UserAgentTest {
    private val context = RuntimeEnvironment.getApplication().applicationContext
    private val scheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(scheduler)
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        System.setProperty("http.agent", SYSTEM_USER_AGENT)
    }

    @Test
    fun `when cached user agent exists, then it should be loaded from SharedPreferences`() = runTest(testDispatcher) {
        withMockedPackageUtils {
            withCachedValue(cachedUserAgent = CACHED_USER_AGENT) {
                val result = UserAgent(context, APP_NAME, testScope.backgroundScope, testDispatcher)

                runCurrent()

                // Verify the cached user agent is used
                assertEquals("$CACHED_USER_AGENT $APP_NAME/$APP_VERSION", result.userAgent)
            }
        }
    }

    @Test
    fun `when WebSettings returns user agent, then it should be saved to SharedPreferences`() = runTest(testDispatcher) {
        withMockedPackageUtils {
            withCachedValue {
                mockStatic(WebSettings::class.java).use {
                    whenever(WebSettings.getDefaultUserAgent(context)).thenReturn(USER_AGENT)

                    UserAgent(context, APP_NAME, testScope.backgroundScope, testDispatcher)

                    // Advance time to read the WebView user agent
                    advanceTimeBy(100)
                    runCurrent()

                    // Verify the user agent is saved to SharedPreferences
                    assertEquals(USER_AGENT, getFluxCPreferences(context).getString(UserAgent.PREF_KEY, null))
                }
            }
        }
    }

    @Test
    fun `when no cached user agent and WebSettings fails, then it should use system property`() = runTest(testDispatcher) {
        withMockedPackageUtils {
            withCachedValue {
                mockStatic(WebSettings::class.java).use {
                    whenever(WebSettings.getDefaultUserAgent(context)).thenThrow(RuntimeException(""))

                    val result = UserAgent(context, APP_NAME, testScope.backgroundScope, testDispatcher)

                    // Advance time to read the WebView user agent
                    advanceTimeBy(100)
                    runCurrent()

                    // Verify the system user agent is used
                    assertEquals("$SYSTEM_USER_AGENT $APP_NAME/$APP_VERSION", result.userAgent)
                }
            }
        }
    }

    @Test
    fun `when cached user agent exists, then WebSettings should still be called after delay`() = runTest(testDispatcher) {
        withMockedPackageUtils {
            withCachedValue(cachedUserAgent = CACHED_USER_AGENT) {
                mockStatic(WebSettings::class.java).use {
                    whenever(WebSettings.getDefaultUserAgent(context)).thenReturn(USER_AGENT)
                    val result = UserAgent(context, APP_NAME, testScope.backgroundScope, testDispatcher)

                    // Initially it should use the cached value
                    runCurrent()
                    assertEquals("$CACHED_USER_AGENT $APP_NAME/$APP_VERSION", result.userAgent)

                    // After delay, it should update with the new value from WebSettings
                    advanceTimeBy(15000)
                    runCurrent()

                    assertEquals("$USER_AGENT $APP_NAME/$APP_VERSION", result.userAgent)
                    // Verify the user agent is saved to SharedPreferences
                    assertEquals(USER_AGENT, getFluxCPreferences(context).getString(UserAgent.PREF_KEY, null))
                }
            }
        }
    }

    private fun withMockedPackageUtils(test: () -> Unit) {
        mockStatic(PackageUtils::class.java).use { _ ->
            whenever(PackageUtils.getVersionName(context)).thenReturn(APP_VERSION)
            test()
        }
    }

    private fun withCachedValue(
        cachedUserAgent: String? = null,
        test: () -> Unit
    ) {
        getFluxCPreferences(context).edit()
            .apply {
                if (cachedUserAgent != null) {
                    putString(UserAgent.PREF_KEY, cachedUserAgent)
                } else {
                    remove(UserAgent.PREF_KEY)
                }
            }
            .apply()
        test()
    }
}
