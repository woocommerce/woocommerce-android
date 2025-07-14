package com.woocommerce.android.ui.woopos.eligibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WooPosEligibilityRetryState {
    object Loading : WooPosEligibilityRetryState
    object Eligible : WooPosEligibilityRetryState
    object Ineligible : WooPosEligibilityRetryState
}

@HiltViewModel
class WooPosEligibilityViewModel @Inject constructor(
    private val canBeLaunchedInTab: WooPosCanBeLaunchedInTab
) : ViewModel() {

    private val _retryState = MutableStateFlow<WooPosEligibilityRetryState?>(null)
    val retryState: StateFlow<WooPosEligibilityRetryState?> = _retryState

    fun retryEligibilityCheck() {
        viewModelScope.launch {
            _retryState.value = WooPosEligibilityRetryState.Loading

            val result = canBeLaunchedInTab(forceRefresh = true)

            _retryState.value = if (result is WooPosLaunchability.Launchable) {
                WooPosEligibilityRetryState.Eligible
            } else {
                WooPosEligibilityRetryState.Ineligible
            }
        }
    }
}
