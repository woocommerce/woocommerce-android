package com.woocommerce.android.util

import android.os.Build
import javax.inject.Inject

class DeviceSecurityPatchProvider @Inject constructor() {
    /** The security patch level in `yyyy-MM-dd` form, or `null` when the device reports none. */
    fun get(): String? = Build.VERSION.SECURITY_PATCH?.takeIf { it.isNotBlank() }
}
