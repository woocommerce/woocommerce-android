package com.woocommerce.android.ui.themes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = savedStateHandle.getStateFlow(
        viewModelScope,
        ViewState(
            demoUri = "https://zainodemo.wpcomstaging.com/\" // TODO pass this as argument from previous screen"
        )
    )
    val viewState = _viewState.asLiveData()

    fun onPageSelected(updatedDemoUri: String) {
        _viewState.value = _viewState.value.copy(demoUri = updatedDemoUri)
    }

    data class ViewState(
        val demoUri: String,
    )
}
