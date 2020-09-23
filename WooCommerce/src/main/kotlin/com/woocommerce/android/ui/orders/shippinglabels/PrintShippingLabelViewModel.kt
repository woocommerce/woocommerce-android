package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewShippingLabelPaperSizes
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelPaperSizeSelectorDialog.ShippingLabelPaperSize
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class PrintShippingLabelViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    private val repository: ShippingLabelRepository,
    private val networkStatus: NetworkStatus,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    private val arguments: PrintShippingLabelFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, PrintShippingLabelViewState())
    private var viewState by viewStateData

    fun onPaperSizeOptionsSelected() {
        triggerEvent(ViewShippingLabelPaperSizes(viewState.paperSize))
    }

    fun onPaperSizeSelected(paperSize: ShippingLabelPaperSize) {
        viewState = viewState.copy(paperSize = paperSize)
    }

    fun onPrintShippingLabelClicked() {
        if (networkStatus.isConnected()) {
            viewState = viewState.copy(isProgressDialogShown = true)
            launch {
                val requestResult = repository.printShippingLabel(
                    viewState.paperSize.name.toLowerCase(Locale.US), arguments.shippingLabelId
                )

                viewState = viewState.copy(isProgressDialogShown = false)
                if (requestResult.isError) {
                    triggerEvent(ShowSnackbar(string.shipping_label_preview_error))
                } else {
                    viewState = viewState.copy(previewShippingLabel = requestResult.model)
                }
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
    }

    fun writeShippingLabelToFile(
        storageDir: File,
        shippingLabelPreview: String
    ) {
        launch(dispatchers.io) {
            val tempFile = FileUtils.createTempPdfFile(storageDir)
            FileUtils.writePdfToTempFile(tempFile, shippingLabelPreview)?.let {
                withContext(dispatchers.main) { viewState = viewState.copy(tempFile = it) }
            }
        }
    }

    @Parcelize
    data class PrintShippingLabelViewState(
        val paperSize: ShippingLabelPaperSize = ShippingLabelPaperSize.LABEL,
        val isProgressDialogShown: Boolean? = null,
        val previewShippingLabel: String? = null,
        val tempFile: File? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<PrintShippingLabelViewModel>
}
