package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGatewayStore

class CashOnDeliverySettingsRepositoryTest {
    private val selectedSite: SelectedSite = mock {
        on(it.get()).thenReturn(SiteModel())
    }
    private val gatewayStore: WCGatewayStore = mock()
    private val cashOnDeliverySettingsRepository = CashOnDeliverySettingsRepository(
        gatewayStore,
        selectedSite
    )

    @Test
    fun `when cod enabled, then return true when queried for cod status`() {
        runTest {
            whenever(gatewayStore.fetchAllGateways(selectedSite.get())).thenReturn(
                getSuccessWooResult("cod", true)
            )
            assertThat(cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()).isTrue
        }
    }

    @Test
    fun `when cod disabled, then return false when queried for cod status`() {
        runTest {
            whenever(gatewayStore.fetchAllGateways(selectedSite.get())).thenReturn(
                getSuccessWooResult("cod", false)
            )
            assertThat(cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()).isFalse
        }
    }

    @Test
    fun `when cod payment type is not present, then return false when queried for cod status`() {
        runTest {
            whenever(gatewayStore.fetchAllGateways(selectedSite.get())).thenReturn(
                getSuccessWooResult("cheque", true)
            )
            assertThat(cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()).isFalse
        }
    }

    @Test
    fun `when api result is error, then return false when queried for cod status`() {
        runTest {
            whenever(gatewayStore.fetchAllGateways(selectedSite.get())).thenReturn(getFailureWooResult())
            assertThat(cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()).isFalse
        }
    }

    private fun getSuccessWooResult(paymentType: String, isPaymentTypeEnabled: Boolean) = WooResult(
        model = listOf(
            WCGatewayModel(
                id = paymentType,
                title = "",
                description = "",
                order = 0,
                isEnabled = isPaymentTypeEnabled,
                methodTitle = "",
                methodDescription = "",
                features = emptyList()
            )
        )
    )

    private fun getFailureWooResult() = WooResult<List<WCGatewayModel>>(
        error = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = BaseRequest.GenericErrorType.NETWORK_ERROR,
            message = "Enabling COD failed. Please try again later"
        )
    )
}
