package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class NewOrderNotificationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedStateHandle) {
    private val currencyParameters = parameterRepository.getParameters()

    private val _viewState = MutableStateFlow(
        ViewState(
            currencySymbol = currencyParameters.currencySymbol.orEmpty()
        )
    )
    val viewState = _viewState.asLiveData()

    fun onNotificationsEnabledChanged(isEnabled: Boolean) {
        _viewState.update { it.copy(notificationsEnabled = isEnabled) }
    }

    fun onNotificationPreferenceChanged(preference: NotificationPreference) {
        _viewState.update { it.copy(notificationPreference = preference) }
    }

    data class ViewState(
        val notificationsEnabled: Boolean = true,
        val notificationPreference: NotificationPreference = NotificationPreference.AllOrders,
        val currencySymbol: String
    )

    sealed interface NotificationPreference {
        data object AllOrders : NotificationPreference

        data class HighValueOrders(
            val thresholdAmount: BigDecimal = BigDecimal(DEFAULT_THRESHOLD_AMOUNT)
        ) : NotificationPreference
    }

    companion object {
        private const val DEFAULT_THRESHOLD_AMOUNT = 100
    }
}
