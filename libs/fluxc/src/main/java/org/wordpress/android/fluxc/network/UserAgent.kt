package org.wordpress.android.fluxc.network

import android.content.Context
import android.webkit.WebSettings
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.utils.PreferenceUtils.getFluxCPreferences
import org.wordpress.android.util.PackageUtils
import kotlin.time.Duration.Companion.seconds

@Suppress("MemberNameEqualsClassName")
class UserAgent(
    private val appContext: Context,
    private val appName: String,
    coroutineScope: CoroutineScope,
    bgDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private var defaultUserAgent: String = System.getProperty("http.agent") ?: ""
    private val appVersionName
        get() = "$appName/${PackageUtils.getVersionName(appContext)}"

    /**
     * User-Agent string when making HTTP connections, for both API traffic and WebViews.
     * Appends "[appName]/version" to WebView's default User-Agent string for the webservers
     * to get the full feature list of the browser and serve content accordingly, e.g.:
     *    "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
     *    AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36
     *    wp-android/4.7"
     */
    val userAgent: String
        get() = "$defaultUserAgent $appVersionName".trim()

    init {
        coroutineScope.launch(bgDispatcher) {
            initUserAgent()
        }
    }

    /**
     * Initializes the User-Agent string.
     * We'll first load the saved User-Agent from SharedPreferences if it exists. Then, we will
     * try to load the default User-Agent from WebSettings after a short delay.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun initUserAgent() {
        var wasCached = false

        // Try to load the default User-Agent from SharedPreferences.
        getFluxCPreferences(appContext).getString(PREF_KEY, "")?.takeIf { it.isNotEmpty() }?.let {
            defaultUserAgent = it
            wasCached = true
        }

        if (wasCached) {
            // Add a short delay.
            // We do this to avoid a potential race condition where the WebView is initialized from multiple threads,
            // that leads to a crash (`AwDataDirLock` error). This issue tends to happen on process launches.
            // Check peaMlT-Tk-p2
            delay(10.seconds)
        }

        try {
            WebSettings.getDefaultUserAgent(appContext).also {
                defaultUserAgent = it

                getFluxCPreferences(appContext).edit()
                    .putString(PREF_KEY, it)
                    .apply()
            }
        } catch (e: Exception) {
            // If we fail to get the default User-Agent, we will use the one we already have.
            // This can happen if the WebView is not initialized yet.
            e.printStackTrace()
        }
    }

    override fun toString(): String = userAgent

    companion object {
        @VisibleForTesting
        internal const val PREF_KEY = "user_agent"
    }
}
