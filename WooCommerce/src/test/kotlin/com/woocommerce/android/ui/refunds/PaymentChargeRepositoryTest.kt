package com.woocommerce.android.ui.refunds

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentChargeApiResult
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore

@ExperimentalCoroutinesApi
class PaymentChargeRepositoryTest : BaseUnitTest() {
    private val cardPreset: WCPaymentChargeApiResult.PaymentMethodDetails.CardPresent = mock {
        on { brand }.thenReturn("visa")
        on { last4 }.thenReturn("1234")
    }
    private val paymentMethodDetails: WCPaymentChargeApiResult.PaymentMethodDetails = mock {
        on { cardPresent }.thenReturn(cardPreset)
    }
    private val apiResult: WCPaymentChargeApiResult = mock {
        on { paymentMethodDetails }.thenReturn(paymentMethodDetails)
    }

    private val ippStore: WCInPersonPaymentsStore = mock()
    private val siteModel = SiteModel().apply {
        id = 1
        siteId = 2L
        selfHostedSiteId = 3L
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val appPrefs: AppPrefs = mock()

    private val repo = PaymentChargeRepository(ippStore, selectedSite, appPrefs)

    @Test
    fun `given active plugin saved and response successful when fetching data then card details returned`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getPaymentPluginType(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(PluginType.STRIPE_EXTENSION_GATEWAY)
            whenever(
                ippStore.fetchPaymentCharge(
                    WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE,
                    siteModel,
                    chargeId
                )
            ).thenReturn(WooPayload(apiResult))

            // WHEN
            val result = repo.fetchCardDataUsedForOrderPayment(chargeId)

            // THEN
            assertThat(result).isInstanceOf(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success::class.java
            )
            assertThat((result as PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success).cardLast4)
                .isEqualTo("1234")
            assertThat(result.cardBrand).isEqualTo("visa")
        }
    }

    @Test
    fun `given active plugin saved and response notsuccessful when fetching data then error returned`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getPaymentPluginType(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(PluginType.STRIPE_EXTENSION_GATEWAY)
            whenever(
                ippStore.fetchPaymentCharge(
                    WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE,
                    siteModel,
                    chargeId
                )
            ).thenReturn(WooPayload(WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NOT_FOUND)))

            // WHEN
            val result = repo.fetchCardDataUsedForOrderPayment(chargeId)

            // THEN
            assertThat(result).isInstanceOf(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error::class.java
            )
        }
    }

    @Test
    fun `given active plugin is not saved, when fetching data, then error returned`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getPaymentPluginType(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenThrow(IllegalStateException())

            // WHEN
            val result = repo.fetchCardDataUsedForOrderPayment(chargeId)

            // THEN
            assertThat(result).isInstanceOf(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error::class.java
            )
        }
    }
}
