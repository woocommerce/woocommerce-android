package com.woocommerce.android.ui.login.storecreation.onboarding.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_PAYMENTS
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class GetPaidViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    private val wpAdminSetupUrl = selectedSite.get().url.slashJoin("/wp-admin")
    private val pluginSetupUrl = selectedSite.get().url
        .slashJoin("/wp-admin/admin.php?page=wc-admin&task=payments")
    private val pluginPurchaseUrl = selectedSite.get().url
        .slashJoin("/wp-admin/admin.php?page=wc-settings&tab=checkout")

    private val _viewState = MutableStateFlow(ViewState(wpAdminSetupUrl, true))
    val viewState = _viewState.asLiveData()

    private val webViewUrl: Deferred<String>

    init {
        webViewUrl = getWebViewUrlAsync()
    }

    private fun getWebViewUrlAsync() = async {
        val fetchSitePluginsResult = wooStore.fetchSitePlugins(selectedSite.get())
        if (fetchSitePluginsResult.isError) {
            return@async pluginPurchaseUrl
        } else {
            val wcPayPluginInfo = wooStore.getSitePlugin(selectedSite.get(), WOO_PAYMENTS)
            if (wcPayPluginInfo != null) {
                return@async pluginSetupUrl
            } else {
                return@async pluginPurchaseUrl
            }
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onUrlLoaded(url: String) {
        if (url.contains("wordpress.com/wp-admin/")) {
            viewModelScope.launch {
                _viewState.update { ViewState(webViewUrl.await(), false) }
            }
        }
    }

    data class ViewState(val url: String, val showSpinner: Boolean)
}
