package com.woocommerce.android.ui.onboarding.aboutyourstore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.onboarding.aboutyourstore.AboutYourStoreViewModel.ViewState.WebViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class AboutYourStoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    private companion object {
        const val ABOUT_STORE_SETTINGS_SECTION = "/wp-admin/admin.php?page=wc-settings&tab=general&tutorial=true"
    }

    val viewState = flowOf(WebViewState(selectedSite.get().url.slashJoin(ABOUT_STORE_SETTINGS_SECTION)))
        .asLiveData()

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    sealed class ViewState {
        object LoadingState : ViewState()
        data class WebViewState(
            val url: String
        ) : ViewState()
    }
}
