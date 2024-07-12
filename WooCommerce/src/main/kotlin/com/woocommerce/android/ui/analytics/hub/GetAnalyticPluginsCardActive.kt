package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class GetAnalyticPluginsCardActive @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {
    private val analyticPlugins = mutableListOf(
        WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES,
        WooCommerceStore.WooPlugin.WOO_GIFT_CARDS,
        WooCommerceStore.WooPlugin.JETPACK
    ).apply {
        if (FeatureFlag.GOOGLE_ADS_ANALYTICS_HUB_M1.isEnabled()) {
            add(WooCommerceStore.WooPlugin.GOOGLE_ADS)
        }
    }.toList()

    suspend operator fun invoke(): Set<AnalyticsCards> {
        val selectedSite = selectedSite.getOrNull() ?: return emptySet()
        return wooCommerceStore.getSitePlugins(selectedSite, analyticPlugins)
            .filter { pluginModel ->
                pluginModel.isActive
            }.mapNotNull { pluginModel ->
                when (pluginModel.name) {
                    WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES.pluginName -> AnalyticsCards.Bundles
                    WooCommerceStore.WooPlugin.WOO_GIFT_CARDS.pluginName -> AnalyticsCards.GiftCards
                    WooCommerceStore.WooPlugin.JETPACK.pluginName -> AnalyticsCards.Session
                    WooCommerceStore.WooPlugin.GOOGLE_ADS.pluginName -> AnalyticsCards.GoogleAds
                    else -> null
                }
            }.toSet()
    }
}
