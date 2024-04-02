package com.woocommerce.android.ui.mystore.widgeteditor

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.mystore.widgeteditor.MyStoreWidgetEditorViewModel.WidgetType.BlazeCampaigns
import com.woocommerce.android.ui.mystore.widgeteditor.MyStoreWidgetEditorViewModel.WidgetType.StoreOnboarding
import com.woocommerce.android.ui.mystore.widgeteditor.MyStoreWidgetEditorViewModel.WidgetType.StoreStats
import com.woocommerce.android.ui.mystore.widgeteditor.MyStoreWidgetEditorViewModel.WidgetType.TopProducts
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class MyStoreWidgetEditorViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val widgetEditorState: MutableStateFlow<WidgetEditorState> = savedState.getStateFlow(
        viewModelScope,
        WidgetEditorState(
            widgetList = listOf(
                MyStoreWidget(
                    title = "Store Stats",
                    isSelected = true,
                    StoreStats
                ),
                MyStoreWidget(
                    title = "Top Products",
                    isSelected = true,
                    TopProducts
                ),
                MyStoreWidget(
                    title = "Blaze Campaigns",
                    isSelected = true,
                    BlazeCampaigns
                ),
                MyStoreWidget(
                    title = "Store Onboarding",
                    isSelected = false,
                    StoreOnboarding
                ),
            ),
            showDiscardDialog = false,
            isLoading = false,
        )
    )
    val viewState = widgetEditorState.asLiveData()

    private val existingWidgetConfiguration = widgetEditorState.value.widgetList
    private var editedWidgets = widgetEditorState.value.widgetList

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onSaveClicked() {
        TODO("Not yet implemented")
    }

    fun onSelectionChange(myStoreWidget: MyStoreWidget, selected: Boolean) {
        editedWidgets = editedWidgets
            .map { if (it == myStoreWidget) it.copy(isSelected = selected) else it }
        updateWidgetStateWith(editedWidgets)
    }

    fun onOrderChange(fromIndex: Int, toIndex: Int) {
        editedWidgets = editedWidgets.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        updateWidgetStateWith(editedWidgets)
    }

    private fun updateWidgetStateWith(editedWidgets: List<MyStoreWidget>) {
        widgetEditorState.update {
            it.copy(
                widgetList = editedWidgets,
                isSaveButtonEnabled = editedWidgets != existingWidgetConfiguration
            )
        }
    }


    fun onDismissDiscardChanges() {
        TODO("Not yet implemented")
    }

    fun onDiscardChanges() {
        TODO("Not yet implemented")
    }

    @Parcelize
    data class WidgetEditorState(
        val widgetList: List<MyStoreWidget>,
        val showDiscardDialog: Boolean = false,
        val isSaveButtonEnabled: Boolean = false,
        val isLoading: Boolean = false
    ) : Parcelable

    @Parcelize
    data class MyStoreWidget(
        val title: String,
        val isSelected: Boolean,
        val type: WidgetType,
    ) : Parcelable

    enum class WidgetType {
        StoreStats,
        TopProducts,
        BlazeCampaigns,
        StoreOnboarding,
    }
}
