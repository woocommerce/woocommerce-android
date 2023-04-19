package com.woocommerce.android.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.Builder
import androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
import androidx.browser.customtabs.CustomTabsService.KEY_URL
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.woocommerce.android.R
import com.woocommerce.android.extensions.intentActivities
import com.woocommerce.android.extensions.physicalScreenHeightInPx
import com.woocommerce.android.extensions.service
import com.woocommerce.android.util.ChromeCustomTabUtils.Height.Full
import com.woocommerce.android.util.ChromeCustomTabUtils.Height.Partial
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
    private var activityResultLauncher: ActivityResultLauncher<String>? = null
    private var partialHeightToUse: Partial? = null

    fun connectAndStartSession(context: Context, preloadUrl: String? = null, otherLikelyUrls: Array<String>? = null) {
        if (!canUseCustomTabs(context)) return

        val connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0)
                session = client.newSession(null)

                val otherLikelyBundles = otherLikelyUrls?.map { otherLikelyUrl ->
                    Bundle().apply { putParcelable(KEY_URL, Uri.parse(otherLikelyUrl)) }
                }

                preloadUrl.let {
                    session?.mayLaunchUrl(Uri.parse(it), null, otherLikelyBundles)
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                session = null
                connection = null
            }
        }
        CustomTabsClient.bindCustomTabsService(context, CUSTOM_TAB_PACKAGE_NAME_STABLE, connection)
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

    fun registerForPartialTabUsage(activity: FragmentActivity) {
        activityResultLauncher = activity.registerForActivityResult(
            object : ActivityResultContract<String, Int>() {
                override fun createIntent(context: Context, input: String) =
                    createIntent(context, partialHeightToUse!!).intent.apply {
                        data = Uri.parse(input)
                    }

                override fun parseResult(resultCode: Int, intent: Intent?) = resultCode
            }) {}
    }

    fun launchUrl(context: Context, url: String, height: Height = Height.Full) {
        try {
            if (canUseCustomTabs(context)) {
                if (session == null && height is Height.Partial && activityResultLauncher != null) {
                    partialHeightToUse = height
                    activityResultLauncher?.launch(url)
                } else {
                    createIntent(context, height, session).launchUrl(context, Uri.parse(url))
                }
            } else {
                ActivityUtils.openUrlExternal(context, url)
            }
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, context.getString(R.string.error_cant_open_url), ToastUtils.Duration.LONG)
            WooLog.e(WooLog.T.UTILS, "No default app available on the device to open the link: $url", e)
        }
    }

    private fun createIntent(
        context: Context,
        height: Height,
        tabSession: CustomTabsSession? = null
    ): CustomTabsIntent {
        val defaultColorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.color_toolbar))
            .build()
        val intent = Builder(tabSession)
            .setDefaultColorSchemeParams(defaultColorSchemeParams)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .setShowTitle(true)
            .setTabHeight(height, context)
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
        val resolvedActivityList = pm.intentActivities(activityIntent, 0)
        for (info in resolvedActivityList) {
            val serviceIntent = Intent()
            serviceIntent.action = ACTION_CUSTOM_TABS_CONNECTION
            serviceIntent.setPackage(info.activityInfo.packageName)
            if (pm.service(serviceIntent, 0) != null) {
                canUseCustomTabs = true
                return true
            }
        }

        canUseCustomTabs = false
        return false
    }

    private fun Builder.setTabHeight(height: Height, context: Context) =
        when (height) {
            Full -> this
            is Partial -> setInitialActivityHeightPx(height.toPx(context))
        }

    sealed class Height {
        object Full : Height()
        sealed class Partial : Height() {
            object Half : Partial()
            object Third : Partial()
            object ThreeQuarters : Partial()
        }
    }

    @Suppress("MagicNumber")
    private fun Partial.toPx(context: Context) =
        when (this) {
            is Partial.Half -> context.physicalScreenHeightInPx / 2
            is Partial.Third -> context.physicalScreenHeightInPx / 3
            is Partial.ThreeQuarters -> context.physicalScreenHeightInPx * 3 / 4
        }
}
