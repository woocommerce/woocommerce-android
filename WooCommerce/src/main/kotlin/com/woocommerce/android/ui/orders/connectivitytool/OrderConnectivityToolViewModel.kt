package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import javax.inject.Inject

class OrderConnectivityToolViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    val viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState()
    )

    data class ViewState(
        val isContactSupportEnabled: Boolean = false
    )
}
