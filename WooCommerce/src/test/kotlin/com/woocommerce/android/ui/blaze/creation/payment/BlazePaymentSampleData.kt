package com.woocommerce.android.ui.blaze.creation.payment

import com.woocommerce.android.model.CreditCardType
import com.woocommerce.android.ui.blaze.BlazeRepository

object BlazePaymentSampleData {
    val userPaymentMethods = listOf(
        BlazeRepository.PaymentMethod(
            id = "1",
            name = "Visa 1234",
            info = BlazeRepository.PaymentMethod.PaymentMethodInfo.CreditCard(
                cardHolderName = "John Doe",
                creditCardType = CreditCardType.VISA
            )
        ),
        BlazeRepository.PaymentMethod(
            id = "2",
            name = "MasterCard 5678",
            info = BlazeRepository.PaymentMethod.PaymentMethodInfo.CreditCard(
                cardHolderName = "Jane Doe",
                creditCardType = CreditCardType.MASTERCARD
            )
        )
    )
    val paymentMethodsUrls = BlazeRepository.PaymentMethodUrls(
        formUrl = "https://example.com/form",
        successUrl = "https://example.com/success",
    )
    val paymentMethodsData = BlazeRepository.PaymentMethodsData(
        savedPaymentMethods = userPaymentMethods,
        addPaymentMethodUrls = paymentMethodsUrls
    )
}
