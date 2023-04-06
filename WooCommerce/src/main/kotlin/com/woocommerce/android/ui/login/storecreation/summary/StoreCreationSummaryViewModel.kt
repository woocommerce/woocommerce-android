package com.woocommerce.android.ui.login.storecreation.summary

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StoreCreationSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val createStore: CreateFreeTrialStore
) : ScopedViewModel(savedStateHandle) {
    fun onCancelPressed() { triggerEvent(OnCancelPressed) }
    fun onTryForFreeButtonPressed() { triggerEvent(OnStoreCreationSuccess) }

    object OnCancelPressed : MultiLiveEvent.Event()
    object OnStoreCreationSuccess : MultiLiveEvent.Event()

    object OnStoreCreationFailure: MultiLiveEvent.Event()
}
