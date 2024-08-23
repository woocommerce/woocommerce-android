package com.woocommerce.android.ui.customfields

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomFieldsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CustomFieldsRepository
) : ScopedViewModel(savedStateHandle) {
    private val args: CustomFieldsFragmentArgs by savedStateHandle.navArgs()

    private val isLoading = MutableStateFlow(false)
    private val customFields = repository.observeDisplayableCustomFields(args.parentItemId)

    val state = combine(
        customFields,
        isLoading
    ) { customFields, isLoading ->
        UiState(
            customFields = customFields.map { CustomFieldUiModel(it) },
            isLoading = isLoading
        )
    }.asLiveData()

    fun onBackClick() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onPullToRefresh() {
        launch {
            isLoading.value = true
            repository.refreshCustomFields(args.parentItemId, args.parentItemType).onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.custom_fields_list_loading_error))
            }
            isLoading.value = false
        }
    }

    fun onCustomFieldValueClicked(field: CustomFieldUiModel) {
        triggerEvent(CustomFieldValueClicked(field))
    }

    data class UiState(
        val customFields: List<CustomFieldUiModel>,
        val isLoading: Boolean
    )

    data class CustomFieldValueClicked(val field: CustomFieldUiModel) : MultiLiveEvent.Event()
}
