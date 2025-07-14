package com.woocommerce.android.ui.prefs.plugins

import androidx.annotation.ColorRes

sealed interface PluginsViewState {
    data object Loading : PluginsViewState
    data object Error : PluginsViewState
    data class Loaded(
        val plugins: List<Plugin> = emptyList()
    ) : PluginsViewState {
        data class Plugin(
            val name: String,
            val authorName: String?,
            val version: String,
            val status: PluginStatus
        ) {
            sealed class PluginStatus {
                data class UpToDate(val title: String, @ColorRes val color: Int) : PluginStatus()
                data class UpdateAvailable(val title: String, @ColorRes val color: Int) : PluginStatus()
                data class Inactive(val title: String, @ColorRes val color: Int) : PluginStatus()
                data object Unknown : PluginStatus()
            }
        }
    }
}
