package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.ui.analytics.hub.settings.AnalyticCardConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsSettingsDataStore
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsSettingsResourcesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveAnalyticsCardsConfiguration @Inject constructor(
    private val settingsDataStore: AnalyticsSettingsDataStore,
    resourcesRepository: AnalyticsSettingsResourcesRepository
) {
    private val defaultConfiguration = resourcesRepository.getDefaultAnalyticsCardsConfiguration()
    operator fun invoke(): Flow<List<AnalyticCardConfiguration>> =
        settingsDataStore.observeCardsConfiguration().map { currentConfiguration ->
            currentConfiguration ?: defaultConfiguration
        }
}
