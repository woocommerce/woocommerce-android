package com.woocommerce.android.ui.prefs.plugins

import com.woocommerce.android.viewmodel.MultiLiveEvent

sealed class PluginsEvent : MultiLiveEvent.Event() {
    data class NavigateToPluginsWeb(val url: String) : PluginsEvent()
}
