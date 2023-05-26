package com.woocommerce.android.extensions

import androidx.fragment.app.FragmentActivity
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin

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
