package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.LEFT
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class NewOrderNotificationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedStateHandle) {
    private val currencyParameters = parameterRepository.getParameters()
    private val currencyFormattingParameters = currencyParameters.currencyFormattingParameters

    private val _viewState = MutableStateFlow(
        ViewState(
            currencySymbol = currencyParameters.currencySymbol.orEmpty(),
            currencyPosition = currencyFormattingParameters?.currencyPosition ?: LEFT,
            currencyDecimalSeparator = currencyFormattingParameters?.currencyDecimalSeparator ?: ".",
            currencyThousandSeparator = currencyFormattingParameters?.currencyThousandSeparator ?: ",",
            currencyDecimalNumber = currencyFormattingParameters?.currencyDecimalNumber ?: DEFAULT_DECIMAL_NUMBER
        )
    )
    val viewState = _viewState.asStateFlow()

    fun onNotificationsEnabledChanged(isEnabled: Boolean) {
        _viewState.update { it.copy(notificationsEnabled = isEnabled) }
    }

    fun onNotificationPreferenceChanged(preference: NotificationPreference) {
        _viewState.update { it.copy(notificationPreference = preference) }
    }

    data class ViewState(
        val notificationsEnabled: Boolean = true,
        val notificationPreference: NotificationPreference = NotificationPreference.AllOrders,
        val currencySymbol: String,
        val currencyPosition: CurrencyPosition,
        val currencyDecimalSeparator: String,
        val currencyThousandSeparator: String,
        val currencyDecimalNumber: Int
    )

    sealed interface NotificationPreference {
        data object AllOrders : NotificationPreference

        data class HighValueOrders(
            val thresholdAmount: BigDecimal = BigDecimal(DEFAULT_THRESHOLD_AMOUNT)
        ) : NotificationPreference
    }

    companion object {
        private const val DEFAULT_DECIMAL_NUMBER = 2
        private const val DEFAULT_THRESHOLD_AMOUNT = 100
    }
}
