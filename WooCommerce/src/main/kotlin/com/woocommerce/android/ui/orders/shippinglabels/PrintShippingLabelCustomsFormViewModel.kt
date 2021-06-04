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

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    lateinit var storageDirectory: File

    fun onPrintButtonClicked() {
        printJob?.cancel()
        printJob = launch {
            viewState = viewState.copy(isProgressDialogShown = true)
            val file = downloadInvoice()
            viewState = viewState.copy(isProgressDialogShown = false)
            if (!isActive) return@launch
            if (file == null) {
                triggerEvent(ShowSnackbar(R.string.shipping_label_print_customs_form_download_failed))
                return@launch
            }
            triggerEvent(PrintCustomsForm(file))
        }
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

    private suspend fun downloadInvoice(): File? {
        val file = fileUtils.createTempTimeStampedFile(
            storageDir = storageDirectory,
            prefix = "PDF",
            fileExtension = "pdf"
        ) ?: return null
        return if (fileDownloader.downloadFile(navArgs.url, file)) {
            file
        } else {
            null
        }
    }

    @Parcelize
    data class ViewState(
        val isProgressDialogShown: Boolean = false
    ) : Parcelable

    data class PrintCustomsForm(val file: File) : MultiLiveEvent.Event()
}
