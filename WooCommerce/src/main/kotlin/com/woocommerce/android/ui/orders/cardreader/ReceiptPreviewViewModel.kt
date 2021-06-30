package com.woocommerce.android.ui.orders.cardreader

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReceiptPreviewViewModel
@Inject constructor(
    savedState: SavedStateHandle
): ScopedViewModel(savedState) {
    private val arguments: ReceiptPreviewFragmentArgs by savedState.navArgs()
}
