package com.woocommerce.android.ui.appwidgets

import android.content.Context
import android.os.PowerManager
import javax.inject.Inject

class IsDeviceBatterySaverActive @Inject constructor(
    private val appContext: Context
) {
    operator fun invoke(): Boolean {
        return appContext.getSystemService(Context.POWER_SERVICE)
            .run { this as? PowerManager }
            ?.isPowerSaveMode
            ?: false
    }
}
