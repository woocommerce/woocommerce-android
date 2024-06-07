package com.woocommerce.android.ui.woopos

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.IsWindowClassExpandedAndBigger
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.StoreCurrencies
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IsWooPosEnabledTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val ippStore: WCInPersonPaymentsStore = mock()
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker = mock()
    private val isWindowSizeExpandedAndBigger: IsWindowClassExpandedAndBigger = mock()
    private val isWooPosFFEnabled: IsWooPosFFEnabled = mock()
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock {
        on { invoke() }.thenReturn("6.6.0")
    }

    private lateinit var sut: IsWooPosEnabled

    @Before
    fun setup() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().also { it.id = 1 })
        val onboardingCompleted = mock<CardReaderOnboardingState.OnboardingCompleted>()
        whenever(onboardingCompleted.preferredPlugin).thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)
        whenever(cardReaderOnboardingChecker.getOnboardingState()).thenReturn(onboardingCompleted)
        whenever(isWindowSizeExpandedAndBigger()).thenReturn(true)
        whenever(ippStore.loadAccount(any(), any())).thenReturn(buildPaymentAccountResult())
        whenever(isWooPosFFEnabled()).thenReturn(true)

        sut = IsWooPosEnabled(
            selectedSite = selectedSite,
            ippStore = ippStore,
            cardReaderOnboardingChecker = cardReaderOnboardingChecker,
            isWindowSizeExpandedAndBigger = isWindowSizeExpandedAndBigger,
            isWooPosFFEnabled = isWooPosFFEnabled,
            getWooCoreVersion = getWooCoreVersion,
        )
    }

    @Test
    fun `given big enough screen, IPP Onboarding completed, USD currency, store in the US, then return true`() =
        testBlocking {
            whenever(selectedSite.getOrNull()).thenReturn(SiteModel().also { it.id = 1 })
            whenever(isWindowSizeExpandedAndBigger()).thenReturn(true)
            val onboardingCompleted = mock<CardReaderOnboardingState.OnboardingCompleted>()
            whenever(cardReaderOnboardingChecker.getOnboardingState()).thenReturn(onboardingCompleted)
            whenever(onboardingCompleted.preferredPlugin).thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)
            whenever(ippStore.loadAccount(any(), any()))
                .thenReturn(buildPaymentAccountResult(countryCode = "US", defaultCurrency = "USD"))
            whenever(isWooPosFFEnabled()).thenReturn(true)

            assertTrue(sut())
        }

    @Test
    fun `given store not in the US, then return false`() = testBlocking {
        val result = buildPaymentAccountResult(countryCode = "CA")
        whenever(ippStore.loadAccount(any(), any())).thenReturn(result)
        assertFalse(sut())
    }

    @Test
    fun `given not big enough screen, then return false`() = testBlocking {
        whenever(isWindowSizeExpandedAndBigger()).thenReturn(false)
        assertFalse(sut())
    }

    @Test
    fun `given currency is not USD, then return false`() = testBlocking {
        val result = buildPaymentAccountResult(defaultCurrency = "CAD")
        whenever(ippStore.loadAccount(any(), any())).thenReturn(result)
        assertFalse(sut())
    }

    @Test
    fun `given ipp onboarding is Completed, then return true`() = testBlocking {
        val onboardingCompleted = mock<CardReaderOnboardingState.OnboardingCompleted>()
        whenever(onboardingCompleted.preferredPlugin).thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)
        whenever(cardReaderOnboardingChecker.getOnboardingState()).thenReturn(onboardingCompleted)

        assertTrue(sut())
    }

    @Test
    fun `given ipp onboarding is Pending Requirements, then return true`() = testBlocking {
        val onboardingCompleted = mock<CardReaderOnboardingState.StripeAccountPendingRequirement>()
        whenever(onboardingCompleted.preferredPlugin).thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)
        whenever(cardReaderOnboardingChecker.getOnboardingState()).thenReturn(onboardingCompleted)

        assertTrue(sut())
    }

    @Test
    fun `given ipp onboarding is WCPayNotInstalled, then return false`() = testBlocking {
        val onboardingCompleted = CardReaderOnboardingState.WcpayNotInstalled
        whenever(cardReaderOnboardingChecker.getOnboardingState()).thenReturn(onboardingCompleted)

        assertFalse(sut())
    }

    @Test
    fun `given ipp plugin is not woo payments, then return false`() = testBlocking {
        val onboardingCompleted = mock<CardReaderOnboardingState.OnboardingCompleted>()
        whenever(onboardingCompleted.preferredPlugin).thenReturn(PluginType.STRIPE_EXTENSION_GATEWAY)
        whenever(cardReaderOnboardingChecker.getOnboardingState()).thenReturn(onboardingCompleted)

        assertFalse(sut())
    }

    @Test
    fun `given feature flag disabled, then return false`() = testBlocking {
        whenever(isWooPosFFEnabled.invoke()).thenReturn(false)
        assertFalse(sut())
    }

    @Test
    fun `given woo version 6_5_0, when invoked, then return false`() = testBlocking {
        whenever(getWooCoreVersion.invoke()).thenReturn("6.5.0")
        assertFalse(sut())
    }

    @Test
    fun `given woo version 6_6_0, when invoked, then return true`() = testBlocking {
        whenever(getWooCoreVersion.invoke()).thenReturn("6.6.0")
        assertTrue(sut())
    }

    @Test
    fun `given woo version 6_6_0_1, when invoked, then return true`() = testBlocking {
        whenever(getWooCoreVersion.invoke()).thenReturn("6.6.0.1")
        assertTrue(sut())
    }

    @Test
    fun `given woo version 10_0_1, when invoked, then return true`() = testBlocking {
        whenever(getWooCoreVersion.invoke()).thenReturn("10.0.1")
        assertTrue(sut())
    }

    private fun buildPaymentAccountResult(
        countryCode: String = "US",
        defaultCurrency: String = "USD"
    ) = WooResult(
        WCPaymentAccountResult(
            status = mock(),
            hasPendingRequirements = false,
            hasOverdueRequirements = false,
            currentDeadline = null,
            statementDescriptor = "",
            storeCurrencies = StoreCurrencies(defaultCurrency, listOf(defaultCurrency)),
            country = countryCode,
            isLive = true,
            testMode = false,
        )
    )
}
