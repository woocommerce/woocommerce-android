package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalPaymentPreparationResolverTest {
    private val wooStore: WooCommerceStore = mock()
    private val appPrefs: AppPrefs = mock()
    private val site = SiteModel().apply {
        id = 1
        siteId = 2L
        selfHostedSiteId = 3L
    }

    private lateinit var sut: TerminalPaymentPreparationResolver

    @Before
    fun setUp() {
        sut = TerminalPaymentPreparationResolver(wooStore, appPrefs)
    }

    @Test
    fun `given AU WooPayments site, when resolving, then AU preparation is returned without checking routes`() =
        runTest {
            givenPreferredPlugin(PluginType.WOOCOMMERCE_PAYMENTS)

            val result = sut.resolve(
                countryCode = "AU",
                site = site,
                onRouteCheckFailed = {}
            )

            assertThat(result).isEqualTo(PaymentInfo.TerminalPaymentPreparation.AUSTRALIA_CARD_PRESENT)
            verify(wooStore, never()).fetchSiteRootApiRoutes(any())
        }

    @Test
    fun `given CA WooPayments site with prepare route, when resolving, then Canada preparation is returned`() =
        runTest {
            var routeCheckFailed = false
            givenPreferredPlugin(PluginType.WOOCOMMERCE_PAYMENTS)
            whenever(wooStore.fetchSiteRootApiRoutes(site)).thenReturn(WooResult(listOf(PREPARE_ROUTE)))

            val result = sut.resolve(
                countryCode = "CA",
                site = site,
                onRouteCheckFailed = { routeCheckFailed = true }
            )

            assertThat(result).isEqualTo(PaymentInfo.TerminalPaymentPreparation.CANADA_INTERAC)
            assertThat(routeCheckFailed).isFalse()
        }

    @Test
    fun `given CA WooPayments site without prepare route, when resolving, then preparation is not needed`() =
        runTest {
            var routeCheckFailed = false
            givenPreferredPlugin(PluginType.WOOCOMMERCE_PAYMENTS)
            whenever(wooStore.fetchSiteRootApiRoutes(site)).thenReturn(WooResult(emptyList()))

            val result = sut.resolve(
                countryCode = "CA",
                site = site,
                onRouteCheckFailed = { routeCheckFailed = true }
            )

            assertThat(result).isEqualTo(PaymentInfo.TerminalPaymentPreparation.NONE)
            assertThat(routeCheckFailed).isFalse()
        }

    @Test
    fun `given CA WooPayments site route check fails, when resolving, then preparation is not needed and failure is reported`() =
        runTest {
            var routeCheckFailed = false
            givenPreferredPlugin(PluginType.WOOCOMMERCE_PAYMENTS)
            whenever(wooStore.fetchSiteRootApiRoutes(site)).thenReturn(
                WooResult(WooError(WooErrorType.API_ERROR, GenericErrorType.UNKNOWN))
            )

            val result = sut.resolve(
                countryCode = "CA",
                site = site,
                onRouteCheckFailed = { routeCheckFailed = true }
            )

            assertThat(result).isEqualTo(PaymentInfo.TerminalPaymentPreparation.NONE)
            assertThat(routeCheckFailed).isTrue()
        }

    @Test
    fun `given non-WooPayments site, when resolving, then preparation is not needed and routes are not checked`() =
        runTest {
            givenPreferredPlugin(PluginType.STRIPE_EXTENSION_GATEWAY)

            val result = sut.resolve(
                countryCode = "CA",
                site = site,
                onRouteCheckFailed = {}
            )

            assertThat(result).isEqualTo(PaymentInfo.TerminalPaymentPreparation.NONE)
            verify(wooStore, never()).fetchSiteRootApiRoutes(any())
        }

    private fun givenPreferredPlugin(pluginType: PluginType) {
        whenever(appPrefs.getCardReaderPreferredPlugin(site.id, site.siteId, site.selfHostedSiteId))
            .thenReturn(pluginType)
    }

    private companion object {
        const val PREPARE_ROUTE = "/wc/v3/payments/orders/(?P<order_id>\\w+)/prepare_terminal_payment"
    }
}
