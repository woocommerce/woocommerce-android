package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FirstProductCelebrationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: FirstProductCelebrationDialogArgs by savedStateHandle.navArgs()

    fun onShareButtonClicked() {
        triggerEvent(ProductNavigationTarget.ShareProduct(navArgs.permalink, navArgs.productName))
    }

    fun onDismissButtonClicked() {
        triggerEvent(Exit)
    }
}
