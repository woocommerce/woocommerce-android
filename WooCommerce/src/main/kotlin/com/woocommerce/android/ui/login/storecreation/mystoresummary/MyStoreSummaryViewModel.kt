package com.woocommerce.android.ui.login.storecreation.mystoresummary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyStoreSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableLiveData<MyStoreSummaryState>()
    val viewState: LiveData<MyStoreSummaryState> = _viewState

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    data class MyStoreSummaryState(
        val name: String,
        val domain: String,
        val category: String,
        val location: String
    )
}
