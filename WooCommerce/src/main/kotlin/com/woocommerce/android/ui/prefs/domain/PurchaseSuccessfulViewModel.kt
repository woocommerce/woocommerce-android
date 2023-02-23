package com.woocommerce.android.ui.prefs.domain

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class PurchaseSuccessfulViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val KEY_DOMAIN = "domain"
    }

    private val _viewState = savedStateHandle.getStateFlow(this, ViewState(savedStateHandle[KEY_DOMAIN] ?: ""))
    val viewState = _viewState.asLiveData()

    fun onDoneButtonClicked() {
        triggerEvent(NavigateToDashboard)
    }

    @Parcelize
    data class ViewState(
        val domain: String
    ) : Parcelable

    object NavigateToDashboard : MultiLiveEvent.Event()
}
