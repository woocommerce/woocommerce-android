package com.woocommerce.android.tracker

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class TrackStoreSnapshot @Inject constructor(
    private val selectedSite: SelectedSite,
    private val tracker: AnalyticsTrackerWrapper,
    private val appPrefs: AppPrefs,
    private val productStore: WCProductStore,
    private val ordersStore: WCOrderStore,
    private val wooStore: WooCommerceStore,
) {
    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
            val site = selectedSite.getIfExists()
            if (site != null) {
                val wasTrackedForSite = appPrefs.isApplicationStoreSnapshotTrackedForSite(
                    localSiteId = site.id,
                    remoteSiteId = site.siteId,
                    selfHostedSiteId = site.selfHostedSiteId
                )
                if (!wasTrackedForSite) {
                    val (productCounts, ordersCount, sitePlugins) = fetchRequiredData(site)

                    if (anyRequestFailed(productCounts, ordersCount, sitePlugins)) {
                        WooLog.e(WooLog.T.UTILS, "Failed to track store snapshot.")
                    } else {
                        trackEvent(productCounts, ordersCount as WCOrderStore.OrdersCountResult.Success, sitePlugins)

                        appPrefs.setApplicationStoreSnapshotTrackedForSite(
                            localSiteId = site.id, remoteSiteId = site.siteId, selfHostedSiteId = site.selfHostedSiteId
                        )
                    }
                }
            } else {
                WooLog.i(WooLog.T.UTILS, "Site is not selected - not tracking store snapshot.")
            }
        }
    }

    private fun anyRequestFailed(
        productCounts: WooResult<Long>,
        ordersCount: WCOrderStore.OrdersCountResult,
        sitePlugins: WooResult<List<SitePluginModel>>
    ) = productCounts.isError || ordersCount is WCOrderStore.OrdersCountResult.Failure || sitePlugins.isError

    private suspend fun CoroutineScope.fetchRequiredData(site: SiteModel):
        Triple<WooResult<Long>, WCOrderStore.OrdersCountResult, WooResult<List<SitePluginModel>>> {
        val productCountsDeferred = this.async { productStore.fetchProductsCount(site) }
        val ordersCountDeferred = this.async { ordersStore.fetchOrdersCount(site) }
        val sitePluginsDeferred = this.async { wooStore.fetchSitePlugins(site) }

        val productCounts = productCountsDeferred.await()
        val ordersCount = ordersCountDeferred.await()
        val sitePlugins = sitePluginsDeferred.await()
        return Triple(productCounts, ordersCount, sitePlugins)
    }

    private fun trackEvent(
        productCounts: WooResult<Long>,
        ordersCount: WCOrderStore.OrdersCountResult.Success,
        sitePlugins: WooResult<List<SitePluginModel>>
    ) {
        tracker.track(
            AnalyticsEvent.APPLICATION_STORE_SNAPSHOT,
            mapOf(
                KEY_PRODUCTS_COUNT to productCounts.model,
                KEY_ORDERS_COUNT to ordersCount.count,
                KEY_WOOCOMMERCE to sitePlugins.model.getPlugin(KEY_WOOCOMMERCE).getPluginStatus(),
                KEY_STRIPE_PLUGIN to sitePlugins.model.getPlugin(KEY_STRIPE_PLUGIN).getPluginStatus(),
                KEY_SQUARE_PLUGIN to sitePlugins.model.getPlugin(KEY_SQUARE_PLUGIN).getPluginStatus(),
                KEY_PAYPAL_PLUGIN to sitePlugins.model.getPlugin(KEY_PAYPAL_PLUGIN).getPluginStatus(),
            )
        )
    }

    private fun List<SitePluginModel>?.getPlugin(type: String) = this?.firstOrNull {
        it.name.endsWith(type)
    }

    private fun SitePluginModel?.getPluginStatus() = when (this) {
        null -> VALUE_PLUGIN_NOT_INSTALLED
        else -> when {
            isActive -> VALUE_PLUGIN_INSTALLED_AND_ACTIVATED
            else -> VALUE_PLUGIN_INSTALLED_AND_NOT_ACTIVATED
        }
    }

    private companion object {
        private const val KEY_PRODUCTS_COUNT = "products_count"
        private const val KEY_ORDERS_COUNT = "orders_count"

        private const val KEY_WOOCOMMERCE = "woocommerce-payments"
        private const val KEY_STRIPE_PLUGIN = "woocommerce-gateway-stripe"
        private const val KEY_SQUARE_PLUGIN = "woocommerce-square"
        private const val KEY_PAYPAL_PLUGIN = "woocommerce-gateway-paypal-express-checkout"

        private const val VALUE_PLUGIN_NOT_INSTALLED = "not_installed"
        private const val VALUE_PLUGIN_INSTALLED_AND_NOT_ACTIVATED = "installed_and_not_activated"
        private const val VALUE_PLUGIN_INSTALLED_AND_ACTIVATED = "installed_and_activated"
    }
}
