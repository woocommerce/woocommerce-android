package com.woocommerce.android.ui.appwidgets

import android.content.Context
import android.os.Build
import android.os.PowerManager

class IsDeviceBatterySaverActive(

) {
    operator fun invoke(
        context: Context
    ): Boolean {
        return context.getSystemService(Context.POWER_SERVICE)
            .run { this as? PowerManager }
            ?.isPowerSaveMode
            ?: false
    }
}
