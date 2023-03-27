package com.woocommerce.android.ui.login.storecreation.onboarding.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.login.storecreation.onboarding.payments.GetPaidViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.onboarding.payments.GetPaidViewModel.ViewState.WebViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_PAYMENTS
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class GetPaidViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val pluginRepository: PluginRepository
) : ScopedViewModel(savedStateHandle) {
    private val wooPaymentsUrl = selectedSite.get().url
        .slashJoin("/wp-admin/admin.php?page=wc-settings&tab=checkout")
    private val allPaymentsUrl = selectedSite.get().url
        .slashJoin("/wp-admin/admin.php?page=wc-admin&task=payments")
    private val _viewState = MutableStateFlow<ViewState>(LoadingState)
    val viewState = _viewState.asLiveData()

    init {
        viewModelScope.launch {
            val webViewUrl = if (hasWCPayPlugin()) wooPaymentsUrl else allPaymentsUrl
            val shouldAuthenticate = selectedSite.get().isWPComAtomic
            _viewState.update {
                WebViewState(webViewUrl, shouldAuthenticate)
            }
        }
    }

    private suspend fun hasWCPayPlugin(): Boolean {
        val wooPayPlugin = pluginRepository.fetchPlugin(selectedSite.get(), WOO_PAYMENTS.pluginName)
        return wooPayPlugin.getOrNull() != null
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    sealed class ViewState {
        object LoadingState : ViewState()
        data class WebViewState(val url: String, val shouldAuthenticate: Boolean) : ViewState()
    }
}
