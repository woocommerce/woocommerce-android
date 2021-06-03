package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class PrintShippingLabelCustomsFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: PrintShippingLabelCustomsFormFragmentArgs by savedState.navArgs()

    private val _viewState = MutableLiveData(ViewState())
    val viewState = _viewState

    fun onPrintButtonClicked() {
    }

    fun onSaveForLaterClicked() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val isProgressDialogShown: Boolean = false
    ) : Parcelable
}
