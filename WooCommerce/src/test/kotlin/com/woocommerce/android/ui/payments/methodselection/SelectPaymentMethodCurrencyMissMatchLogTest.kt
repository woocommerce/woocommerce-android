package com.woocommerce.android.ui.payments.methodselection

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLogWrapper
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class SelectPaymentMethodCurrencyMissMatchLogTest {
    private val wooLogWrapper: WooLogWrapper = mock()
    private val selectPaymentMethodCurrencyMissMatchLog = SelectPaymentMethodCurrencyMissMatchLog(wooLogWrapper)

    @Test
    fun `given currencies do not match, when invoke is called, then logs warning`() {
        // GIVEN
        val storeCurrency = "USD"
        val orderCurrency = "EUR"

        // WHEN
        selectPaymentMethodCurrencyMissMatchLog(storeCurrency, orderCurrency)

        // THEN
        val expectedMessage =
            "⚠️ Order currency: EUR differs from store's currency: USD which can lead to payment methods being unavailable"
        verify(wooLogWrapper).w(WooLog.T.ORDERS, expectedMessage)
    }

    @Test
    fun `given currencies matches, when invoke is called, then does not log warning`() {
        // GIVEN
        val storeCurrency = "USD"
        val orderCurrency = "USD"

        // WHEN
        selectPaymentMethodCurrencyMissMatchLog(storeCurrency, orderCurrency)

        // THEN
        verify(wooLogWrapper, never()).w(any(), any())
    }
}
