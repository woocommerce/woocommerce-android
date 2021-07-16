package com.woocommerce.android.ui.prefs.cardreader.onboarding

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
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

    private fun buildPaymentAccountResult(
        status: WCPaymentAccountResult.WCPayAccountStatusEnum = WCPaymentAccountResult.WCPayAccountStatusEnum.COMPLETE
    ) = WooResult(
        WCPaymentAccountResult(
            status,
            hasPendingRequirements = false,
            hasOverdueRequirements = false,
            currentDeadline = null,
            statementDescriptor = "",
            storeCurrencies = WCPaymentAccountResult.WCPayAccountStatusEnum.StoreCurrencies("", listOf()),
            country = "US",
            isCardPresentEligible = true
        )
    )
}
