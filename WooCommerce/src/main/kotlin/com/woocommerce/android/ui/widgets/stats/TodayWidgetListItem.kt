package com.woocommerce.android.ui.widgets.stats

import androidx.annotation.LayoutRes

data class TodayWidgetListItem(
    @LayoutRes val layout: Int,
    val localSiteId: Int,
    val key: String,
    val value: String
)
