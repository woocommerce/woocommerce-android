package com.woocommerce.android.ui.login.storecreation.mystoresummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MyStoreSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    newStore: NewStore
) : ScopedViewModel(savedStateHandle) {

    val viewState = newStore.store
        .map {
            MyStoreSummaryState(
                name = it.name,
                domain = it.domain ?: "",
                category = it.category,
                country = it.country ?: "TODO default value US?"
            )
        }
        .asLiveData()

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
