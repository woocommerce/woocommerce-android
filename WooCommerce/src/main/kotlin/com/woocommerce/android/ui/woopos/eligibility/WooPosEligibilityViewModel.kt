package com.woocommerce.android.ui.woopos.eligibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosEligibilityViewModel @Inject constructor() : ViewModel() {

    private val _isRetryLoading = MutableStateFlow(false)
    val isRetryLoading: StateFlow<Boolean> = _isRetryLoading

    fun retryEligibilityCheck() {
        viewModelScope.launch {
            _isRetryLoading.value = true

            val dummyDelay = 2000
            kotlinx.coroutines.delay(dummyDelay) // simulate retry work

            _isRetryLoading.value = false
        }
    }
}
