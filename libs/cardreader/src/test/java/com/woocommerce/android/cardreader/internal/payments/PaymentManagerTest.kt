package com.woocommerce.android.cardreader.internal.payments

import com.nhaarman.mockitokotlin2.mock
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PaymentManagerTest {
    private lateinit var manager: PaymentManager
    private val cardReaderStore: CardReaderStore = mock()
    private val createPaymentAction: CreatePaymentAction = mock()
    private val collectPaymentAction: CollectPaymentAction = mock()
    private val processPaymentAction: ProcessPaymentAction = mock()

    @Before
    fun setUp() {
        manager = PaymentManager(cardReaderStore, createPaymentAction, collectPaymentAction, processPaymentAction)
    }
}
