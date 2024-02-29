package com.woocommerce.android.extensions

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import org.wordpress.android.util.DisplayUtils

/**
 * Used for starting the HelpActivity in a wrapped way whenever a troubleshooting URL click happens
 */
fun FragmentActivity.startHelpActivity(origin: HelpOrigin) =
    startActivity(
        HelpActivity.createIntent(
            this,
            origin,
            null
        )
    )

var Activity.currentScreenBrightness: Float
    get() = window.attributes.screenBrightness
    set(value) {
        window.attributes = window.attributes.apply { screenBrightness = value }
    }

fun Activity.isTablet() = DisplayUtils.isTablet(this) || DisplayUtils.isXLargeTablet(this)
