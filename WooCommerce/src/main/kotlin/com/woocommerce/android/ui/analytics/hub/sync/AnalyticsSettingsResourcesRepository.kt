package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class AnalyticsSettingsResourcesRepository @Inject constructor(private val resourceProvider: ResourceProvider) {
    fun getDefaultAnalyticsCardsConfiguration() = AnalyticsCards.entries.map { card ->
        AnalyticCardConfiguration(
            card = card,
            title = resourceProvider.getString(card.resId),
            isVisible = true
        )
    }
}
