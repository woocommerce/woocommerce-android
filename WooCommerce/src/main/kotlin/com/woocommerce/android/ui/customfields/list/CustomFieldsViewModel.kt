package com.woocommerce.android.ui.customfields.list

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class CustomFieldsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CustomFieldsRepository
) : ScopedViewModel(savedStateHandle) {
    private val args: CustomFieldsFragmentArgs by savedStateHandle.navArgs()

    private val isLoading = MutableStateFlow(false)
    private val customFields = repository.observeDisplayableCustomFields(args.parentItemId)
    private val pendingChanges = savedStateHandle.getStateFlow(viewModelScope, PendingChanges())

    val parentItemId = args.parentItemId

    val state = combine(
        customFields,
        pendingChanges,
        isLoading,
    ) { customFields, pendingChanges, isLoading ->
        UiState(
            customFields = customFields.map { CustomFieldUiModel(it) }.combineWithChanges(pendingChanges),
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

    fun onCustomFieldClicked(field: CustomFieldUiModel) {
        triggerEvent(OpenCustomFieldEditor(field))
    }

    fun onCustomFieldValueClicked(field: CustomFieldUiModel) {
        triggerEvent(CustomFieldValueClicked(field))
    }

    fun onCustomFieldUpdated(result: CustomFieldUiModel) {
        pendingChanges.update {
            if (result.id == null) {
                it.copy(insertedFields = it.insertedFields + result)
            } else {
                it.copy(editedFields = it.editedFields + result)
            }
        }
    }

    private fun List<CustomFieldUiModel>.combineWithChanges(pendingChanges: PendingChanges) = map { customField ->
        pendingChanges.editedFields.find { it.id == customField.id } ?: customField
    } + pendingChanges.insertedFields

    data class UiState(
        val customFields: List<CustomFieldUiModel>,
        val isLoading: Boolean
    )

    @Parcelize
    private data class PendingChanges(
        val editedFields: List<CustomFieldUiModel> = emptyList(),
        val insertedFields: List<CustomFieldUiModel> = emptyList()
    ) : Parcelable

    data class OpenCustomFieldEditor(val field: CustomFieldUiModel) : MultiLiveEvent.Event()
    data class CustomFieldValueClicked(val field: CustomFieldUiModel) : MultiLiveEvent.Event()
}
