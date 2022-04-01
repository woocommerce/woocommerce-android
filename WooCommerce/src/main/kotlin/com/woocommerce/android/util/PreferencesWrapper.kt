package com.woocommerce.android.util

import android.content.Context
import androidx.preference.PreferenceManager
import javax.inject.Inject

class PreferencesWrapper @Inject constructor(val context: Context) {
    val sharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)
}
