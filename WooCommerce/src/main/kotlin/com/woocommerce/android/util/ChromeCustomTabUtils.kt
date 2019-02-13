package com.woocommerce.android.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.customtabs.CustomTabsClient
import android.support.customtabs.CustomTabsIntent
import android.support.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
import android.support.customtabs.CustomTabsServiceConnection
import android.support.customtabs.CustomTabsSession
import android.support.v4.content.ContextCompat
import com.woocommerce.android.R

/**
 * Simplifies using Chrome Custom Tabs
 *  - Call connect with an optional URL to preload when the activity starts
 *  - Call launchUrl() to actually display the URL
 *  - Call disconnect when the activity ends
 */
object ChromeCustomTabUtils {
    private const val CUSTOM_TAB_PACKAGE_NAME_STABLE = "com.android.chrome"

    private var session: CustomTabsSession? = null
    private var connection: CustomTabsServiceConnection? = null
    private var canUseCustomTabs: Boolean? = null

    fun connect(context: Context, preloadUrl: String? = null): Boolean {
        if (!canUseCustomTabs(context)) {
            return false
        }

        connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0)
                session = client.newSession(null)
                preloadUrl?.let { url ->
                    session?.mayLaunchUrl(Uri.parse(url), null, null)
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
        // if there's no connection then the device doesn't support custom tabs (or the caller neglected to connect)
        if (connection == null) {
            ActivityUtils.openUrlExternal(context, url)
            return
        }

        val intent = CustomTabsIntent.Builder(session)
                .addDefaultShareMenuItem()
                .setToolbarColor(ContextCompat.getColor(context, R.color.wc_purple))
                .setShowTitle(true)
                .build()
        intent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.packageName))
        intent.launchUrl(context, Uri.parse(url))
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
