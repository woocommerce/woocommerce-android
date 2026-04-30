package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.prefs.notifications.NewOrderNotificationSettingsViewModel.NotificationPreference
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.CurrencyFormattingParameters
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.LEFT
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class NewOrderNotificationSettingsViewModelTest : BaseUnitTest() {
    private val parameterRepository: ParameterRepository = mock()
    private lateinit var viewModel: NewOrderNotificationSettingsViewModel

    private suspend fun setup() {
        whenever(parameterRepository.getParameters()).thenReturn(
            SiteParameters(
                currencyCode = "USD",
                currencySymbol = "$",
                currencyFormattingParameters = CurrencyFormattingParameters(
                    currencyThousandSeparator = ",",
                    currencyDecimalSeparator = ".",
                    currencyDecimalNumber = 2,
                    currencyPosition = LEFT
                ),
                weightUnit = null,
                dimensionUnit = null,
                gmtOffset = 0f
            )
        )
        viewModel = NewOrderNotificationSettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            parameterRepository = parameterRepository
        )
    }

    @Test
    fun `when view is loaded, then all orders preference is selected`() = testBlocking {
        setup()

        assertThat(viewModel.viewState.getOrAwaitValue().notificationPreference)
            .isEqualTo(NotificationPreference.AllOrders)
    }

    @Test
    fun `when notifications switch is changed, then update state`() = testBlocking {
        setup()

        viewModel.onNotificationsEnabledChanged(false)

        assertThat(viewModel.viewState.getOrAwaitValue().notificationsEnabled).isFalse()
    }

    @Test
    fun `when high value preference is selected, then update state`() = testBlocking {
        setup()

        viewModel.onNotificationPreferenceChanged(NotificationPreference.HighValueOrders())

        assertThat(viewModel.viewState.getOrAwaitValue().notificationPreference)
            .isEqualTo(NotificationPreference.HighValueOrders())
    }

    @Test
    fun `when high value threshold amount is changed, then update state`() = testBlocking {
        setup()

        viewModel.onNotificationPreferenceChanged(NotificationPreference.HighValueOrders(BigDecimal(750)))

        assertThat(viewModel.viewState.getOrAwaitValue().notificationPreference)
            .isEqualTo(NotificationPreference.HighValueOrders(BigDecimal(750)))
    }
}
