package com.woocommerce.android.ui.barcodescanner

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.ScanningSource
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.plus
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import javax.inject.Inject
import javax.inject.Singleton

private const val MIN_SKU_LENGTH = 3

@Singleton
class HardwareBarcodeScanner @Inject constructor(
    @AppCoroutineScope appCoroutineScope: CoroutineScope,
    dispatchers: CoroutineDispatchers,
    currentTimeProvider: CurrentTimeProvider,
    private val barcodeScanningTracker: BarcodeScanningTracker,
) {
    private var listener: Listener? = null

    private val _scansWithoutEnterSuffix = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scansWithoutEnterSuffix: SharedFlow<Unit> = _scansWithoutEnterSuffix.asSharedFlow()

    private val detector = BarcodeInputDetector(
        onBarcodeEvent = ::onBarcodeEvent,
        currentTimeProvider = currentTimeProvider,
        coroutineScope = appCoroutineScope + dispatchers.main,
        minBarcodeLength = MIN_SKU_LENGTH,
    )

    fun startListening(source: ScanningSource, onBarcodeScanned: (CodeScannerStatus.Success) -> Unit) {
        detector.clear()
        listener = Listener(source, onBarcodeScanned)
    }

    fun stopListening(onBarcodeScanned: (CodeScannerStatus.Success) -> Unit) {
        if (listener?.onBarcodeScanned === onBarcodeScanned) {
            detector.clear()
            listener = null
        }
    }

    fun onKeyDown(unicodeChar: Int, isTextInputFocused: Boolean): Boolean {
        return when {
            listener == null || isTextInputFocused || !unicodeChar.isBarcodeInputChar() -> false
            unicodeChar.toChar().isWhitespace() && !detector.hasPendingInput -> false
            else -> detector.handleKeyInput(unicodeChar.toChar())
        }
    }

    private fun onBarcodeEvent(result: BarcodeInputDetector.BarcodeResult) {
        when (result) {
            is BarcodeInputDetector.BarcodeResult.Success -> listener?.let { listener ->
                barcodeScanningTracker.trackHardwareScanSuccess(listener.source)
                listener.onBarcodeScanned(
                    CodeScannerStatus.Success(code = result.barcode, format = result.barcode.inferBarcodeFormat())
                )
            }
            is BarcodeInputDetector.BarcodeResult.Error -> onBarcodeError(result)
        }
    }

    private fun onBarcodeError(error: BarcodeInputDetector.BarcodeResult.Error) {
        if (error.isMissingEnterSuffix()) {
            _scansWithoutEnterSuffix.tryEmit(Unit)
        }
    }

    private fun String.inferBarcodeFormat(): BarcodeFormat = when {
        !all { it in '0'..'9' } || !hasValidGs1CheckDigit() -> BarcodeFormat.FormatUnknown
        length == EAN_8_LENGTH -> BarcodeFormat.FormatEAN8
        length == UPC_A_LENGTH -> BarcodeFormat.FormatUPCA
        length == EAN_13_LENGTH -> BarcodeFormat.FormatEAN13
        else -> BarcodeFormat.FormatUnknown
    }

    private fun String.hasValidGs1CheckDigit(): Boolean {
        val weightedSum = dropLast(1).reversed().withIndex().sumOf { (index, char) ->
            val weight = when (index % 2 == 0) {
                true -> GS1_ODD_POSITION_WEIGHT
                false -> 1
            }
            char.digitToInt() * weight
        }
        return (GS1_MODULO - weightedSum % GS1_MODULO) % GS1_MODULO == last().digitToInt()
    }

    private data class Listener(
        val source: ScanningSource,
        val onBarcodeScanned: (CodeScannerStatus.Success) -> Unit,
    )

    companion object {
        private const val EAN_8_LENGTH = 8
        private const val UPC_A_LENGTH = 12
        private const val EAN_13_LENGTH = 13
        private const val GS1_ODD_POSITION_WEIGHT = 3
        private const val GS1_MODULO = 10
    }
}

fun BarcodeInputDetector.BarcodeResult.Error.isMissingEnterSuffix(): Boolean = when (failureReason) {
    BarcodeInputDetector.FailureReason.NO_TERMINATOR -> barcode.length >= MIN_SKU_LENGTH
    BarcodeInputDetector.FailureReason.TOO_SHORT -> false
}

fun Fragment.listenForHardwareBarcodes(
    hardwareBarcodeScanner: HardwareBarcodeScanner,
    source: ScanningSource,
    onBarcodeScanned: (CodeScannerStatus.Success) -> Unit,
) {
    viewLifecycleOwner.lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                hardwareBarcodeScanner.startListening(source, onBarcodeScanned)
            }

            override fun onPause(owner: LifecycleOwner) {
                hardwareBarcodeScanner.stopListening(onBarcodeScanned)
            }
        }
    )
}
