package com.woocommerce.android.ui.woopos.util.ext

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.woocommerce.android.ui.woopos.WooPosIsScreenSizeAllowed
import kotlin.math.max
import kotlin.math.min

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "woo_pos_data_store")

fun Context.isWooPosPhoneLayout(): Boolean {
    val shortSize = min(getScreenWidthDp(), getScreenHeightDp()).dp
    val longSize = max(getScreenWidthDp(), getScreenHeightDp()).dp
    return !WooPosIsScreenSizeAllowed.isTabletSize(shortSize, longSize)
}

fun Context.getScreenWidthDp(): Int {
    val displayMetrics = resources.displayMetrics
    return (displayMetrics.widthPixels / displayMetrics.density).toInt()
}

fun Context.getScreenHeightDp(): Int {
    val displayMetrics = resources.displayMetrics
    return (displayMetrics.heightPixels / displayMetrics.density).toInt()
}

fun Context.getLongestScreenSideDp() = maxOf(getScreenWidthDp(), getScreenHeightDp()).dp
