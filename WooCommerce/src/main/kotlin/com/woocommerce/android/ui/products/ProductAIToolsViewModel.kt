package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProductAIToolsViewModel @Inject constructor(savedState: SavedStateHandle) : ScopedViewModel(savedState) {
    fun onBackPressed() {
        triggerEvent(Exit)
    }
}
