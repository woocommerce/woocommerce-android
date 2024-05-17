package com.woocommerce.android.ui.woopos

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.util.IsWindowClassExpandedAndBigger
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.COMPLETE
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.StoreCurrencies
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IsWooPosEnabledTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val ippStore: WCInPersonPaymentsStore = mock()
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin = mock()
    private val isWindowSizeExpandedAndBigger: IsWindowClassExpandedAndBigger = mock()
    private val isWooPosFFEnabled: IsWooPosFFEnabled = mock()

    private lateinit var sut: IsWooPosEnabled

    @Before
    fun setup() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel())
        whenever(getActivePaymentsPlugin()).thenReturn(WOOCOMMERCE_PAYMENTS)
        whenever(isWindowSizeExpandedAndBigger()).thenReturn(true)
        whenever(ippStore.loadAccount(any(), any())).thenReturn(buildPaymentAccountResult())
        whenever(isWooPosFFEnabled()).thenReturn(true)

        sut = IsWooPosEnabled(
            selectedSite = selectedSite,
            ippStore = ippStore,
            getActivePaymentsPlugin = getActivePaymentsPlugin,
            isWindowSizeExpandedAndBigger = isWindowSizeExpandedAndBigger,
            isWooPosFFEnabled = isWooPosFFEnabled,
        )
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
    fun `given ipp plugin is not enabled, then return false`() = testBlocking {
        whenever(getActivePaymentsPlugin()).thenReturn(null)
        assertFalse(sut())
    }

    @Test
    fun `given ipp plugin is not woo payments, then return false`() = testBlocking {
        whenever(getActivePaymentsPlugin()).thenReturn(STRIPE)
        assertFalse(sut())
    }

    @Test
    fun `given feature flag disabled, then return false`() = testBlocking {
        whenever(isWooPosFFEnabled.invoke()).thenReturn(false)
        assertFalse(sut())
    }

    @Test
    fun `given big enough screen, woo payments enabled, USD currency and store in the US, then return true`() = testBlocking {
        val result = buildPaymentAccountResult(defaultCurrency = "USD", countryCode = "US", status = COMPLETE)
        whenever(ippStore.loadAccount(any(), any())).thenReturn(result)
        assertTrue(sut())
    }

    private fun buildPaymentAccountResult(
        status: WCPaymentAccountResult.WCPaymentAccountStatus = COMPLETE,
        countryCode: String = "US",
        defaultCurrency: String = "USD"
    ) = WooResult(
        WCPaymentAccountResult(
            status,
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
