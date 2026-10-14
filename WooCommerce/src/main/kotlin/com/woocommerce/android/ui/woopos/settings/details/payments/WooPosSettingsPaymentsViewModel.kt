package com.woocommerce.android.ui.woopos.settings.details.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsPaymentsViewModel @Inject constructor(
    private val preferencesRepository: WooPosPreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSettingsPaymentsState>(WooPosSettingsPaymentsState.Loading)
    val state: StateFlow<WooPosSettingsPaymentsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.isPaymentSuccessSoundEnabled.collect { isEnabled ->
                _state.value = WooPosSettingsPaymentsState.Content(isPaymentSuccessSoundEnabled = isEnabled)
            }
        }
    }

    fun onUIEvent(event: WooPosSettingsPaymentsUIEvent) {
        when (event) {
            is WooPosSettingsPaymentsUIEvent.PaymentSuccessSoundToggled -> onPaymentSuccessSoundToggled(event.isEnabled)
        }
    }

    private fun onPaymentSuccessSoundToggled(isEnabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPaymentSuccessSoundEnabled(isEnabled)
        }
    }
}
