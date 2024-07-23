package com.woocommerce.android.ui.woopos.util.ext

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pos_preferences")

fun Context.getScreenWidthDp(): Int {
    val displayMetrics = resources.displayMetrics
    return (displayMetrics.widthPixels / displayMetrics.density).toInt()
}

fun Context.getScreenHeightDp(): Int {
    val displayMetrics = resources.displayMetrics
    return (displayMetrics.heightPixels / displayMetrics.density).toInt()
}

fun Context.getLongestScreenSideDp() = maxOf(getScreenWidthDp(), getScreenHeightDp()).dp
