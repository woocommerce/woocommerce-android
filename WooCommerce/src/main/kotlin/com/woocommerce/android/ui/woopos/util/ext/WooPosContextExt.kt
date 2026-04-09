package com.woocommerce.android.ui.woopos.util.ext

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
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

fun Context.isWooPosPhoneLayout(): Boolean {
    val shortSide = minOf(getScreenWidthDp(), getScreenHeightDp())
    return shortSide < PHONE_MAX_SHORT_SIDE_DP
}

@Composable
fun isWooPosPhoneLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return minOf(configuration.screenWidthDp, configuration.screenHeightDp) < PHONE_MAX_SHORT_SIDE_DP
}

private const val PHONE_MAX_SHORT_SIDE_DP = 674
