package com.woocommerce.android.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.view.WindowManager
import com.woocommerce.android.R
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.util.ToastUtils

// TODO Duplicating methods from WordPress' WPActivityUtils
object ActivityUtils {
    fun isEmailClientAvailable(context: Context?): Boolean {
        if (context == null) {
            return false
        }

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        val packageManager = context.packageManager
        val emailApps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        return !emailApps.isEmpty()
    }

    fun openEmailClient(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openUrlExternal(context: Context, url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, context.getString(R.string.error_cant_open_url), ToastUtils.Duration.LONG)
            WooLog.e(T.UTILS, "No default app available on the device to open the link: $url", e)
        } catch (se: SecurityException) {
            WooLog.e(T.UTILS, "Error opening url in default browser. Url: $url", se)

            val infos = context.packageManager.queryIntentActivities(intent, 0)
            if (infos.size == 1) {
                // there's only one handler and apparently it caused the exception so, just inform and bail
                WooLog.d(T.UTILS, "Only one url handler found so, bailing.")
                ToastUtils.showToast(context, context.getString(R.string.error_cant_open_url))
            } else {
                val chooser = Intent.createChooser(intent, context.getString(R.string.error_please_choose_browser))
                context.startActivity(chooser)
            }
        }
    }

    fun setStatusBarColor(activity : Activity, @ColorRes colorRes : Int) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val window = activity.window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(activity, colorRes)
        }
    }
}
