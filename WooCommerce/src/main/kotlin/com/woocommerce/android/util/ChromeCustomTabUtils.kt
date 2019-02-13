package com.woocommerce.android.util

import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import com.woocommerce.android.R

object ChromeCustomTabUtils {
    fun viewUrl(context: Context, url: String) {
        CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(context, R.color.wc_purple))
                .setStartAnimations(context, R.anim.activity_slide_in_from_right, 0)
                .setExitAnimations(context, 0, R.anim.activity_slide_out_to_right)
                .setShowTitle(true)
                .build()
                .launchUrl(context, Uri.parse(url))
    }
}
