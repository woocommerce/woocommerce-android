package com.woocommerce.android.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AIProductDescriptionDialogViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun onTryNowButtonClicked() {
        triggerEvent(TryAIProductDescriptionGeneration)
    }

    fun onDismissButtonClicked() {
        triggerEvent(Exit)
    }

    object TryAIProductDescriptionGeneration : MultiLiveEvent.Event()
}
