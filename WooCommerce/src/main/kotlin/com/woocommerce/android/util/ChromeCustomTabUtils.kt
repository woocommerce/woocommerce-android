package com.woocommerce.android.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
import androidx.browser.customtabs.CustomTabsService.KEY_URL
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import com.woocommerce.android.R
import org.wordpress.android.util.ToastUtils

/**
 * Simplifies using Chrome Custom Tabs
 *
 *  - Call connect with an optional URL and optional list of other URLs to preload when the activity starts
 *  - Call launchUrl() to actually display the URL
 *  - Call disconnect when the activity stops
 *
 *  OR
 *
 *  - Call launchUrl() by itself to avoid connecting and disconnecting
 *
 *  The latter is recommended when it's not necessary to pre-load any URLs (Google recommends
 *  preloading only when there's at least a 50% chance users will visit the URL). Note that
 *  when passing a list of other likely URLs, they should be ordered in descending priority
 */
object ChromeCustomTabUtils {
    private const val CUSTOM_TAB_PACKAGE_NAME_STABLE = "com.android.chrome"

    private var session: CustomTabsSession? = null
    private var connection: CustomTabsServiceConnection? = null
    private var canUseCustomTabs: Boolean? = null

    fun connect(context: Context, preloadUrl: String? = null, otherLikelyUrls: Array<String>? = null): Boolean {
        if (!canUseCustomTabs(context)) {
            return false
        }

        val thisConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0)
                session = client.newSession(null)

                val otherLikelyBundles = ArrayList<Bundle>()
                otherLikelyUrls?.let { urlList ->
                    for (url in urlList) {
                        val bundle = Bundle()
                        bundle.putParcelable(KEY_URL, Uri.parse(url))
                        otherLikelyBundles.add(bundle)
                    }
                }

                preloadUrl?.let {
                    session?.mayLaunchUrl(Uri.parse(it), null, otherLikelyBundles)
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                session = null
                connection = null
            }
        }
        CustomTabsClient.bindCustomTabsService(context, CUSTOM_TAB_PACKAGE_NAME_STABLE, thisConnection)
        connection = thisConnection

        return true
    }

    fun disconnect(context: Context) {
        if (connection != null) {
            try {
                context.unbindService(connection!!)
            } catch (e: IllegalArgumentException) {
                WooLog.e(WooLog.T.SUPPORT, e)
            }
        }

        session = null
        connection = null
    }

    fun launchUrl(context: Context, url: String) {
        try {
            if (connection == null) {
                if (canUseCustomTabs(context)) {
                    createIntent(context).launchUrl(context, Uri.parse(url))
                } else {
                    ActivityUtils.openUrlExternal(context, url)
                }
            } else {
                createIntent(context, session).launchUrl(context, Uri.parse(url))
            }
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, context.getString(R.string.error_cant_open_url), ToastUtils.Duration.LONG)
            WooLog.e(WooLog.T.UTILS, "No default app available on the device to open the link: $url", e)
        }
    }

    private fun createIntent(context: Context, tabSession: CustomTabsSession? = null): CustomTabsIntent {
        val intent = CustomTabsIntent.Builder(tabSession)
                .setToolbarColor(ContextCompat.getColor(context, R.color.color_surface))
                .addDefaultShareMenuItem()
                .setShowTitle(true)
                .build()
        intent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.packageName))
        return intent
    }

    /**
     * Adapted from https://github.com/GoogleChrome/custom-tabs-client/blob/master/shared/src/main/java/org/
     * chromium/customtabsclient/shared/CustomTabsHelper.java
     */
    private fun canUseCustomTabs(context: Context): Boolean {
        canUseCustomTabs?.let { return it }

        val pm = context.packageManager
        val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
        val resolvedActivityList = pm.queryIntentActivities(activityIntent, 0)
        for (info in resolvedActivityList) {
            val serviceIntent = Intent()
            serviceIntent.action = ACTION_CUSTOM_TABS_CONNECTION
            serviceIntent.setPackage(info.activityInfo.packageName)
            if (pm.resolveService(serviceIntent, 0) != null) {
                canUseCustomTabs = true
                return true
            }
        }

        canUseCustomTabs = false
        return false
    }
}
