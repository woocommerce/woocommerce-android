package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class LearnMoreUrlProviderTest {
    private val selectedSite: SelectedSite = mock {
        on(it.get()).thenReturn(SiteModel())
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val provider = LearnMoreUrlProvider(selectedSite, appPrefsWrapper)

    @Test
    fun `given preferred plugin WcPay, when providing learn more url, then WcPay learn more url returned`() {
        // GIVEN
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(WOOCOMMERCE_PAYMENTS)

        // WHEN
        val res = provider.providerLearnMoreUrl()

        // THEN
        assertThat(res).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
    }

    @Test
    fun `given preferred plugin null, when providing learn more url, then WcPay learn more url returned`() {
        // GIVEN
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(null)

        // WHEN
        val res = provider.providerLearnMoreUrl()

        // THEN
        assertThat(res).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
    }

    @Test
    fun `given preferred plugin Stripe, when providing learn more url, then Stripe learn more url returned`() {
        // GIVEN
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(STRIPE_EXTENSION_GATEWAY)

        // WHEN
        val res = provider.providerLearnMoreUrl()

        // THEN
        assertThat(res).isEqualTo(AppUrls.STRIPE_LEARN_MORE_ABOUT_PAYMENTS)
    }
}
