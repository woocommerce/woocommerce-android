package com.woocommerce.android.ui.prefs.domain

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.domain.DomainPurchaseViewModel.ViewState.LoadingState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class DomainPurchaseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val CART_URL = "https://wordpress.com/checkout"
        const val WEBVIEW_SUCCESS_TRIGGER_KEYWORD = "https://wordpress.com/checkout/thank-you/"
        const val WEBVIEW_EXIT_TRIGGER_KEYWORD = "https://woocommerce.com/"
    }

    private val navArgs: DomainPurchaseFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = savedStateHandle.getStateFlow<ViewState>(this, LoadingState)
    val viewState = _viewState.asLiveData()

    fun onExitTriggered() {
        triggerEvent(Exit)
    }

    fun onPurchaseSuccess() {
        triggerEvent(NavigateToSuccessScreen(navArgs.domain))
    }

    init {
        _viewState.update {
            val siteHost = Uri.parse(selectedSite.get().url).host
            ViewState.CheckoutState(
                startUrl = "$CART_URL/$siteHost",
                successTriggerKeyword = WEBVIEW_SUCCESS_TRIGGER_KEYWORD,
                exitTriggerKeyword = WEBVIEW_EXIT_TRIGGER_KEYWORD
            )
        }
    }

    sealed interface ViewState : Parcelable {
        @Parcelize
        object LoadingState : ViewState

        @Parcelize
        data class CheckoutState(
            val startUrl: String,
            val successTriggerKeyword: String,
            val exitTriggerKeyword: String
        ) : ViewState
    }

    data class NavigateToSuccessScreen(val domain: String) : MultiLiveEvent.Event()
}
