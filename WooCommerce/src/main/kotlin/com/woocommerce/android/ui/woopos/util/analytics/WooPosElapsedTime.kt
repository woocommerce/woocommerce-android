package com.woocommerce.android.ui.woopos.util.analytics

import android.os.SystemClock
import javax.inject.Inject

class WooPosElapsedTime @Inject constructor() {
    fun milliseconds(): Long = SystemClock.elapsedRealtime()
}
