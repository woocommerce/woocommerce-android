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

    init {
        _viewState.value = MyStoreSummaryState(
            name = "White Christmas Trees",
            domain = "whitechristmastrees.mywc.mysite",
            category = "Arts and Crafts",
            country = "Canada"
        )
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onContinueClicked() {
        // TODO
    }

    data class MyStoreSummaryState(
        val name: String? = null,
        val domain: String,
        val category: String? = null,
        val country: String
    )
}
