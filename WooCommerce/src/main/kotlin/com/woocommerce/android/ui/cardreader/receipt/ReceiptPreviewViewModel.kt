package com.woocommerce.android.ui.cardreader.receipt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_EMAIL_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_EMAIL_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_CANCELED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.receipt.ReceiptEvent.PrintReceipt
import com.woocommerce.android.ui.cardreader.receipt.ReceiptEvent.SendReceipt
import com.woocommerce.android.ui.cardreader.receipt.ReceiptPreviewViewModel.ReceiptPreviewEvent.LoadUrl
import com.woocommerce.android.ui.cardreader.receipt.ReceiptPreviewViewModel.ReceiptPreviewViewState.Content
import com.woocommerce.android.ui.cardreader.receipt.ReceiptPreviewViewModel.ReceiptPreviewViewState.Loading
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.STARTED
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptPreviewViewModel
@Inject constructor(
    savedState: SavedStateHandle,
    private val tracker: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite,
) : ScopedViewModel(savedState) {
    private val args: ReceiptPreviewFragmentArgs by savedState.navArgs()

    private val viewState = MutableLiveData<ReceiptPreviewViewState>(Loading)
    val viewStateData: LiveData<ReceiptPreviewViewState> = viewState

    init {
        _event.value = LoadUrl(args.receiptUrl)
    }

    fun onReceiptLoaded() {
        viewState.value = Content
    }

    fun onPrintClicked() {
        tracker.track(RECEIPT_PRINT_TAPPED)
        triggerEvent(PrintReceipt(args.receiptUrl, "receipt-order-${args.orderId}"))
    }

    fun onSendEmailClicked() {
        launch {
            tracker.track(RECEIPT_EMAIL_TAPPED)
            triggerEvent(
                SendReceipt(
                    content = UiStringRes(
                        string.card_reader_payment_receipt_email_content,
                        listOf(UiStringText(args.receiptUrl))
                    ),
                    subject = UiStringRes(
                        string.card_reader_payment_receipt_email_subject,
                        listOf(UiStringText(selectedSite.get().name.orEmpty()))
                    ),
                    address = args.billingEmail
                )
            )
        }
    }

    fun onEmailActivityNotFound() {
        tracker.track(RECEIPT_EMAIL_FAILED)
        triggerEvent(ShowSnackbar(string.card_reader_payment_email_client_not_found))
    }

    fun onPrintResult(result: PrintJobResult) {
        tracker.track(
            when (result) {
                CANCELLED -> RECEIPT_PRINT_CANCELED
                FAILED -> RECEIPT_PRINT_FAILED
                STARTED -> RECEIPT_PRINT_SUCCESS
            }
        )
    }

    sealed class ReceiptPreviewViewState(
        val isProgressVisible: Boolean = false,
        val isContentVisible: Boolean = false
    ) {
        object Loading : ReceiptPreviewViewState(isProgressVisible = true)
        object Content : ReceiptPreviewViewState(isContentVisible = true)
    }

    sealed class ReceiptPreviewEvent : Event() {
        data class LoadUrl(val url: String) : ReceiptPreviewEvent()
    }
}
