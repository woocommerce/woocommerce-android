package com.woocommerce.android.ui.prefs.plugins

import androidx.annotation.ColorRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Error
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded.Plugin
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded.Plugin.PluginStatus.AutoManaged
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded.Plugin.PluginStatus.Inactive
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded.Plugin.PluginStatus.Unknown
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded.Plugin.PluginStatus.UpToDate
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded.Plugin.PluginStatus.UpdateAvailable
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loading
import com.woocommerce.android.util.isGreaterThan
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel
import org.wordpress.android.fluxc.store.PluginCoroutineStore
import org.wordpress.android.fluxc.store.PluginCoroutineStore.InstalledPluginResponse
import javax.inject.Inject

@HiltViewModel
class PluginsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pluginsStore: PluginCoroutineStore,
    private val site: SelectedSite,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private val MANAGED_PLUGINS = setOf(
            "akismet",
            "amp",
            "automatewoo",
            "av1-file-shim",
            "classic-editor",
            "coblocks",
            "crowdsignal-forms",
            "enable-jquery-migrate-helper",
            "facebook-for-woocommerce",
            "full-site-editing",
            "gutenberg",
            "hello-dolly",
            "jetpack",
            "jetpack-weekly",
            "klarna-checkout-for-woocommerce",
            "klarna-payments-for-woocommerce",
            "layout-grid",
            "mailchimp-for-woocommerce",
            "page-optimize",
            "polldaddy",
            "storefront-powerpack",
            "taxjar-simplified-taxes-for-woocommerce",
            "woocommerce",
            "woocommerce-bookings",
            "woocommerce-conditional-shipping-and-payments",
            "woocommerce-deposits",
            "woocommerce-gateway-eway",
            "woocommerce-gateway-paypal-express-checkout",
            "woocommerce-gateway-stripe",
            "woocommerce-min-max-quantities",
            "woocommerce-one-page-checkout",
            "woocommerce-payfast-gateway",
            "woocommerce-points-and-rewards",
            "woocommerce-pre-orders",
            "woocommerce-product-addons",
            "woocommerce-product-bundles",
            "woocommerce-product-vendors",
            "woocommerce-services",
            "woocommerce-shipment-tracking",
            "woocommerce-shipping-australia-post",
            "woocommerce-shipping-canada-post",
            "woocommerce-shipping-fedex",
            "woocommerce-shipping-royalmail",
            "woocommerce-shipping-ups",
            "woocommerce-shipping-usps",
            "woocommerce-square",
            "woocommerce-subscriptions",
            "woocommerce-table-rate-shipping",
            "woocommerce-xero",
            "woothemes-sensei",
            "wordpress-seo",
            "wordpress-seo-premium",
            "wpcom-file-shim",
            "wpcomsh"
        )
    }
    private val _viewState = MutableSharedFlow<ViewState>(1)
    val viewState = _viewState.asLiveData()

    init {
        loadPlugins()
    }

    private fun loadPlugins() {
        viewModelScope.launch(Dispatchers.IO) {
            _viewState.emit(Loading)
            val response = pluginsStore.fetchInstalledPlugins(site.get())
            if (response is InstalledPluginResponse.Success) {
                _viewState.emit(
                    Loaded(
                        plugins = response.plugins
                            .filter { it.installedVersion.isNotNullOrEmpty() && it.displayName.isNotNullOrEmpty() }
                            .map { Plugin(it.displayName!!, it.authorName, it.installedVersion!!, it.getState()) }
                    )
                )
            } else {
                _viewState.emit(Error)
            }
        }
    }

    private fun ImmutablePluginModel.getState(): Plugin.PluginStatus {
        return when {
            isAutoManaged(site.get()) -> AutoManaged(resourceProvider.getString(R.string.plugin_state_auto_managed))
            !isActive -> Inactive(resourceProvider.getString(R.string.plugin_state_inactive))
            wpOrgPluginVersion.isNullOrEmpty() -> Unknown
            isUpdateAvailable() -> UpdateAvailable(
                resourceProvider.getString(R.string.plugin_state_update_available, wpOrgPluginVersion!!)
            )
            else -> UpToDate(resourceProvider.getString(R.string.plugin_state_up_to_date))
        }
    }

    private fun ImmutablePluginModel.isUpdateAvailable(): Boolean {
        return wpOrgPluginVersion.isGreaterThan(installedVersion)
    }

    private fun ImmutablePluginModel.isAutoManaged(site: SiteModel): Boolean {
        return if (!site.isAutomatedTransfer || !isInstalled) {
            false
        } else {
            slug in MANAGED_PLUGINS
        }
    }

    fun onRetryClicked() {
        loadPlugins()
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    sealed interface ViewState {
        data object Loading : ViewState
        data object Error : ViewState
        data class Loaded(
            val plugins: List<Plugin> = emptyList()
        ) : ViewState {
            data class Plugin(
                val name: String,
                val authorName: String?,
                val version: String,
                val status: PluginStatus
            ) {
                sealed class PluginStatus(open val title: String, @ColorRes val color: Int) {
                    data class UpToDate(override val title: String) : PluginStatus(title, R.color.color_info)
                    data class AutoManaged(override val title: String) : PluginStatus(title, R.color.color_info)
                    data class UpdateAvailable(override val title: String) : PluginStatus(title, R.color.color_alert)
                    data class Inactive(override val title: String) : PluginStatus(
                        title,
                        R.color.color_on_surface_disabled
                    )
                    data object Unknown : PluginStatus("", R.color.color_on_surface_disabled)
                }
            }
        }
    }
}
