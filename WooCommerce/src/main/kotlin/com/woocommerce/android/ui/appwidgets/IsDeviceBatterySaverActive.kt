package com.woocommerce.android.ui.appwidgets

import android.content.Context
import android.os.PowerManager
import javax.inject.Inject

class IsDeviceBatterySaverActive @Inject constructor() {
    operator fun invoke(
        context: Context
    ): Boolean {
        return context.getSystemService(Context.POWER_SERVICE)
            .run { this as? PowerManager }
            ?.isPowerSaveMode
            ?: false
    }
}
