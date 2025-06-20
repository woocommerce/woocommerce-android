package com.woocommerce.android.ui.woopos.common.composeui.modifier

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.focusable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector.Companion.FIRST_PRINTABLE_CHAR_CODE
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import org.wordpress.android.fluxc.utils.CurrentTimeProvider

fun Modifier.listenForBarcodes(
    onBarcodeScanned: (String) -> Unit,
    enabled: Boolean = true
): Modifier = composed {
    val focusRequester = remember { FocusRequester() }
    val detector = remember { BarcodeInputDetector(onBarcodeScanned, CurrentTimeProvider()) }

    LaunchedEffect(enabled) {
        if (enabled) {
            focusRequester.requestFocus()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            detector.clear()
        }
    }

    this
        .focusRequester(focusRequester)
        .focusable(enabled)
        .onKeyEvent { keyEvent ->
            if (!enabled) return@onKeyEvent false

            if (keyEvent.type == KeyEventType.KeyDown) {
                val charCode = keyEvent.utf16CodePoint
                val isValidChar = charCode > 0
                val isPrintableOrNewline = charCode == '\n'.code ||
                    charCode == '\r'.code ||
                    charCode >= FIRST_PRINTABLE_CHAR_CODE

                if (isValidChar && isPrintableOrNewline) {
                    val pressedKey = charCode.toChar()
                    return@onKeyEvent detector.handleKeyInput(pressedKey)
                }
            }

            false
        }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
class BarcodeInputDetector(
    private val onBarcodeScanned: (String) -> Unit,
    private val currentTimeProvider: CurrentTimeProvider,
) {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    companion object {
        const val MAX_SCANNER_INTER_CHAR_DELAY_MS = 200L
        const val FIRST_PRINTABLE_CHAR_CODE = 32
        const val MIN_BARCODE_LENGTH = 4
    }

    private val barcodeBuffer = StringBuilder()
    private var lastCharTime: Long = -1L

    fun handleKeyInput(char: Char): Boolean {
        val currentTime = currentTimeProvider.currentDate().time

        if (lastCharTime != -1L && currentTime - lastCharTime > MAX_SCANNER_INTER_CHAR_DELAY_MS) {
            clear()
        }

        when (char) {
            '\n', '\r' -> {
                processBarcodeBuffer()
            }

            else -> {
                lastCharTime = currentTime
                barcodeBuffer.append(char)
            }
        }
        return true
    }

    private fun processBarcodeBuffer() {
        val scannedBarcode = barcodeBuffer.toString()

        if (scannedBarcode.length >= MIN_BARCODE_LENGTH) {
            onBarcodeScanned(scannedBarcode)
            WooPosLogWrapper.d("Barcode scanned: $scannedBarcode")
        }

        clear()
    }

    fun clear() {
        barcodeBuffer.clear()
        lastCharTime = -1
    }
}
