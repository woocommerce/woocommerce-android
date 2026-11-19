package com.woocommerce.android.ui.woopos.receiptprinting

import app.cash.turbine.test
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardDetails
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosReceiptPrintingEnabled
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptButtonState.Hidden
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptButtonState.Idle
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptButtonState.Loading
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptButtonState.Success
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPrintReceiptCoordinatorTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val connectionStatus =
        MutableStateFlow<ReceiptPrinterConnectionStatus>(ReceiptPrinterConnectionStatus.Disconnected)
    private val receiptPrinterFacade: WooPosReceiptPrinterFacade = mock {
        on { connectionStatus } doReturn connectionStatus
    }
    private val logger: WooPosLogWrapper = mock()
    private val isReceiptPrintingEnabled: WooPosReceiptPrintingEnabled = mock {
        on { invoke() } doReturn true
    }

    @Test
    fun `given receipt printing is disabled, when the coordinator is created, then the button is hidden`() = runTest {
        // GIVEN
        whenever(isReceiptPrintingEnabled()).thenReturn(false)

        // WHEN
        val sut = createCoordinator()

        // THEN
        assertThat(sut.buttonState.value).isEqualTo(Hidden)
    }

    @Test
    fun `given receipt printing is enabled, when the coordinator is created, then the button is idle`() = runTest {
        // WHEN
        val sut = createCoordinator()

        // THEN
        assertThat(sut.buttonState.value).isEqualTo(Idle)
    }

    @Test
    fun `given a printer is connected, when print receipt is clicked, then the receipt is printed without setup`() =
        runTest {
            // GIVEN
            connectPrinter()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).thenReturn(ReceiptPrinterResult.Success)
            val sut = createCoordinator()

            sut.events.test {
                // WHEN
                sut.onPrintReceiptClicked(ORDER_ID)
                runCurrent()

                // THEN
                verify(receiptPrinterFacade).printReceipt(ORDER_ID, null)
                expectNoEvents()
            }
        }

    @Test
    fun `given no printer is connected, when print receipt is clicked, then setup is requested and nothing is printed`() =
        runTest {
            // GIVEN
            val sut = createCoordinator()

            sut.events.test {
                // WHEN
                sut.onPrintReceiptClicked(ORDER_ID)
                runCurrent()

                // THEN
                assertThat(awaitItem()).isEqualTo(WooPosPrintReceiptEvent.ShowPrinterSetup)
                assertThat(sut.buttonState.value).isEqualTo(Idle)
                verify(receiptPrinterFacade, never()).printReceipt(any(), anyOrNull())
            }
        }

    @Test
    fun `given setup was requested, when setup closes with a printer connected, then the receipt is printed`() =
        runTest {
            // GIVEN
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).thenReturn(ReceiptPrinterResult.Success)
            val sut = createCoordinator()
            sut.onPrintReceiptClicked(ORDER_ID)

            // WHEN
            connectPrinter()
            sut.onPrinterSetupClosed(ORDER_ID)
            runCurrent()

            // THEN
            verify(receiptPrinterFacade).printReceipt(ORDER_ID, null)
        }

    @Test
    fun `given setup was requested, when setup closes without a printer, then the pending print is dropped`() =
        runTest {
            // GIVEN
            val sut = createCoordinator()
            sut.onPrintReceiptClicked(ORDER_ID)

            // WHEN
            sut.onPrinterSetupClosed(ORDER_ID)
            connectPrinter()
            runCurrent()

            // THEN
            verify(receiptPrinterFacade, never()).printReceipt(any(), anyOrNull())
        }

    @Test
    fun `given the coordinator was recreated during setup, when setup closes with a printer connected, then the receipt is printed`() =
        runTest {
            // GIVEN
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).thenReturn(ReceiptPrinterResult.Success)
            createCoordinator().onPrintReceiptClicked(ORDER_ID)
            val recreated = createCoordinator()

            // WHEN
            connectPrinter()
            recreated.onPrinterSetupClosed(ORDER_ID)
            runCurrent()

            // THEN
            verify(receiptPrinterFacade).printReceipt(ORDER_ID, null)
        }

    @Test
    fun `given a printer is connected, when the receipt is printing, then the button is loading`() = runTest {
        // GIVEN
        connectPrinter()
        whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).doSuspendableAnswer {
            CompletableDeferred<ReceiptPrinterResult>().await()
        }
        val sut = createCoordinator()

        // WHEN
        sut.onPrintReceiptClicked(ORDER_ID)
        runCurrent()

        // THEN
        assertThat(sut.buttonState.value).isEqualTo(Loading)
    }

    @Test
    fun `given the receipt is printing, when print receipt is clicked again, then it is printed once`() = runTest {
        // GIVEN
        connectPrinter()
        whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).doSuspendableAnswer {
            CompletableDeferred<ReceiptPrinterResult>().await()
        }
        val sut = createCoordinator()
        sut.onPrintReceiptClicked(ORDER_ID)
        runCurrent()

        // WHEN
        sut.onPrintReceiptClicked(ORDER_ID)
        runCurrent()

        // THEN
        verify(receiptPrinterFacade, times(1)).printReceipt(ORDER_ID, null)
    }

    @Test
    fun `given a printer is connected, when the receipt is printed, then the button shows success for 2 seconds`() =
        runTest {
            // GIVEN
            connectPrinter()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).thenReturn(ReceiptPrinterResult.Success)
            val sut = createCoordinator()

            // WHEN
            sut.onPrintReceiptClicked(ORDER_ID)
            runCurrent()

            // THEN
            assertThat(sut.buttonState.value).isEqualTo(Success)
            advanceTimeBy(1_999)
            assertThat(sut.buttonState.value).isEqualTo(Success)
            advanceTimeBy(2)
            assertThat(sut.buttonState.value).isEqualTo(Idle)
        }

    @Test
    fun `given a printer is connected, when printing fails, then the failure is reported and the button is idle`() =
        runTest {
            // GIVEN
            connectPrinter()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null))
                .thenReturn(ReceiptPrinterResult.Failure(ReceiptPrinterError.PaperEmpty))
            val sut = createCoordinator()

            sut.events.test {
                // WHEN
                sut.onPrintReceiptClicked(ORDER_ID)
                runCurrent()

                // THEN
                assertThat(awaitItem())
                    .isEqualTo(WooPosPrintReceiptEvent.PrintFailed(ORDER_ID, WooPosPrintReceiptFailure.PaperEmpty))
                verify(logger).e("Printing the receipt failed: PaperEmpty")
                assertThat(sut.buttonState.value).isEqualTo(Idle)
            }
        }

    @Test
    fun `given printing failed and the printer is still connected, when print receipt is clicked, then it is printed again`() =
        runTest {
            // GIVEN
            connectPrinter()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null))
                .thenReturn(ReceiptPrinterResult.Failure(ReceiptPrinterError.PaperEmpty))
            val sut = createCoordinator()
            sut.onPrintReceiptClicked(ORDER_ID)
            runCurrent()

            // WHEN
            sut.onPrintReceiptClicked(ORDER_ID)
            runCurrent()

            // THEN
            verify(receiptPrinterFacade, times(2)).printReceipt(ORDER_ID, null)
        }

    @Test
    fun `given printing failed because the connection was lost, when print receipt is clicked, then setup is requested`() =
        runTest {
            // GIVEN
            connectPrinter()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).doSuspendableAnswer {
                connectionStatus.value = ReceiptPrinterConnectionStatus.Disconnected
                ReceiptPrinterResult.Failure(ReceiptPrinterError.ConnectionLost())
            }
            val sut = createCoordinator()

            sut.events.test {
                sut.onPrintReceiptClicked(ORDER_ID)
                runCurrent()
                assertThat(awaitItem())
                    .isEqualTo(WooPosPrintReceiptEvent.PrintFailed(ORDER_ID, WooPosPrintReceiptFailure.PrinterOffline))

                // WHEN
                sut.onPrintReceiptClicked(ORDER_ID)
                runCurrent()

                // THEN
                assertThat(awaitItem()).isEqualTo(WooPosPrintReceiptEvent.ShowPrinterSetup)
                verify(receiptPrinterFacade, times(1)).printReceipt(ORDER_ID, null)
            }
        }

    @Test
    fun `given the receipt is printing, when email receipt is opened and printing fails, then the failure is not reported`() =
        runTest {
            // GIVEN
            connectPrinter()
            val printResult = CompletableDeferred<ReceiptPrinterResult>()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).doSuspendableAnswer { printResult.await() }
            val sut = createCoordinator()

            sut.events.test {
                sut.onPrintReceiptClicked(ORDER_ID)
                runCurrent()

                // WHEN
                sut.onEmailReceiptOpened()
                printResult.complete(ReceiptPrinterResult.Failure(ReceiptPrinterError.PaperEmpty))
                runCurrent()

                // THEN
                expectNoEvents()
                assertThat(sut.buttonState.value).isEqualTo(Idle)
            }
        }

    @Test
    fun `given a failure was ignored after email receipt was opened, when the next print fails, then the failure is reported`() =
        runTest {
            // GIVEN
            connectPrinter()
            val printResult = CompletableDeferred<ReceiptPrinterResult>()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).doSuspendableAnswer { printResult.await() }
            val sut = createCoordinator()

            sut.events.test {
                sut.onPrintReceiptClicked(ORDER_ID)
                runCurrent()
                sut.onEmailReceiptOpened()
                printResult.complete(ReceiptPrinterResult.Failure(ReceiptPrinterError.PaperEmpty))
                runCurrent()
                expectNoEvents()

                // WHEN
                sut.onPrintReceiptClicked(ORDER_ID)
                runCurrent()

                // THEN
                assertThat(awaitItem())
                    .isEqualTo(WooPosPrintReceiptEvent.PrintFailed(ORDER_ID, WooPosPrintReceiptFailure.PaperEmpty))
                verify(receiptPrinterFacade, times(2)).printReceipt(ORDER_ID, null)
            }
        }

    @Test
    fun `given the order was paid by card, when its receipt is printed, then the card details are printed`() =
        runTest {
            // GIVEN
            connectPrinter()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, CARD_DETAILS)).thenReturn(ReceiptPrinterResult.Success)
            val sut = createCoordinator()
            sut.onOrderPaidByCard(ORDER_ID, CARD_DETAILS)

            // WHEN
            sut.onPrintReceiptClicked(ORDER_ID)
            runCurrent()

            // THEN
            verify(receiptPrinterFacade).printReceipt(ORDER_ID, CARD_DETAILS)
        }

    @Test
    fun `given another order was paid by card, when a receipt is printed, then no card details are printed`() =
        runTest {
            // GIVEN
            connectPrinter()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).thenReturn(ReceiptPrinterResult.Success)
            val sut = createCoordinator()
            sut.onOrderPaidByCard(OTHER_ORDER_ID, CARD_DETAILS)

            // WHEN
            sut.onPrintReceiptClicked(ORDER_ID)
            runCurrent()

            // THEN
            verify(receiptPrinterFacade).printReceipt(ORDER_ID, null)
        }

    @Test
    fun `given the order was paid by card without card details, when its receipt is printed, then no card details are printed`() =
        runTest {
            // GIVEN
            connectPrinter()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, null)).thenReturn(ReceiptPrinterResult.Success)
            val sut = createCoordinator()
            sut.onOrderPaidByCard(ORDER_ID, CARD_DETAILS)
            sut.onOrderPaidByCard(ORDER_ID, null)

            // WHEN
            sut.onPrintReceiptClicked(ORDER_ID)
            runCurrent()

            // THEN
            verify(receiptPrinterFacade).printReceipt(ORDER_ID, null)
        }

    @Test
    fun `given a card receipt was printed, when it is printed again, then the card details are printed again`() =
        runTest {
            // GIVEN
            connectPrinter()
            whenever(receiptPrinterFacade.printReceipt(ORDER_ID, CARD_DETAILS)).thenReturn(ReceiptPrinterResult.Success)
            val sut = createCoordinator()
            sut.onOrderPaidByCard(ORDER_ID, CARD_DETAILS)
            sut.onPrintReceiptClicked(ORDER_ID)
            advanceTimeBy(3_000)

            // WHEN
            sut.onPrintReceiptClicked(ORDER_ID)
            runCurrent()

            // THEN
            verify(receiptPrinterFacade, times(2)).printReceipt(ORDER_ID, CARD_DETAILS)
        }

    private fun connectPrinter() {
        connectionStatus.value = ReceiptPrinterConnectionStatus.Connected(
            ReceiptPrinterDevice(id = "1", name = "Star mC-Print3")
        )
    }

    private fun TestScope.createCoordinator() = WooPosPrintReceiptCoordinator(
        receiptPrinterFacade = receiptPrinterFacade,
        logger = logger,
        isReceiptPrintingEnabled = isReceiptPrintingEnabled,
        scope = backgroundScope,
    )

    private companion object {
        const val ORDER_ID = 123L
        const val OTHER_ORDER_ID = 456L
        val CARD_DETAILS = CardDetails(brand = "visa", last4 = "4242")
    }
}
