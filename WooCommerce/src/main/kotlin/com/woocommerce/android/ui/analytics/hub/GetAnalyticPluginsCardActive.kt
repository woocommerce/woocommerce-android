package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.google.IsGoogleForWooEnabled
import com.woocommerce.android.util.FeatureFlag
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.GOOGLE_ADS
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.JETPACK
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_GIFT_CARDS
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES

class GetAnalyticPluginsCardActive @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val isGoogleForWooEnabled: IsGoogleForWooEnabled
) {
    private val analyticPlugins = listOf(
        WOO_PRODUCT_BUNDLES,
        WOO_GIFT_CARDS,
        JETPACK
    )

    suspend operator fun invoke(): Set<AnalyticsCards> {
        val isGoogleForWooEnabled = isGoogleForWooEnabled()

        val selectedSite = selectedSite.getOrNull() ?: return emptySet()
        return wooCommerceStore.getSitePlugins(selectedSite, analyticPlugins)
            .filter { pluginModel ->
                pluginModel.isActive || pluginModel.isValidGoogleAdsPlugin(isGoogleForWooEnabled)
            }.mapNotNull { pluginModel ->
                when (pluginModel.name) {
                    WOO_PRODUCT_BUNDLES.pluginName -> AnalyticsCards.Bundles
                    WOO_GIFT_CARDS.pluginName -> AnalyticsCards.GiftCards
                    JETPACK.pluginName -> AnalyticsCards.Session
                    GOOGLE_ADS.pluginName -> AnalyticsCards.GoogleAds
                    else -> null
                }
            }.toSet()
    }

    private fun SitePluginModel.isValidGoogleAdsPlugin(isGoogleForWooEnabled: Boolean) =
        name == GOOGLE_ADS.pluginName &&
            FeatureFlag.GOOGLE_ADS_ANALYTICS_HUB_M1.isEnabled() &&
            isGoogleForWooEnabled
}
