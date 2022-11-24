package com.woocommerce.android.ui.login.storecreation.mystoresummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class MyStoreSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    newStore: NewStore
) : ScopedViewModel(savedStateHandle) {

    val viewState = MutableStateFlow(
        MyStoreSummaryState(
            name = newStore.data.name,
            domain = newStore.data.domain ?: "",
            category = newStore.data.category,
            country = newStore.data.country ?: "TODO default value locale?"
        )
    ).asLiveData()

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onContinueClicked() {
        triggerEvent(NavigateToNextStep)
    }

    data class MyStoreSummaryState(
        val name: String? = null,
        val domain: String,
        val category: String? = null,
        val country: String,
    )

    object NavigateToNextStep : MultiLiveEvent.Event()
}
