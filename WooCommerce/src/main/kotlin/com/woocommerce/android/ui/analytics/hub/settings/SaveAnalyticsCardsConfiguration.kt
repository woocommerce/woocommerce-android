package com.woocommerce.android.ui.analytics.hub.settings

import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsSettingsDataStore
import javax.inject.Inject

class SaveAnalyticsCardsConfiguration @Inject constructor(
    private val analyticsSettingsDataStore: AnalyticsSettingsDataStore
) {
    suspend operator fun invoke(cards: List<AnalyticCardConfiguration>) {
        analyticsSettingsDataStore.saveAnalyticsCardsConfiguration(cards.sortedByDescending { it.isVisible })
    }
}
