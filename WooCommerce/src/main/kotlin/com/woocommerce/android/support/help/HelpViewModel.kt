package com.woocommerce.android.support.help

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.support.TicketType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin
import javax.inject.Inject

@HiltViewModel
class HelpViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) : ScopedViewModel(savedState) {

    fun contactSupport(ticketType: TicketType) {
        if (!selectedSite.exists()) {
            triggerEvent(ContactPaymentsSupportClickEvent.CreateTicket(ticketType, emptyList()))
            return
        }

        triggerEvent(ContactPaymentsSupportClickEvent.ShowLoading)
        launch {
            wooStore.fetchSitePlugins(selectedSite.get())
            val fetchSitePluginsResult = wooStore.fetchSitePlugins(selectedSite.get())
            val tags = if (fetchSitePluginsResult.isError) {
                listOf(SITE_PLUGINS_FETCHING_ERROR_TAG)
            } else {
                val wcPayPluginInfo = wooStore.getSitePlugin(selectedSite.get(), WooPlugin.WOO_PAYMENTS)
                val stripePluginInfo = wooStore.getSitePlugin(selectedSite.get(), WooPlugin.WOO_STRIPE_GATEWAY)

                listOf(determineWcPayTag(wcPayPluginInfo), determineStripeTag(stripePluginInfo))
            }
            triggerEvent(ContactPaymentsSupportClickEvent.CreateTicket(ticketType, tags))
        }
    }

    private fun determineWcPayTag(wcPayPluginInfo: SitePluginModel?) =
        if (wcPayPluginInfo == null) {
            WCPAY_NOT_INSTALLED_TAG
        } else if (wcPayPluginInfo.isActive) {
            WCPAY_INSTALLED_AND_ACTIVATED
        } else {
            WCPAY_INSTALLED
        }

    private fun determineStripeTag(stripePluginInfo: SitePluginModel?) =
        if (stripePluginInfo == null) {
            STRIPE_NOT_INSTALLED_TAG
        } else if (stripePluginInfo.isActive) {
            STRIPE_INSTALLED_AND_ACTIVATED
        } else {
            STRIPE_INSTALLED
        }

    sealed class ContactPaymentsSupportClickEvent : MultiLiveEvent.Event() {
        object ShowLoading : ContactPaymentsSupportClickEvent()
        data class CreateTicket(
            val ticketType: TicketType,
            val supportTags: List<String>,
        ) : ContactPaymentsSupportClickEvent()
    }

    private companion object {
        private const val WCPAY_NOT_INSTALLED_TAG = "woo_mobile_wcpay_not_installed"
        private const val WCPAY_INSTALLED = "woo_mobile_wcpay_installed_and_not_activated"
        private const val WCPAY_INSTALLED_AND_ACTIVATED = "woo_mobile_wcpay_installed_and_activated"

        private const val STRIPE_NOT_INSTALLED_TAG = "woo_mobile_stripe_not_installed"
        private const val STRIPE_INSTALLED = "woo_mobile_stripe_installed_and_not_activated"
        private const val STRIPE_INSTALLED_AND_ACTIVATED = "woo_mobile_stripe_installed_and_activated"

        private const val SITE_PLUGINS_FETCHING_ERROR_TAG = "woo_mobile_site_plugins_fetching_error"
    }
}
