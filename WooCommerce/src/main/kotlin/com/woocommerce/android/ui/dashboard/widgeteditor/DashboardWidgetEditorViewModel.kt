package com.woocommerce.android.ui.dashboard.widgeteditor

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.dashboard.data.DashboardRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class DashboardWidgetEditorViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val dashboardRepository: DashboardRepository,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val widgetEditorState = savedState.getStateFlow(viewModelScope, WidgetEditorState())
    val viewState = combine(widgetEditorState, dashboardRepository.widgets) { state, widgets ->
        state.copy(
            isLoading = state.widgetList.isEmpty(),
            isSaveButtonEnabled = state.widgetList.map { DashboardWidget(it.type, it.isSelected) } != widgets,
        )

    }.asLiveData()

    private var editedWidgets: List<DashboardWidgetUi>
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
                .map { widget ->
                    DashboardWidgetUi(
                        title = resourceProvider.getString(widget.type.titleResource),
                        isSelected = widget.isAdded,
                        type = widget.type
                    )
                }
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
            dashboardRepository.updateWidgets(editedWidgets.map { widget ->
                DashboardWidget(
                    type = widget.type,
                    isAdded = widget.isSelected
                )
            })
        }
        triggerEvent(Exit)
    }

    fun onSelectionChange(dashboardWidget: DashboardWidgetUi, selected: Boolean) {
        editedWidgets = editedWidgets.map { if (it == dashboardWidget) it.copy(isSelected = selected) else it }
    }

    fun onOrderChange(fromIndex: Int, toIndex: Int) {
        editedWidgets = editedWidgets.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    fun onDismissDiscardDialog() {
        widgetEditorState.update { it.copy(showDiscardDialog = false) }
    }

    fun onDiscardChanges() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class WidgetEditorState(
        val widgetList: List<DashboardWidgetUi> = emptyList(),
        val showDiscardDialog: Boolean = false,
        val isSaveButtonEnabled: Boolean = false,
        val isLoading: Boolean = true
    ) : Parcelable

    @Parcelize
    data class DashboardWidgetUi(
        val title: String,
        val isSelected: Boolean,
        val type: DashboardWidget.Type
    ) : Parcelable
}
