package com.woocommerce.android.ui.dashboard.widgeteditor

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.dashboard.data.DashboardRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class DashboardWidgetEditorViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val widgetEditorState = savedState.getStateFlow(viewModelScope, WidgetEditorState())
    val viewState = combine(widgetEditorState, dashboardRepository.widgets) { state, widgets ->
        state.copy(
            isLoading = state.widgetList.isEmpty(),
            isSaveButtonEnabled = state.widgetList != widgets,
        )
    }.asLiveData()

    private var editedWidgets: List<DashboardWidget>
        get() = widgetEditorState.value.widgetList
        set(value) {
            widgetEditorState.update { it.copy(widgetList = value) }
        }

    private val hasChanges
        get() = viewState.value?.isSaveButtonEnabled == true

    init {
        loadWidgets()
    }

    private fun loadWidgets() {
        viewModelScope.launch {
            editedWidgets = dashboardRepository.widgets.first()
        }
    }

    fun onBackPressed() {
        when {
            hasChanges -> widgetEditorState.update { it.copy(showDiscardDialog = true) }
            else -> triggerEvent(Exit)
        }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            dashboardRepository.updateWidgets(editedWidgets)
        }
        triggerEvent(Exit)
    }

    fun onSelectionChange(dashboardWidget: DashboardWidget, selected: Boolean) {
        editedWidgets = editedWidgets.map { if (it == dashboardWidget) it.copy(isVisible = selected) else it }
    }

    fun onOrderChange(fromIndex: Int, toIndex: Int) {
        val mappedFromIndex = editedWidgets.indexOf(widgetEditorState.value.orderedWidgetList[fromIndex])
        val mappedToIndex = editedWidgets.indexOf(widgetEditorState.value.orderedWidgetList[toIndex])
        editedWidgets = editedWidgets.toMutableList().apply { add(mappedFromIndex, removeAt(mappedToIndex)) }
    }

    fun onDismissDiscardDialog() {
        widgetEditorState.update { it.copy(showDiscardDialog = false) }
    }

    fun onDiscardChanges() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class WidgetEditorState(
        val widgetList: List<DashboardWidget> = emptyList(),
        val showDiscardDialog: Boolean = false,
        val isSaveButtonEnabled: Boolean = false,
        val isLoading: Boolean = true
    ) : Parcelable {
        @IgnoredOnParcel
        val orderedWidgetList: List<DashboardWidget> =
            widgetList.filter { it.isAvailable } + widgetList.filterNot { it.isAvailable }
    }
}
