package com.woocommerce.android.ui.jetpack.benefits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.jetpack.JetpackActivationEligibilityErrorFragmentArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@HiltViewModel
class JetpackActivationEligibilityErrorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationEligibilityErrorFragmentArgs by savedStateHandle.navArgs()
    private val isRetrying =
        savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = false, key = "is-loading")

    val viewState = combine(
        flowOf(Pair(navArgs.username, navArgs.role)),
        isRetrying
    ) { (username, role), isRetrying ->
        ViewState(
            username = username,
            role = role,
            isRetrying = isRetrying
        )
    }.asLiveData()

    data class ViewState(
        val username: String,
        val role: String,
        val isRetrying: Boolean
    )
}
