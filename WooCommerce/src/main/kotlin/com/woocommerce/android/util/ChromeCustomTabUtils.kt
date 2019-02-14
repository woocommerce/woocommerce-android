package com.woocommerce.android.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsClient
import android.support.customtabs.CustomTabsIntent
import android.support.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
import android.support.customtabs.CustomTabsService.KEY_URL
import android.support.customtabs.CustomTabsServiceConnection
import android.support.customtabs.CustomTabsSession
import android.support.v4.content.ContextCompat
import com.woocommerce.android.R

/**
 * Simplifies using Chrome Custom Tabs
 *
 *  - Call connect with an optional URL or list of URLs to preload when the activity starts
 *  - Call launchUrl() to actually display the URL
 *  - Call disconnect when the activity stops
 *
 *  OR
 *
 *  - Call launchUrl() by itself to avoid connecting and disconnecting
 *
 *  The latter is recommended when it's not necessary to pre-load the URL (Google recommends
 *  preloading only when there's at least a 50% chance users will visit the URL). Note that
 *  when passing a list of URLs, they should be ordered in descending order of priority
 */
object ChromeCustomTabUtils {
    private const val CUSTOM_TAB_PACKAGE_NAME_STABLE = "com.android.chrome"

    private var session: CustomTabsSession? = null
    private var connection: CustomTabsServiceConnection? = null
    private var canUseCustomTabs: Boolean? = null

    fun connect(context: Context, preloadUrl: String? = null): Boolean {
        val preloadUrlList = ArrayList<String>()
        preloadUrl?.let { url ->
            preloadUrlList.add(url)
        }
        return ChromeCustomTabUtils.connect(context, preloadUrlList)
    }

    fun connect(context: Context, preloadUrlArray: Array<String>): Boolean {
        val preloadUrlList = ArrayList<String>()
        for (url in preloadUrlArray) {
            preloadUrlList.add(url)
        }
        return connect(context, preloadUrlList)
    }

    fun connect(context: Context, preloadUrlList: ArrayList<String>): Boolean {
        if (!canUseCustomTabs(context)) {
            return false
        }

        connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0)
                session = client.newSession(null)
                if (preloadUrlList.size > 0) {
                    val firstPreloadUrl = preloadUrlList.removeAt(0)
                    val otherLikelyBundles = ArrayList<Bundle>()
                    for (url in preloadUrlList) {
                        val bundle = Bundle()
                        bundle.putParcelable(KEY_URL, Uri.parse(url))
                        otherLikelyBundles.add(bundle)
                    }
                    session?.mayLaunchUrl(Uri.parse(firstPreloadUrl), null, otherLikelyBundles)
                }
            }
            override fun onServiceDisconnected(name: ComponentName) {
                session = null
                connection = null
            }
        }
        CustomTabsClient.bindCustomTabsService(context, CUSTOM_TAB_PACKAGE_NAME_STABLE, connection)
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
        if (connection == null) {
            if (canUseCustomTabs(context)) {
                createIntent(context).launchUrl(context, Uri.parse(url))
            } else {
                ActivityUtils.openUrlExternal(context, url)
            }
        } else {
            createIntent(context, session).launchUrl(context, Uri.parse(url))
        }
    }

    private fun createIntent(context: Context, tabSession: CustomTabsSession? = null): CustomTabsIntent {
        val intent = CustomTabsIntent.Builder(tabSession)
                .setToolbarColor(ContextCompat.getColor(context, R.color.wc_purple))
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
