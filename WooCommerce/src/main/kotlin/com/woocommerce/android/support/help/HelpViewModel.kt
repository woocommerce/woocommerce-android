package com.woocommerce.android.support.help

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class HelpViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) : ScopedViewModel(savedState) {
    fun onContactPaymentsSupportClicked() {
        triggerEvent(ContactPaymentsSupportClickEvent.ShowLoading)
        launch {
            wooStore.fetchSitePlugins(selectedSite.get())
            val fetchSitePluginsResult = wooStore.fetchSitePlugins(selectedSite.get())
            if (fetchSitePluginsResult.isError) {
                triggerEvent(ContactPaymentsSupportClickEvent.CreateTicket(listOf(SITE_PLUGINS_FETCHING_ERROR_TAG)))
                return@launch
            }
            val wcPayPluginInfo = wooStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_PAYMENTS)

            val tags = mutableListOf(
                if (wcPayPluginInfo == null) {
                    WCPAY_NOT_INSTALLED_TAG
                } else if (wcPayPluginInfo.isActive) {
                    WCPAY_INSTALLED_AND_ACTIVATED
                } else {
                    WCPAY_INSTALLED
                }
            )
            triggerEvent(ContactPaymentsSupportClickEvent.CreateTicket(tags))
        }
    }

    sealed class ContactPaymentsSupportClickEvent : MultiLiveEvent.Event() {
        object ShowLoading : ContactPaymentsSupportClickEvent()
        data class CreateTicket(val supportTags: List<String>) : ContactPaymentsSupportClickEvent()
    }

    private companion object {
        private const val WCPAY_NOT_INSTALLED_TAG = "wcpay_not_installed"
        private const val WCPAY_INSTALLED = "wcpay_installed"
        private const val WCPAY_INSTALLED_AND_ACTIVATED = "wcpay_installed_and_activated"

        private const val SITE_PLUGINS_FETCHING_ERROR_TAG = "site_plugins_fetching_error"
    }
}
