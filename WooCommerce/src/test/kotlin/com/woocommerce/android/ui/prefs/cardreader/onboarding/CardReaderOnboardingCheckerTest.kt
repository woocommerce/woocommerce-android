package com.woocommerce.android.ui.prefs.cardreader.onboarding

import com.nhaarman.mockitokotlin2.*
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pay.WCPaymentAccountResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCPayStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class CardReaderOnboardingCheckerTest : BaseUnitTest() {
    private lateinit var checker: CardReaderOnboardingChecker

    private val selectedSite: SelectedSite = mock()
    private val wooStore: WooCommerceStore = mock()
    private val wcPayStore: WCPayStore = mock()

    private val site = SiteModel()

    @Before
    fun setUp() = testBlocking {
        checker = CardReaderOnboardingChecker(selectedSite, wooStore, wcPayStore, coroutinesTestRule.testDispatchers)
        whenever(selectedSite.get()).thenReturn(site)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")
        whenever(wcPayStore.loadAccount(site)).thenReturn(buildPaymentAccountResult())
    }

    @Test
    fun `when store country not supported, then COUNTRY_NOT_SUPPORTED returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("unsupported country abc")

        val result = checker.getOnboardingState()

        assertThat(result).isEqualTo(CardReaderOnboardingState.COUNTRY_NOT_SUPPORTED)
    }

    @Test
    fun `when store country supported, then COUNTRY_NOT_SUPPORTED not returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")

        val result = checker.getOnboardingState()

        assertThat(result).isNotEqualTo(CardReaderOnboardingState.COUNTRY_NOT_SUPPORTED)
    }

    @Test
    fun `given country code in lower case, when store country supported, then COUNTRY_NOT_SUPPORTED not returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("us")

            val result = checker.getOnboardingState()

            assertThat(result).isNotEqualTo(CardReaderOnboardingState.COUNTRY_NOT_SUPPORTED)
        }

    @Test
    fun `when country code is not found, then COUNTRY_NOT_SUPPORTED returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn(null)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.COUNTRY_NOT_SUPPORTED)
        }

    @Test
    fun `given country not supported, then stripe account loading does not even start`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("unsupported country abc")

            checker.getOnboardingState()

            verify(wcPayStore, never()).loadAccount(anyOrNull())
        }

    @Test
    fun `when stripe account not connected, then WCPAY_SETUP_NOT_COMPLETED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.NO_ACCOUNT
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WCPAY_SETUP_NOT_COMPLETED)
        }

    @Test
    fun `when stripe account under review, then WCPAY_SETUP_NOT_COMPLETED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.STRIPE_ACCOUNT_UNDER_REVIEW)
        }

    @Test
    fun `when stripe account pending requirements, then STRIPE_ACCOUNT_PENDING_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = true,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.STRIPE_ACCOUNT_PENDING_REQUIREMENT)
        }

    @Test
    fun `when stripe account has overdue requirements, then STRIPE_ACCOUNT_OVERDUE_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = true
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.STRIPE_ACCOUNT_OVERDUE_REQUIREMENT)
        }

    @Test
    fun `when stripe account marked as fraud, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_FRAUD,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.STRIPE_ACCOUNT_REJECTED)
        }

    @Test
    fun `when stripe account listed, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_LISTED,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.STRIPE_ACCOUNT_REJECTED)
        }

    @Test
    fun `when stripe account violates terms of service, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_TERMS_OF_SERVICE,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.STRIPE_ACCOUNT_REJECTED)
        }

    @Test
    fun `when stripe account rejected for other reasons, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcPayStore.loadAccount(site)).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_OTHER,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.STRIPE_ACCOUNT_REJECTED)
        }

    private fun buildPaymentAccountResult(
        status: WCPaymentAccountResult.WCPayAccountStatusEnum = WCPaymentAccountResult.WCPayAccountStatusEnum.COMPLETE,
        hasPendingRequirements: Boolean = false,
        hadOverdueRequirements: Boolean = false
    ) = WooResult(
        WCPaymentAccountResult(
            status,
            hasPendingRequirements = hasPendingRequirements,
            hasOverdueRequirements = hadOverdueRequirements,
            currentDeadline = null,
            statementDescriptor = "",
            storeCurrencies = WCPaymentAccountResult.WCPayAccountStatusEnum.StoreCurrencies("", listOf()),
            country = "US",
            isCardPresentEligible = true
        )
    )
}
