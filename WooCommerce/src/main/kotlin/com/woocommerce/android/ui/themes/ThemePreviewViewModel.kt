package com.woocommerce.android.ui.themes

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class ThemePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = savedStateHandle.getStateFlow(
        viewModelScope,
        ViewState(
            themeName = "Zaino",
            demoUri = "https://zainodemo.wpcomstaging.com/\" // TODO pass this as argument from previous screen"
        )
    )
    val viewState = _viewState.asLiveData()

    fun onPageSelected(updatedDemoUri: String) {
        _viewState.value = _viewState.value.copy(demoUri = updatedDemoUri)
    }

    fun onBackNavigationClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onSelectThemeClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    @Parcelize
    data class ViewState(
        val themeName: String,
        val demoUri: String
    ) : Parcelable
}
