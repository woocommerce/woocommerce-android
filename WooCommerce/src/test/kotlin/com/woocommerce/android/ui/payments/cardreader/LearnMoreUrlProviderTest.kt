package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.AppUrls
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider.LearnMoreUrlType.CASH_ON_DELIVERY
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LearnMoreUrlProviderTest {
    private val provider = LearnMoreUrlProvider()

    @Test
    fun `when providing learn more url for IPP, then WcPay learn more url returned`() {
        // WHEN
        val res = provider.provideLearnMoreUrlFor(IN_PERSON_PAYMENTS)

        // THEN
        assertThat(res).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
    }

    @Test
    fun `when providing learn more url for COD, then WcPay COD learn more url returned`() {
        // WHEN
        val res = provider.provideLearnMoreUrlFor(CASH_ON_DELIVERY)

        // THEN
        assertThat(res).isEqualTo(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY)
    }
}
