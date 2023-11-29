package com.woocommerce.android.ui.login.storecreation.theme

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.login.storecreation.theme.ThemeActivationViewModel.ViewState.LoadingState
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeActivationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(LoadingState)
    val viewState = _viewState.asLiveData()

    sealed interface ViewState {
        object LoadingState : ViewState
        data class ErrorState(val onRetry: () -> Unit) : ViewState
    }
}
