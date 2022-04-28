package com.woocommerce.android.ui.refunds

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.cardreader.onboarding.PreferredPluginResult
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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
    private val cardDetails: WCPaymentChargeApiResult.PaymentMethodDetails.CardDetails = mock {
        on { brand }.thenReturn("visa")
        on { last4 }.thenReturn("1234")
    }
    private val paymentMethodDetails: WCPaymentChargeApiResult.PaymentMethodDetails = mock {
        on { cardDetails }.thenReturn(cardDetails)
        on { interacCardDetails }.thenReturn(cardDetails)
        on { type }.thenReturn("card_present")
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
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker = mock()

    private val repo = PaymentChargeRepository(ippStore, selectedSite, appPrefs, cardReaderOnboardingChecker)

    @Test
    fun `given active plugin saved and card response successful, when fetching data, then card details returned`() {
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
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
    fun `given active plugin saved and interac response successful, when fetching data, then card details returned`() {
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(PluginType.STRIPE_EXTENSION_GATEWAY)
            whenever(cardDetails.last4).thenReturn("2345")
            whenever(cardDetails.brand).thenReturn("mastercard")
            whenever(paymentMethodDetails.type).thenReturn("interac_present")
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
                .isEqualTo("2345")
            assertThat(result.cardBrand).isEqualTo("mastercard")
        }
    }

    @Test
    fun `given active plugin saved and unknown response successful, when fetching data, then card details null`() {
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(PluginType.STRIPE_EXTENSION_GATEWAY)
            whenever(paymentMethodDetails.type).thenReturn("unknown")
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
            assertThat((result as PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success).cardLast4).isNull()
            assertThat(result.cardBrand).isNull()
        }
    }

    @Test
    fun `given active plugin saved and response not successful, when fetching data, then error returned`() {
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
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
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(null)

            // WHEN
            val result = repo.fetchCardDataUsedForOrderPayment(chargeId)

            // THEN
            assertThat(result).isInstanceOf(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error::class.java
            )
        }
    }

    @Test
    fun `given interac transaction, when fetching data, then card details returned`() {
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(PluginType.STRIPE_EXTENSION_GATEWAY)
            whenever(cardDetails.last4).thenReturn("2345")
            whenever(cardDetails.brand).thenReturn("mastercard")
            whenever(paymentMethodDetails.type).thenReturn("interac_present")
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
            assertThat((result as PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success).paymentMethodType)
                .isEqualTo("interac_present")
        }
    }

    @Test
    fun `given non-interac transaction, when fetching data, then card details returned`() {
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(PluginType.STRIPE_EXTENSION_GATEWAY)
            whenever(cardDetails.last4).thenReturn("2345")
            whenever(cardDetails.brand).thenReturn("mastercard")
            whenever(paymentMethodDetails.type).thenReturn("card_present")
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
            assertThat((result as PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success).paymentMethodType)
                .isEqualTo("card_present")
        }
    }

    @Test
    fun `given unknown transaction, when fetching data, then card details returned`() {
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(PluginType.STRIPE_EXTENSION_GATEWAY)
            whenever(paymentMethodDetails.type).thenReturn("unknown")
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
            assertThat((result as PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success).paymentMethodType)
                .isNull()
        }
    }

    @Test
    fun `given missing active plugin, when fetching one with error, then error returned`() {
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(null)
            whenever(cardReaderOnboardingChecker.fetchPreferredPlugin()).thenReturn(PreferredPluginResult.Error)

            // WHEN
            val result = repo.fetchCardDataUsedForOrderPayment(chargeId)

            // THEN
            assertThat(result).isInstanceOf(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error::class.java
            )
        }
    }

    @Test
    fun `given missing active plugin, when fetching one with success stripe, then returned plugin is used`() {
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(null)
            whenever(cardReaderOnboardingChecker.fetchPreferredPlugin()).thenReturn(
                PreferredPluginResult.Success(PluginType.STRIPE_EXTENSION_GATEWAY)
            )
            whenever(
                ippStore.fetchPaymentCharge(
                    WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE,
                    siteModel,
                    chargeId
                )
            ).thenReturn(WooPayload(apiResult))

            // WHEN
            repo.fetchCardDataUsedForOrderPayment(chargeId)

            // THEN
            verify(ippStore).fetchPaymentCharge(
                WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE,
                siteModel,
                chargeId
            )
        }
    }

    @Test
    fun `given missing active plugin, when fetching one with success wcpay, then returned plugin is used`() {
        testBlocking {
            // GIVEN
            val chargeId = "charge_id"
            whenever(appPrefs.getCardReaderPreferredPlugin(siteModel.id, siteModel.siteId, siteModel.selfHostedSiteId))
                .thenReturn(null)
            whenever(cardReaderOnboardingChecker.fetchPreferredPlugin()).thenReturn(
                PreferredPluginResult.Success(PluginType.WOOCOMMERCE_PAYMENTS)
            )
            whenever(
                ippStore.fetchPaymentCharge(
                    WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS,
                    siteModel,
                    chargeId
                )
            ).thenReturn(WooPayload(apiResult))

            // WHEN
            repo.fetchCardDataUsedForOrderPayment(chargeId)

            // THEN
            verify(ippStore).fetchPaymentCharge(
                WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS,
                siteModel,
                chargeId
            )
        }
    }
}
