package com.woocommerce.android.cardreader.internal.wrappers

import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DoNotMockDataClass")
class TerminalWrapperPaymentCancellationTest : CardReaderBaseUnitTest() {
    @Test
    fun `given SDK start throws synchronously, when the next command starts, then the reader gate is available`() = testBlocking {
        // GIVEN
        val terminal: Terminal = mock()
        val handle: Cancelable = mock()
        val intent: PaymentIntent = mock()
        whenever(terminal.processPaymentIntent(any(), any(), any(), any()))
            .thenThrow(IllegalStateException("Start failed"))
            .thenAnswer {
                it.getArgument<PaymentIntentCallback>(3).onSuccess(intent)
                handle
            }
        val wrapper = TerminalWrapper { terminal }

        // WHEN
        val failed = runCatching { wrapper.processPaymentIntent(intent) }
        val next = wrapper.processPaymentIntent(intent)

        // THEN
        assertThat(failed.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(next).isSameAs(intent)
        assertThat(wrapper.cancelPaymentCommandAndAwait()).isTrue()
    }

    @Test
    fun `given SDK cancellation succeeds, when the payment callback is pending, then cancellation still waits`() =
        testBlocking {
            // GIVEN
            val terminal: Terminal = mock()
            val handle: Cancelable = mock()
            val intent: PaymentIntent = mock()
            lateinit var paymentCallback: PaymentIntentCallback
            lateinit var cancelCallback: Callback
            whenever(terminal.processPaymentIntent(any(), any(), any(), any())).thenAnswer {
                paymentCallback = it.getArgument(3)
                handle
            }
            doAnswer {
                cancelCallback = it.getArgument(0)
                null
            }.whenever(handle).cancel(any())

            run {
                val wrapper = TerminalWrapper { terminal }
                val processing = async { runCatching { wrapper.processPaymentIntent(intent) } }
                val cancellation = async { wrapper.cancelPaymentCommandAndAwait() }

                // WHEN
                cancelCallback.onSuccess()

                // THEN
                assertThat(cancellation.isCompleted).isFalse()

                // WHEN
                paymentCallback.onFailure(TerminalException(TerminalErrorCode.CANCELED, "Cancelled"))

                // THEN
                assertThat(cancellation.await()).isTrue()
                assertThat(processing.await().isFailure).isTrue()
            }
        }

    @Test
    fun `given SDK cancellation fails, when the payment command is still active, then it remains owned`() =
        testBlocking {
            // GIVEN
            val terminal: Terminal = mock()
            val handle: Cancelable = mock()
            val intent: PaymentIntent = mock()
            lateinit var paymentCallback: PaymentIntentCallback
            whenever(terminal.processPaymentIntent(any(), any(), any(), any())).thenAnswer {
                paymentCallback = it.getArgument(3)
                handle
            }
            doAnswer {
                it.getArgument<Callback>(0).onFailure(TerminalException(TerminalErrorCode.CANCEL_FAILED, "Unconfirmed"))
                null
            }.whenever(handle).cancel(any())

            run {
                val wrapper = TerminalWrapper { terminal }
                val processing = async { runCatching { wrapper.processPaymentIntent(intent) } }

                // THEN
                assertThat(wrapper.cancelPaymentCommandAndAwait()).isFalse()
                assertThat(processing.isCompleted).isFalse()

                // WHEN
                val another = runCatching { wrapper.processPaymentIntent(intent) }

                // THEN
                assertThat((another.exceptionOrNull() as TerminalException).errorCode)
                    .isEqualTo(TerminalErrorCode.READER_BUSY)
                paymentCallback.onFailure(TerminalException(TerminalErrorCode.CANCELED, "Cancelled"))
                processing.await()
            }
        }
}
