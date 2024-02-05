package com.woocommerce.android.ui.payments.receipt.preview

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptShare
import com.woocommerce.android.ui.payments.receipt.preview.ReceiptPreviewViewModel.ReceiptPreviewViewState.Content
import com.woocommerce.android.ui.payments.receipt.preview.ReceiptPreviewViewModel.ReceiptPreviewViewState.Loading
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.STARTED
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ReceiptPreviewViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ReceiptPreviewViewModel

    private val paymentsFlowTracker: PaymentsFlowTracker = mock()
    private val paymentReceiptShare: PaymentReceiptShare = mock()

    private val savedState: SavedStateHandle = ReceiptPreviewFragmentArgs(
        receiptUrl = "testing url",
        billingEmail = "testing email",
        orderId = 999L
    ).toSavedStateHandle()

    @Before
    fun setUp() {
        viewModel = ReceiptPreviewViewModel(savedState, paymentsFlowTracker, paymentReceiptShare)
    }

    @Test
    fun `when screen shown, then loading receipt urls starts`() {
        assertThat(viewModel.event.value).isInstanceOf(LoadUrl::class.java)
    }

    @Test
    fun `when loading receipt url finishes, then content shown`() {
        viewModel.onReceiptLoaded()

        assertThat(viewModel.viewStateData.value).isInstanceOf(Content::class.java)
    }

    @Test
    fun `when progress state shown, then progress is visible and content hidden`() {
        val contentState = viewModel.viewStateData.value as Loading
        assertThat(contentState.isProgressVisible).isTrue
        assertThat(contentState.isContentVisible).isFalse
    }

    @Test
    fun `when content state shown, then progress is hidden and content visible`() {
        viewModel.onReceiptLoaded()

        val contentState = viewModel.viewStateData.value as Content
        assertThat(contentState.isProgressVisible).isFalse
        assertThat(contentState.isContentVisible).isTrue
    }

    @Test
    fun `when user clicks on send email, then event tracked`() =
        testBlocking {
            viewModel.onShareClicked()

            verify(paymentsFlowTracker).trackEmailReceiptTapped()
        }

    @Test
    fun `given sharing success, when onShareClicked, then no events emitted`() =
        testBlocking {
            // GIVEN
            whenever(paymentReceiptShare("testing url", 999L)).thenReturn(
                PaymentReceiptShare.ReceiptShareResult.Success
            )

            // WHEN
            viewModel.onShareClicked()

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(LoadUrl::class.java)
        }

    @Test
    fun `given sharing failed with file cretion, when onShareClicked, then ShowSnackbar emitted`() =
        testBlocking {
            // GIVEN
            whenever(paymentReceiptShare("testing url", 999L)).thenReturn(
                PaymentReceiptShare.ReceiptShareResult.Error.FileCreation
            )

            // WHEN
            viewModel.onShareClicked()

            // THEN
            assertThat((viewModel.event.value as MultiLiveEvent.Event.ShowSnackbar).message).isEqualTo(
                R.string.card_reader_payment_receipt_can_not_be_stored
            )
            verify(paymentsFlowTracker).trackPaymentsReceiptSharingFailed(
                PaymentReceiptShare.ReceiptShareResult.Error.FileCreation
            )
        }

    @Test
    fun `given sharing failed with file downloading, when onShareClicked, then ShowSnackbar emitted`() =
        testBlocking {
            // GIVEN
            whenever(paymentReceiptShare("testing url", 999L)).thenReturn(
                PaymentReceiptShare.ReceiptShareResult.Error.FileDownload
            )

            // WHEN
            viewModel.onShareClicked()

            // THEN
            assertThat((viewModel.event.value as MultiLiveEvent.Event.ShowSnackbar).message).isEqualTo(
                R.string.card_reader_payment_receipt_can_not_be_downloaded
            )
            verify(paymentsFlowTracker).trackPaymentsReceiptSharingFailed(
                PaymentReceiptShare.ReceiptShareResult.Error.FileDownload
            )
        }

    @Test
    fun `given sharing failed with file sharing, when onShareClicked, then ShowSnackbar emitted`() =
        testBlocking {
            // GIVEN
            val sharing = PaymentReceiptShare.ReceiptShareResult.Error.Sharing(Exception())
            whenever(paymentReceiptShare("testing url", 999L)).thenReturn(sharing)

            // WHEN
            viewModel.onShareClicked()

            // THEN
            assertThat((viewModel.event.value as MultiLiveEvent.Event.ShowSnackbar).message).isEqualTo(
                R.string.card_reader_payment_email_client_not_found
            )
            verify(paymentsFlowTracker).trackPaymentsReceiptSharingFailed(
                sharing
            )
        }

    @Test
    fun `when user clicks on print receipt, then print receipt event emitted`() =
        testBlocking {
            viewModel.onPrintClicked()

            assertThat(viewModel.event.value).isInstanceOf(PrintReceipt::class.java)
        }

    @Test
    fun `when user clicks on print receipt, then event tracked`() =
        testBlocking {
            viewModel.onPrintClicked()

            verify(paymentsFlowTracker).trackPrintReceiptTapped()
        }

    @Test
    fun `when printing receipt fails, then event tracked`() =
        testBlocking {
            viewModel.onPrintResult(FAILED)

            verify(paymentsFlowTracker).trackPrintReceiptFailed()
        }

    @Test
    fun `when user cancels printing receipt, then event tracked`() =
        testBlocking {
            viewModel.onPrintResult(CANCELLED)

            verify(paymentsFlowTracker).trackPrintReceiptCancelled()
        }

    @Test
    fun `when printing receipt succeeds, then event tracked`() =
        testBlocking {
            viewModel.onPrintResult(STARTED)

            verify(paymentsFlowTracker).trackPrintReceiptSucceeded()
        }
}
