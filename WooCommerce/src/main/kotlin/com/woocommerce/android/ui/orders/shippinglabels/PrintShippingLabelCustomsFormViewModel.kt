package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.parcelize.Parcelize
import java.io.File
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class PrintShippingLabelCustomsFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedStateHandle) {
    private var printJob: Job? = null
    private val navArgs: PrintShippingLabelCustomsFormFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    lateinit var storageDirectory: File

    fun onPrintButtonClicked() {
        printJob = launch {
            viewState = viewState.copy(isProgressDialogShown = true)
            val file = downloadFile(navArgs.url)
            viewState = viewState.copy(isProgressDialogShown = false)
            if (!isActive) return@launch
            if (file == null) {
                // TODO triggerEvent(ShowSnackbar(R.string.))
                return@launch
            }
            triggerEvent(PrintCustomsForm(file))
        }
    }

    fun onSaveForLaterClicked() {
        triggerEvent(Exit)
    }

    fun onDownloadCanceled() {
        printJob?.cancel()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadFile(url: String): File? = runInterruptible(dispatchers.io) {
        try {
            val file = FileUtils.createTempPDFFile(storageDirectory) ?: return@runInterruptible null
            if (file.exists()) file.delete()
            URL(url).openConnection().inputStream.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file
        } catch (e: Exception) {
            WooLog.e(T.ORDERS, "Downloading commercial invoice failed", e)
            null
        }
    }

    @Parcelize
    data class ViewState(
        val isProgressDialogShown: Boolean = false
    ) : Parcelable

    data class PrintCustomsForm(val file: File) : MultiLiveEvent.Event()
}
