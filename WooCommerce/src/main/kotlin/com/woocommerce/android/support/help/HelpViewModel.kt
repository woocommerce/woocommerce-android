package com.woocommerce.android.support.help

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.formatResult
import com.woocommerce.android.support.zendesk.ZendeskTicketType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLogWrapper
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
    private val wooLogWrapper: WooLogWrapper,
) : ScopedViewModel(savedState) {
    var ssr: String? = null
        private set

    init {
        if (selectedSite.exists()) {
            fetchSSR()
        }
    }

    private fun fetchSSR() {
        launch {
            wooLogWrapper.i(WooLog.T.SUPPORT, "Fetching SSR")
            val result = wooStore.fetchSSR(selectedSite.get())
            if (result.isError) {
                wooLogWrapper.e(WooLog.T.SUPPORT, "Error fetching SSR")
            } else {
                wooLogWrapper.i(WooLog.T.SUPPORT, "SSR fetched successfully")
            }
            ssr = result.model?.formatResult()
        }
    }

    fun contactSupport(ticketType: ZendeskTicketType) {
        if (!selectedSite.exists()) {
            triggerEvent(ContactSupportEvent.CreateTicket(ticketType, emptyList()))
            return
        }

        triggerEvent(ContactSupportEvent.ShowLoading)
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
            triggerEvent(ContactSupportEvent.CreateTicket(ticketType, tags))
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

    sealed class ContactSupportEvent : MultiLiveEvent.Event() {
        object ShowLoading : ContactSupportEvent()
        data class CreateTicket(
            val ticketType: ZendeskTicketType,
            val supportTags: List<String>,
        ) : ContactSupportEvent()
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
