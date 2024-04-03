package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsSettingsDataStore
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsSettingsResourcesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveAnalyticsCardsConfiguration @Inject constructor(
    private val settingsDataStore: AnalyticsSettingsDataStore,
    private val resourcesRepository: AnalyticsSettingsResourcesRepository,
    private val getAnalyticPluginsCardActive: GetAnalyticPluginsCardActive
) {
    suspend operator fun invoke(): Flow<List<AnalyticCardConfiguration>> {
        val activePluginCards = getAnalyticPluginsCardActive()
        return settingsDataStore.observeCardsConfiguration().map { currentConfiguration ->
            currentConfiguration?.map { cardConfiguration ->
                if (cardConfiguration.card.isPlugin && (cardConfiguration.card in activePluginCards).not()) {
                    cardConfiguration.copy(isVisible = false)
                } else {
                    cardConfiguration
                }
            } ?: resourcesRepository.getDefaultAnalyticsCardsConfiguration()
        }
    }
}
