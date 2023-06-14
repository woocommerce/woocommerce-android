package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductSharingViewModel.ViewState.AIGeneratingState
import com.woocommerce.android.ui.products.ProductSharingViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.products.ProductSharingViewModel.ViewState.ProductSharingViewState
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ProductSharingViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: ProductSharingDialogArgs by savedStateHandle.navArgs()

    private val _viewState = MutableStateFlow<ViewState>(LoadingState)
    val viewState = _viewState.asLiveData()

    init {
        _viewState.update {
            ProductSharingViewState(
                productTitle = navArgs.productName,
                shareMessage = "",
                buttonState = AIButtonState.WriteWithAI(
                    resourceProvider.getString(R.string.product_sharing_write_with_ai)
                )
            )
        }
    }

    fun onGenerateButtonClicked() {
        _viewState.update {
            AIGeneratingState
        }
    }

    sealed class ViewState {
        object LoadingState : ViewState()
        object AIGeneratingState : ViewState()
        data class ProductSharingViewState(
            val productTitle: String,
            val shareMessage: String,
            val buttonState: AIButtonState
        ) : ViewState()
    }

    sealed class AIButtonState(val label: String) {
        data class WriteWithAI(val text: String) : AIButtonState(text)
        data class Regenerate(val text: String) : AIButtonState(text)
    }
}
