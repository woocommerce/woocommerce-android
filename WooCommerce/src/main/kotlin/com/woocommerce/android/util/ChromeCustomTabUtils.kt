package com.woocommerce.android.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.customtabs.CustomTabsClient
import android.support.customtabs.CustomTabsIntent
import android.support.customtabs.CustomTabsServiceConnection
import android.support.customtabs.CustomTabsSession
import android.support.v4.content.ContextCompat
import com.woocommerce.android.R

object ChromeCustomTabUtils {
    private const val CUSTOM_TAB_PACKAGE_NAME_STABLE = "com.android.chrome"
    private var session: CustomTabsSession? = null

    fun viewUrl(context: Context, url: String) {
        val intent = CustomTabsIntent.Builder(session)
                .setToolbarColor(ContextCompat.getColor(context, R.color.wc_purple))
                .setStartAnimations(context, R.anim.activity_slide_in_from_right, 0)
                .setExitAnimations(context, 0, R.anim.activity_slide_out_to_right)
                .setShowTitle(true)
                .build()
        intent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.getPackageName()))
        intent.launchUrl(context, Uri.parse(url))
    }

    fun preload(context: Context, url: String) {
        // use existing session if available
        session?.let {
            it.mayLaunchUrl(Uri.parse(url), null, null)
            return
        }

        val connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0)
                session = client.newSession(null)
                session?.mayLaunchUrl(Uri.parse(url), null, null)
            }
            override fun onServiceDisconnected(name: ComponentName) {
                session = null
            }
        }
        CustomTabsClient.bindCustomTabsService(context, CUSTOM_TAB_PACKAGE_NAME_STABLE, connection)
    }

    fun disconnect(context: Context) {
        session = null
    }
}
