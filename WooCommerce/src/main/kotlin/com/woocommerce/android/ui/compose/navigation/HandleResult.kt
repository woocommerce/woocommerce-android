package com.woocommerce.android.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.navigation.findNavController

@Composable
fun <T> HandleResult(key: String, entryId: Int? = null, handler: (T) -> Unit) {
    val view = LocalView.current
    LaunchedEffect(key1 = key, key2 = view) {
        val entry = if (entryId != null) {
            view.findNavController().getBackStackEntry(entryId)
        } else {
            view.findNavController().currentBackStackEntry
        }

        entry?.savedStateHandle?.let { saveState ->
            saveState.getStateFlow<T?>(key, null).collect {
                it?.let {
                    handler(it)
                    saveState.set(key, null)
                }
            }
        }
    }
}
