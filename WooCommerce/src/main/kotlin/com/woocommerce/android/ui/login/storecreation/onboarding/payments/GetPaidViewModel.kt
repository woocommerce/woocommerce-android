package com.woocommerce.android.ui.login.storecreation.onboarding.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType
import com.woocommerce.android.ui.login.storecreation.onboarding.payments.GetPaidViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.onboarding.payments.GetPaidViewModel.ViewState.WebViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class GetPaidViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    private val args: GetPaidFragmentArgs by savedStateHandle.navArgs()

    private val wooPaymentsUrl = selectedSite.get().url
        .slashJoin("/wp-admin/admin.php?page=wc-settings&tab=checkout")
    private val allPaymentsUrl = selectedSite.get().url
        .slashJoin("/wp-admin/admin.php?page=wc-admin&task=payments")

    private val _viewState = MutableStateFlow<ViewState>(LoadingState)
    val viewState = _viewState.asLiveData()

    init {
        val webViewUrl = if (args.taskId == OnboardingTaskType.WC_PAYMENTS.id) wooPaymentsUrl else allPaymentsUrl
        val shouldAuthenticate = selectedSite.get().isWPComAtomic
        _viewState.update {
            WebViewState(webViewUrl, shouldAuthenticate)
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    sealed class ViewState {
        object LoadingState : ViewState()
        data class WebViewState(val url: String, val shouldAuthenticate: Boolean) : ViewState()
    }
}
