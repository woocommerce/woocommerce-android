package com.woocommerce.android.ui.mystore.widgeteditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.mystore.widgeteditor.MyStoreWidgetEditorViewModel.WidgetEditorViewState.WidgetEditorContent
import com.woocommerce.android.ui.mystore.widgeteditor.MyStoreWidgetEditorViewModel.WidgetType.BlazeCampaigns
import com.woocommerce.android.ui.mystore.widgeteditor.MyStoreWidgetEditorViewModel.WidgetType.StoreOnboarding
import com.woocommerce.android.ui.mystore.widgeteditor.MyStoreWidgetEditorViewModel.WidgetType.StoreStats
import com.woocommerce.android.ui.mystore.widgeteditor.MyStoreWidgetEditorViewModel.WidgetType.TopProducts
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class MyStoreWidgetEditorViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun onBackPressed() {
        TODO("Not yet implemented")
    }

    fun onSaveChanges() {
        TODO("Not yet implemented")
    }

    @Suppress("unused_parameter")
    fun onSelectionChange(myStoreWidget: MyStoreWidget, selected: Boolean) {

    }

    @Suppress("unused_parameter")
    fun onOrderChange(fromIndex: Int, toIndex: Int) {

    }

    fun onDismissDiscardChanges() {
        TODO("Not yet implemented")
    }

    fun onDiscardChanges() {
        TODO("Not yet implemented")
    }

    private val widgetEditorState: MutableStateFlow<WidgetEditorViewState> = savedState.getStateFlow(
        viewModelScope,
        WidgetEditorContent(
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
        )
    )
    val viewState = widgetEditorState.asLiveData()

    sealed class WidgetEditorViewState {
        data class WidgetEditorContent(
            val widgetList: List<MyStoreWidget>,
            val showDiscardDialog: Boolean = false,
            val isSaveButtonEnabled: Boolean = false
        ) : WidgetEditorViewState()

        data object Loading : WidgetEditorViewState()
    }

    data class MyStoreWidget(
        val title: String,
        val isSelected: Boolean,
        val type: WidgetType,
    )

    enum class WidgetType {
        StoreStats,
        TopProducts,
        BlazeCampaigns,
        StoreOnboarding,
    }
}
