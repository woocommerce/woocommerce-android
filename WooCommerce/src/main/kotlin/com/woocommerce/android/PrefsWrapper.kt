package com.woocommerce.android

import javax.inject.Inject

class PrefsWrapper @Inject constructor(private val prefs: AppPrefs) {
    var isTrackingExtensionAvailable: Boolean
        get() = prefs.isTrackingExtensionAvailable()
        set(value) {
            prefs.setTrackingExtensionAvailable(value)
        }
}
