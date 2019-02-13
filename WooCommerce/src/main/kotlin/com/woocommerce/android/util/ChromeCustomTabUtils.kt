package com.woocommerce.android.util

import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import com.woocommerce.android.R

object ChromeCustomTabUtils {
    fun viewUrl(context: Context, url: String) {
        val builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(ContextCompat.getColor(context, R.color.wc_purple))
        builder.setStartAnimations(context, R.anim.activity_slide_in_from_right, 0)
        builder.setExitAnimations(context, 0, R.anim.activity_slide_out_to_right)

        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}
