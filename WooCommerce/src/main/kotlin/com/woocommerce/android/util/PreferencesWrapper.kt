package com.woocommerce.android.util

import android.content.Context
import androidx.preference.PreferenceManager
import javax.inject.Inject

class PreferencesWrapper @Inject constructor(val context: Context) {
    companion object {
        const val WPCOM_PUSH_DEVICE_TOKEN = "WC_PREF_NOTIFICATIONS_TOKEN"
    }

    val sharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)
}
