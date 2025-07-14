package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.util.FileDownloader
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PrintShippingLabelCustomsFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fileUtils: FileUtils,
    private val fileDownloader: FileDownloader
) : ScopedViewModel(savedStateHandle) {
    private var printJob: Job? = null
    private val navArgs: PrintShippingLabelCustomsFormFragmentArgs by savedState.navArgs()

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState(commercialInvoices = navArgs.invoices.toList()))
    private var viewState by viewStateData

    lateinit var storageDirectory: File

    fun onInvoicePrintButtonClicked(invoiceUrl: String) {
        downloadAndPrintInvoice(invoiceUrl)
    }

    fun onPrintButtonClicked() {
        downloadAndPrintInvoice(viewState.commercialInvoices.first())
    }

    fun onSaveForLaterClicked() {
        triggerEvent(ExitWithResult(Unit))
    }

    fun onBackButtonClicked() {
        if (!navArgs.isReprint) {
            triggerEvent(ExitWithResult(Unit))
        } else {
            triggerEvent(Exit)
        }
    }

    fun onDownloadCanceled() {
        printJob?.cancel()
    }

    private fun downloadAndPrintInvoice(invoiceUrl: String) {
        printJob?.cancel()
        printJob = launch {
            viewState = viewState.copy(isProgressDialogShown = true)
            val file = downloadInvoice(invoiceUrl)
            viewState = viewState.copy(isProgressDialogShown = false)
            if (!isActive) return@launch
            if (file == null) {
                triggerEvent(ShowSnackbar(R.string.shipping_label_print_customs_form_download_failed))
                return@launch
            }
            triggerEvent(PrintCustomsForm(file))
        }
    }

    private suspend fun downloadInvoice(invoiceUrl: String): File? {
        val file = fileUtils.createTempTimeStampedFile(
            storageDir = storageDirectory,
            prefix = "PDF",
            fileExtension = "pdf"
        ) ?: return null
        return if (fileDownloader.downloadFile(invoiceUrl, file)) {
            file
        } else {
            null
        }
    }

    @Parcelize
    data class ViewState(
        val commercialInvoices: List<String>,
        val isProgressDialogShown: Boolean = false
    ) : Parcelable {
        @IgnoredOnParcel
        val showInvoicesAsAList
            get() = commercialInvoices.size > 1
    }

    data class PrintCustomsForm(val file: File) : MultiLiveEvent.Event()
}
