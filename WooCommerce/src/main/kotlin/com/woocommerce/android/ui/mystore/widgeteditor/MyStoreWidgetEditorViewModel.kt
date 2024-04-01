package com.woocommerce.android.ui.mystore.widgeteditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyStoreWidgetEditorViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val widgetEditorState = savedState.getStateFlow(
        viewModelScope,
        WidgetEditorViewState(
            widgetList = listOf(
                MyStoreWidget(title = "Store Stats", WidgetType.StoreStats),
                MyStoreWidget(title = "Top Products", WidgetType.TopProducts),
                MyStoreWidget(title = "Blaze Campaigns", WidgetType.BlazeCampaigns),
                MyStoreWidget(title = "Store Onboarding", WidgetType.StoreOnboarding),
            ),
            isLoading = false,
        )
    )
    val viewState = widgetEditorState.asLiveData()

    data class WidgetEditorViewState(
        val widgetList: List<MyStoreWidget>,
        val isLoading: Boolean,
    )

    data class MyStoreWidget(
        val title: String,
        val type: WidgetType,
    )

    enum class WidgetType {
        StoreStats,
        TopProducts,
        BlazeCampaigns,
        StoreOnboarding,
    }
}
