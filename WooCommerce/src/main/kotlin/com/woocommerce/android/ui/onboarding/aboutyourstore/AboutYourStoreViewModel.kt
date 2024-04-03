package com.woocommerce.android.ui.onboarding.aboutyourstore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.onboarding.aboutyourstore.AboutYourStoreViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.onboarding.aboutyourstore.AboutYourStoreViewModel.ViewState.WebViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutYourStoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    private companion object {
        const val ABOUT_STORE_SETTINGS_SECTION = "/wp-admin/admin.php?page=wc-settings&tab=general&tutorial=true"
    }

    private val _viewState = MutableStateFlow<ViewState>(LoadingState)
    val viewState = _viewState.asLiveData()

    init {
        launch {
            _viewState.update {
                WebViewState(
                    url = selectedSite.get().url + ABOUT_STORE_SETTINGS_SECTION,
                    shouldAuthenticate = selectedSite.get().isWPComAtomic
                )
            }
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    sealed class ViewState {
        object LoadingState : ViewState()
        data class WebViewState(
            val url: String,
            val shouldAuthenticate: Boolean
        ) : ViewState()
    }
}
