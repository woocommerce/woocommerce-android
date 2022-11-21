package com.woocommerce.android.cardreader.internal.wrappers

import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith
import com.stripe.stripeterminal.external.models.PaymentMethodType as StripePaymentMethodType

class PaymentMethodTypeMapperTest {
    private val mapper = PaymentMethodTypeMapper()

    @Test
    fun `given card present payment method type, when map, then stripe card present payment method type is returned`() {
        val result = mapper.map(PaymentMethodType.CARD_PRESENT)

        assertThat(result).isEqualTo(StripePaymentMethodType.CARD_PRESENT)
    }

    @Test
    fun `given interac present payment method type, when map, then stripe interac present method type is returned`() {
        val result = mapper.map(PaymentMethodType.INTERAC_PRESENT)

        assertThat(result).isEqualTo(StripePaymentMethodType.INTERAC_PRESENT)
    }

    @Test
    fun `given unknown payment method type, when map, then exception is thrown`() {
        assertFailsWith<IllegalStateException> { mapper.map(PaymentMethodType.UNKNOWN) }
    }
}
