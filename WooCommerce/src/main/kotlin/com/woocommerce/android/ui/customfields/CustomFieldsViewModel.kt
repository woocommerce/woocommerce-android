package com.woocommerce.android.ui.customfields

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class CustomFieldsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: CustomFieldsRepository
) : ScopedViewModel(savedStateHandle) {
    private val args: CustomFieldsFragmentArgs by savedStateHandle.navArgs()

    private val isLoading = MutableStateFlow(false)
    private val customFields = repository.observeDisplayableCustomFields(args.parentItemId)

    val state = combine(
        customFields,
        isLoading
    ) { customFields, isLoading ->
        UiState(
            customFields = customFields,
            isLoading = isLoading
        )
    }.asLiveData()

    fun onBackClick() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    data class UiState(
        val customFields: List<CustomField>,
        val isLoading: Boolean
    )
}
