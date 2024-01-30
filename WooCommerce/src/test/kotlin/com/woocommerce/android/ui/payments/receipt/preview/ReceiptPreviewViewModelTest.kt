package com.woocommerce.android.ui.payments.receipt.preview

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_EMAIL_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_CANCELED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptShare
import com.woocommerce.android.ui.payments.receipt.preview.ReceiptPreviewViewModel.ReceiptPreviewViewState.Content
import com.woocommerce.android.ui.payments.receipt.preview.ReceiptPreviewViewModel.ReceiptPreviewViewState.Loading
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.STARTED
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class ReceiptPreviewViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ReceiptPreviewViewModel

    private val selectedSite: SelectedSite = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val paymentsFlowTracker: PaymentsFlowTracker = mock()
    private val paymentReceiptShare: PaymentReceiptShare = mock()

    private val savedState: SavedStateHandle = ReceiptPreviewFragmentArgs(
        receiptUrl = "testing url",
        billingEmail = "testing email",
        orderId = 999L
    ).toSavedStateHandle()

    @Before
    fun setUp() {
        viewModel = ReceiptPreviewViewModel(savedState, tracker, paymentsFlowTracker, paymentReceiptShare)
        whenever(selectedSite.get()).thenReturn(SiteModel().apply { name = "testName" })
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

            verify(tracker).track(RECEIPT_EMAIL_TAPPED)
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

            verify(tracker).track(RECEIPT_PRINT_TAPPED)
        }

    @Test
    fun `when printing receipt fails, then event tracked`() =
        testBlocking {
            viewModel.onPrintResult(FAILED)

            verify(tracker).track(RECEIPT_PRINT_FAILED)
        }

    @Test
    fun `when user cancels printing receipt, then event tracked`() =
        testBlocking {
            viewModel.onPrintResult(CANCELLED)

            verify(tracker).track(RECEIPT_PRINT_CANCELED)
        }

    @Test
    fun `when printing receipt succeeds, then event tracked`() =
        testBlocking {
            viewModel.onPrintResult(STARTED)

            verify(tracker).track(RECEIPT_PRINT_SUCCESS)
        }
}
