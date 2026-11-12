package com.woocommerce.android.ui.woopos.lock

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface WooPosPINClock {
    fun now(): WooPosPINTime
}

class WooPosAndroidPINClock @Inject constructor(@ApplicationContext private val context: Context) : WooPosPINClock {
    override fun now() = WooPosPINTime(
        bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT),
        elapsedMillis = SystemClock.elapsedRealtime(),
    )
}
