package com.woocommerce.android.ui.payments.taptopay.summary

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TapToPaySummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
}
