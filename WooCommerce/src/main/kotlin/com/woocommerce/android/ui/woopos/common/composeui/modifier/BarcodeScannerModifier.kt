package com.woocommerce.android.ui.woopos.common.composeui.modifier

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
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import org.wordpress.android.fluxc.utils.CurrentTimeProvider

private const val FIRST_PRINTABLE_CHAR_CODE = 32

fun Modifier.listenForBarcodes(
    onBarcodeEvent: (BarcodeInputDetector.BarcodeResult) -> Unit,
    enabled: Boolean = true
): Modifier = composed {
    val focusRequester = remember { FocusRequester() }
    val detector = remember { BarcodeInputDetector(onBarcodeEvent, CurrentTimeProvider()) }

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

class BarcodeInputDetector(
    private val onBarcodeEvent: (BarcodeResult) -> Unit,
    private val currentTimeProvider: CurrentTimeProvider,
) {
    private companion object {
        const val MAX_SCANNER_INTER_CHAR_DELAY_MS = 200L
        const val MIN_BARCODE_LENGTH = 6
    }

    private val barcodeBuffer = StringBuilder()
    private var lastCharTime: Long = -1L
    private var scanStartTime: Long = -1L

    fun handleKeyInput(char: Char): Boolean {
        val currentTime = currentTimeProvider.currentDate().time

        if (lastCharTime != -1L && currentTime - lastCharTime > MAX_SCANNER_INTER_CHAR_DELAY_MS) {
            if (barcodeBuffer.isNotEmpty()) {
                val scanDuration = if (scanStartTime != -1L) currentTime - scanStartTime else 0L
                val event = BarcodeResult.Error(
                    barcode = barcodeBuffer.toString(),
                    scanDurationMs = scanDuration,
                    failureReason = FailureReason.NO_TERMINATOR,
                )
                onBarcodeEvent(event)
                WooPosLogWrapper.d("Barcode scanning failed: $event")
            }
            clear()
        }

        when (char) {
            '\n', '\r' -> {
                processBarcodeBuffer()
            }

            else -> {
                if (scanStartTime == -1L) {
                    scanStartTime = currentTime
                }
                lastCharTime = currentTime
                barcodeBuffer.append(char)
            }
        }
        return true
    }

    private fun processBarcodeBuffer() {
        val scannedBarcode = barcodeBuffer.toString()
        val currentTime = currentTimeProvider.currentDate().time
        val scanDuration = if (scanStartTime != -1L) currentTime - scanStartTime else 0L

        val event = if (scannedBarcode.length >= MIN_BARCODE_LENGTH) {
            BarcodeResult.Success(
                barcode = scannedBarcode,
                scanDurationMs = scanDuration
            )
        } else if (scannedBarcode.isNotEmpty()) {
            BarcodeResult.Error(
                barcode = scannedBarcode,
                scanDurationMs = scanDuration,
                failureReason = FailureReason.TOO_SHORT,
            )
        } else {
            null
        }

        event?.let {
            onBarcodeEvent(it)
            WooPosLogWrapper.d("Barcode event: $it")
        }

        clear()
    }

    fun clear() {
        barcodeBuffer.clear()
        lastCharTime = -1
        scanStartTime = -1
    }


    sealed class BarcodeResult {
        abstract val scanDurationMs: Long
        abstract val barcode: String

        data class Success(
            override val barcode: String,
            override val scanDurationMs: Long,
        ) : BarcodeResult()

        data class Error(
            override val barcode: String,
            override val scanDurationMs: Long,
            val failureReason: FailureReason,
        ) : BarcodeResult()
    }

    enum class FailureReason(val value: String) {
        TOO_SHORT("too_short"),
        NO_TERMINATOR("no_terminator")
    }
}
