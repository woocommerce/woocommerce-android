package com.woocommerce.android.ui.woopos.util

import android.content.Context
import android.content.pm.PackageManager
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@Reusable
class WooPosCellularCapabilityDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasCellularCapability(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }
}
