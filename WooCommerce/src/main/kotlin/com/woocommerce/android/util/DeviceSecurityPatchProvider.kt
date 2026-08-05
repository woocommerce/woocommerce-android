package com.woocommerce.android.util

import android.os.Build
import javax.inject.Inject

class DeviceSecurityPatchProvider @Inject constructor() {
    /**
     * The device's Android security patch level in `yyyy-MM-dd` form, e.g. `2023-11-05`, or `null`
     * when the device does not report one.
     */
    fun get(): String? = Build.VERSION.SECURITY_PATCH?.takeIf { it.isNotBlank() }
}
