package com.woocommerce.android.ui.barcodescanner

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HardwareBarcodeScanner @Inject constructor(
    @AppCoroutineScope appCoroutineScope: CoroutineScope,
    dispatchers: CoroutineDispatchers,
    currentTimeProvider: CurrentTimeProvider,
) {
    private var onBarcodeScanned: ((CodeScannerStatus.Success) -> Unit)? = null

    private val detector = BarcodeInputDetector(
        onBarcodeEvent = ::onBarcodeEvent,
        currentTimeProvider = currentTimeProvider,
        coroutineScope = appCoroutineScope + dispatchers.main,
    )

    fun startListening(onBarcodeScanned: (CodeScannerStatus.Success) -> Unit) {
        detector.clear()
        this.onBarcodeScanned = onBarcodeScanned
    }

    fun stopListening(onBarcodeScanned: (CodeScannerStatus.Success) -> Unit) {
        if (this.onBarcodeScanned === onBarcodeScanned) {
            detector.clear()
            this.onBarcodeScanned = null
        }
    }

    fun onKeyDown(unicodeChar: Int, isTextInputFocused: Boolean): Boolean {
        val char = unicodeChar.toBarcodeInputCharOrNull()
        return when {
            onBarcodeScanned == null || isTextInputFocused || char == null -> false
            char.isWhitespace() && !detector.hasPendingInput -> false
            else -> detector.handleKeyInput(char)
        }
    }

    private fun onBarcodeEvent(result: BarcodeInputDetector.BarcodeResult) {
        when (result) {
            is BarcodeInputDetector.BarcodeResult.Success -> onBarcodeScanned?.invoke(
                CodeScannerStatus.Success(code = result.barcode, format = result.barcode.inferBarcodeFormat())
            )
            is BarcodeInputDetector.BarcodeResult.Error -> Unit
        }
    }

    private fun String.inferBarcodeFormat(): BarcodeFormat = when {
        !all { it.isDigit() } || !hasValidGs1CheckDigit() -> BarcodeFormat.FormatUnknown
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

    private companion object {
        const val EAN_8_LENGTH = 8
        const val UPC_A_LENGTH = 12
        const val EAN_13_LENGTH = 13
        const val GS1_ODD_POSITION_WEIGHT = 3
        const val GS1_MODULO = 10
    }
}

fun Fragment.listenForHardwareBarcodes(
    hardwareBarcodeScanner: HardwareBarcodeScanner,
    onBarcodeScanned: (CodeScannerStatus.Success) -> Unit,
) {
    viewLifecycleOwner.lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                hardwareBarcodeScanner.startListening(onBarcodeScanned)
            }

            override fun onPause(owner: LifecycleOwner) {
                hardwareBarcodeScanner.stopListening(onBarcodeScanned)
            }
        }
    )
}
