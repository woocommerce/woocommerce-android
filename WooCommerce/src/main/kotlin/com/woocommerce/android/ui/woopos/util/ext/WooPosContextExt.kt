package com.woocommerce.android.ui.woopos.util.ext

import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "woo_pos_data_store")

fun Context.getScreenWidthDp(): Int {
    val displayMetrics = resources.displayMetrics
    return (displayMetrics.widthPixels / displayMetrics.density).toInt()
}

fun Context.getScreenHeightDp(): Int {
    val displayMetrics = resources.displayMetrics
    return (displayMetrics.heightPixels / displayMetrics.density).toInt()
}

fun Context.getLongestScreenSideDp() = maxOf(getScreenWidthDp(), getScreenHeightDp()).dp

fun Context.announceForAccessibility(text: String) {
    val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    if (manager.isEnabled) {
        val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AccessibilityEvent()
        } else {
            @Suppress("DEPRECATION")
            AccessibilityEvent.obtain()
        }
        event.eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
        event.text.add(text)
        manager.sendAccessibilityEvent(event)
    }
}
