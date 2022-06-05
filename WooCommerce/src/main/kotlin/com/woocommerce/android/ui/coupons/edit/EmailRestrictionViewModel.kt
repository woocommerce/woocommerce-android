package com.woocommerce.android.ui.coupons.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class EmailRestrictionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: EmailRestrictionFragmentArgs by savedState.navArgs()

    private val allowedEmailsDraft = savedStateHandle.getStateFlow(
        viewModelScope,
        navArgs.allowedEmails.toList()
    )

    val viewState = allowedEmailsDraft.map {
        ViewState(
            hasChanges = it != navArgs.allowedEmails.toList(),
            allowedEmails = allowedEmailsDraft.value
        )
    }.asLiveData()

    fun onBackPressed() {
        val event = viewState.value?.takeIf { it.hasChanges }?.let { viewState ->
            ExitWithResult(viewState.allowedEmails)
        } ?: Exit

        triggerEvent(event)
    }

    data class ViewState(
        val hasChanges: Boolean,
        val allowedEmails: List<String>
    )
}
