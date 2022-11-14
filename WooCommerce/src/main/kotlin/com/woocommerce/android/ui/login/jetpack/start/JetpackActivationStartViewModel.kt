package com.woocommerce.android.ui.login.jetpack.start

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

@HiltViewModel
class JetpackActivationStartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    val viewState: Flow<JetpackActivationState> = emptyFlow()

    data class JetpackActivationState(
        val url: String,
        val isJetpackInstalled: Boolean,
        // This would be true if the site is connected to a different account
        val isJetpackConnected: Boolean
    )
}
