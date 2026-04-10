package com.woocommerce.android.ui.woopos.util.ext

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlin.math.min

private const val WOO_POS_MIN_TABLET_SHORT_SIZE_DP = 674

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "woo_pos_data_store")

fun Context.isWooPosPhoneLayout(): Boolean {
    val shortSize = min(getScreenWidthDp(), getScreenHeightDp())
    return shortSize < WOO_POS_MIN_TABLET_SHORT_SIZE_DP
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
