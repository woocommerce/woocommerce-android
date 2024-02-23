package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.R
import com.woocommerce.android.ui.analytics.hub.settings.AnalyticCardConfiguration
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class AnalyticsSettingsResourcesRepository @Inject constructor(private val resourceProvider: ResourceProvider) {
    private val cardsTitleResourcesIds = listOf(
        R.string.analytics_revenue_card_title,
        R.string.analytics_orders_card_title,
        R.string.analytics_products_card_title,
        R.string.analytics_session_card_title
    )
    fun getDefaultAnalyticsCardsConfiguration() = cardsTitleResourcesIds.mapIndexed { i, res ->
        AnalyticCardConfiguration(
            id = i,
            title = resourceProvider.getString(res),
            isVisible = true
        )
    }
}
