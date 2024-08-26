package com.woocommerce.android.ui.customfields.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomFieldsEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    val state = savedStateHandle.getStateFlow(viewModelScope, UiState(), "customField")

    init {
        if (!savedStateHandle.contains("customField")) {
            initUiState()
        }
    }

    fun onKeyChanged(key: String) {
        state.value = state.value.copy(customField = state.value.customField.copy(key = key))
    }

    fun onValueChanged(value: String) {
        state.value = state.value.copy(customField = state.value.customField.copy(value = value))
    }

    fun onDoneClicked() {
        TODO()
    }

    private fun initUiState() = launch {
        // TODO: load the custom field from the repository if we have an id
        val customField = CustomFieldUiModel("", "")
        state.value = UiState(
            customField = customField,
            isHtml = customField.valueStrippedHtml != customField.value
        )
    }

    data class UiState(
        val customField: CustomFieldUiModel = CustomFieldUiModel("", ""),
        val isHtml: Boolean = false
    )
}
