package com.woocommerce.android.ui.products.ai.banner

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIProductBannerDialogViewModel @Inject constructor(
    savedState: SavedStateHandle,
    appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedState) {
    init {
        launch {
            appPrefsWrapper.wasAIProductDescriptionPromoDialogShown = true
        }
    }

    fun onTryNowButtonClicked() {
        triggerEvent(TryAIProductDescriptionGeneration)
    }

    fun onDismissButtonClicked() {
        triggerEvent(Exit)
    }

    object TryAIProductDescriptionGeneration : MultiLiveEvent.Event()
}
