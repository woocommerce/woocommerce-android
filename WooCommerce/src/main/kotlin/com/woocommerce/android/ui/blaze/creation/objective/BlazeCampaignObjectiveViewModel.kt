package com.woocommerce.android.ui.blaze.creation.objective

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignObjectiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    fun onDismissClick() {
        triggerEvent(Exit)
    }

    data class ObjectiveViewState(
        val items: List<ObjectiveItem>,
        val selectedItemId: String? = null,
        val isStoreSelectionButtonToggled: Boolean = false,
    ) {
        val isSaveButtonEnabled: Boolean
            get() = selectedItemId != null
    }

    data class ObjectiveItem(
        val id: String,
        val title: String,
        val description: String,
        val suitableForDescription: String,
    )
}
