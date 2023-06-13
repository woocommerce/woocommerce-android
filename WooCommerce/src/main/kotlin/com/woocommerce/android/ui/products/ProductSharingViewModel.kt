package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.products.ProductSharingViewModel.ViewState.AIGeneratingState
import com.woocommerce.android.ui.products.ProductSharingViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.products.ProductSharingViewModel.ViewState.ProductSharingViewState
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ProductSharingViewModel @Inject constructor(
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
                showRegenerateButton = false,
                enableShareButton = false
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
            val showRegenerateButton: Boolean,
            val enableShareButton: Boolean
        ) : ViewState()
    }
}
