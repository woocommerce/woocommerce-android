package com.woocommerce.android.ui.woopos.cardreader.connection

import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class WooPosOnboardingErrorMapperTest {
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any<Int>()) }.thenReturn("stub")
        on { getString(any<Int>(), any()) }.thenReturn("stub")
    }
    private val onDismiss: () -> Unit = mock()
    private val onRetry: () -> Unit = mock()
    private val onSkipPendingRequirements: () -> Unit = mock()

    private val sut = WooPosOnboardingErrorMapper(resourceProvider)

    @Test
    fun `when StoreCountryNotSupported, then returns DialogError without primary button`() {
        // GIVEN
        val state = CardReaderOnboardingState.StoreCountryNotSupported("US")

        // WHEN
        val result = sut.map(state, onDismiss, onRetry, onSkipPendingRequirements)

        // THEN
        assertThat(result).isInstanceOf(WooPosOnboardingErrorMapper.Result.DialogError::class.java)
        val error = (result as WooPosOnboardingErrorMapper.Result.DialogError).error
        assertThat(error.primaryButton).isNull()
    }

    @Test
    fun `when StripeAccountPendingRequirement, then returns DialogError with skip button`() {
        // GIVEN
        val state = CardReaderOnboardingState.StripeAccountPendingRequirement(
            dueDate = null,
            preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
            version = "1.0",
            countryCode = "US",
        )

        // WHEN
        val result = sut.map(state, onDismiss, onRetry, onSkipPendingRequirements)

        // THEN
        assertThat(result).isInstanceOf(WooPosOnboardingErrorMapper.Result.DialogError::class.java)
        val error = (result as WooPosOnboardingErrorMapper.Result.DialogError).error
        assertThat(error.primaryButton).isNotNull
        error.primaryButton!!.onClick()
        verify(onSkipPendingRequirements).invoke()
    }

    @Test
    fun `when NoConnectionError, then returns DialogError with retry button`() {
        // GIVEN
        val state = CardReaderOnboardingState.NoConnectionError

        // WHEN
        val result = sut.map(state, onDismiss, onRetry, onSkipPendingRequirements)

        // THEN
        assertThat(result).isInstanceOf(WooPosOnboardingErrorMapper.Result.DialogError::class.java)
        val error = (result as WooPosOnboardingErrorMapper.Result.DialogError).error
        assertThat(error.primaryButton).isNotNull
        error.primaryButton!!.onClick()
        verify(onRetry).invoke()
    }

    @Test
    fun `when PluginUnsupportedVersion, then returns DialogError with retry button`() {
        // GIVEN
        val state = CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.WOOCOMMERCE_PAYMENTS)

        // WHEN
        val result = sut.map(state, onDismiss, onRetry, onSkipPendingRequirements)

        // THEN
        assertThat(result).isInstanceOf(WooPosOnboardingErrorMapper.Result.DialogError::class.java)
        val error = (result as WooPosOnboardingErrorMapper.Result.DialogError).error
        assertThat(error.primaryButton).isNotNull
    }

    @Test
    fun `when WcpayNotInstalled, then returns FullScreenRequired`() {
        // GIVEN
        val state = CardReaderOnboardingState.WcpayNotInstalled

        // WHEN
        val result = sut.map(state, onDismiss, onRetry, onSkipPendingRequirements)

        // THEN
        assertThat(result).isEqualTo(WooPosOnboardingErrorMapper.Result.FullScreenRequired)
    }

    @Test
    fun `when CashOnDeliveryDisabled, then returns FullScreenRequired`() {
        // GIVEN
        val state = CardReaderOnboardingState.CashOnDeliveryDisabled(
            countryCode = "US",
            preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
            version = "1.0",
        )

        // WHEN
        val result = sut.map(state, onDismiss, onRetry, onSkipPendingRequirements)

        // THEN
        assertThat(result).isEqualTo(WooPosOnboardingErrorMapper.Result.FullScreenRequired)
    }

    @Test
    fun `when ChoosePaymentGatewayProvider, then returns FullScreenRequired`() {
        // GIVEN
        val state = CardReaderOnboardingState.ChoosePaymentGatewayProvider

        // WHEN
        val result = sut.map(state, onDismiss, onRetry, onSkipPendingRequirements)

        // THEN
        assertThat(result).isEqualTo(WooPosOnboardingErrorMapper.Result.FullScreenRequired)
    }
}
