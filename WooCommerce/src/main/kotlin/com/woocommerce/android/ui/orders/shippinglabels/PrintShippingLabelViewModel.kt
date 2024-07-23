package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewPrintCustomsForm
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewPrintShippingLabelInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewShippingLabelFormatOptions
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewShippingLabelPaperSizes
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelPaperSizeSelectorDialog.ShippingLabelPaperSize
import com.woocommerce.android.util.Base64Decoder
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PrintShippingLabelViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val repository: ShippingLabelRepository,
    private val networkStatus: NetworkStatus,
    private val fileUtils: FileUtils,
    private val base64Decoder: Base64Decoder
) : ScopedViewModel(savedState) {
    private val arguments: PrintShippingLabelFragmentArgs by savedState.navArgs()
    private val labels by lazy {
        arguments.shippingLabelIds.map {
            repository.getShippingLabelByOrderIdAndLabelId(
                orderId = arguments.orderId,
                shippingLabelId = it
            )
        }
    }

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(
        savedState,
        PrintShippingLabelViewState(
            // the case of expiring is possible only during reprinting, and when reprinting we will pass only one label
            isLabelExpired = labels.first()?.isAnonymized == true ||
                labels.first()?.expiryDate?.let { Date().after(it) } ?: false
        )
    )
    private var viewState by viewStateData

    val screenTitle: Int
        get() = if (labels.size > 1) {
            R.string.shipping_label_print_multiple_screen_title
        } else {
            R.string.shipping_label_print_screen_title
        }

    fun onPrintShippingLabelInfoSelected() {
        triggerEvent(ViewPrintShippingLabelInfo)
    }

    fun onViewLabelFormatOptionsClicked() {
        triggerEvent(ViewShippingLabelFormatOptions)
    }

    fun onPaperSizeOptionsSelected() {
        triggerEvent(ViewShippingLabelPaperSizes(viewState.paperSize))
    }

    fun onPaperSizeSelected(paperSize: ShippingLabelPaperSize) {
        viewState = viewState.copy(paperSize = paperSize)
    }

    fun onSaveForLaterClicked() {
        triggerEvent(ExitWithResult(Unit))
    }

    fun onPrintShippingLabelClicked() {
        if (networkStatus.isConnected()) {
            AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_PRINT_REQUESTED)
            viewState = viewState.copy(isProgressDialogShown = true)
            launch {
                val requestResult = repository.printShippingLabels(
                    viewState.paperSize.name.lowercase(Locale.US),
                    arguments.shippingLabelIds.toList()
                )

                viewState = viewState.copy(isProgressDialogShown = false)
                if (requestResult.isError) {
                    triggerEvent(ShowSnackbar(R.string.shipping_label_preview_error))
                } else {
                    viewState = viewState.copy(previewShippingLabel = requestResult.model)
                }
            }
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
        }
    }

    fun writeShippingLabelToFile(
        storageDir: File,
        shippingLabelPreview: String
    ) {
        launch(dispatchers.io) {
            val tempFile = fileUtils.createTempTimeStampedFile(
                storageDir = storageDir,
                prefix = "PDF",
                fileExtension = "pdf"
            )
            if (tempFile != null) {
                val content = base64Decoder.decode(shippingLabelPreview, 0)
                fileUtils.writeContentToFile(tempFile, content)?.let {
                    withContext(dispatchers.main) { viewState = viewState.copy(tempFile = it) }
                } ?: handlePreviewError()
            } else {
                handlePreviewError()
            }
        }
    }

    fun onPreviewLabelCompleted() {
        viewState = viewState.copy(tempFile = null, previewShippingLabel = null)
        labels.filter { it?.hasCommercialInvoice == true }
            .map { it!!.commercialInvoiceUrl!! }
            .takeIf { it.isNotEmpty() }
            ?.let { triggerEvent(ViewPrintCustomsForm(it, arguments.isReprint)) }
    }

    private suspend fun handlePreviewError() {
        withContext(dispatchers.main) {
            triggerEvent(ShowSnackbar(R.string.shipping_label_preview_error))
        }
    }

    @Parcelize
    data class PrintShippingLabelViewState(
        val paperSize: ShippingLabelPaperSize = ShippingLabelPaperSize.LABEL,
        val isProgressDialogShown: Boolean? = null,
        val previewShippingLabel: String? = null,
        val isLabelExpired: Boolean = false,
        val tempFile: File? = null
    ) : Parcelable
}
