package com.woocommerce.android.extensions

import androidx.fragment.app.FragmentActivity
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin

/**
 * Used for starting the HelpActivity in a wrapped way whenever a troubleshooting URL click happens
 */
fun FragmentActivity.startHelpActivity(origin: Origin) =
    startActivity(
        HelpActivity.createIntent(
            this,
            origin,
            null
        )
    )
