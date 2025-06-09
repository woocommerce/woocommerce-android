package com.woocommerce.android.ui.woopos.common.composeui.modifier

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.focusable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.utils.CurrentTimeProvider

fun Modifier.listenForBarcodes(
    onBarcodeScanned: (String) -> Unit,
    enabled: Boolean = true
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val detector = remember { BarcodeInputDetector(onBarcodeScanned, scope, CurrentTimeProvider()) }

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
                val pressedKey = keyEvent.utf16CodePoint.toChar()
                return@onKeyEvent detector.handleKeyInput(pressedKey)
            }

            false
        }
}

@VisibleForTesting
class BarcodeInputDetector(
    private val onBarcodeScanned: (String) -> Unit,
    private val coroutineScope: CoroutineScope,
    private val currentTimeProvider: CurrentTimeProvider,
) {
    companion object {
        @Suppress("SpellCheckingInspection")
        const val ALLOWED_BARCODE_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~:/?#[]@!$&'()*+,;="
        const val MAX_SCANNER_TOTAL_SCAN_TIMEOUT_MS = 1500L
        const val MAX_SCANNER_INTER_CHAR_DELAY_MS = 100L
        const val MIN_BARCODE_LENGTH = 4
    }

    private val barcodeBuffer = StringBuilder()
    private var scanStartTime: Long? = null
    private var lastCharTime: Long? = null
    private var timeoutJob: Job? = null

    fun handleKeyInput(char: Char): Boolean {
        val currentTime = currentTimeProvider.currentDate().time

        when (char) {
            '\n', '\r' -> {
                cancelTimeout()
                val isLikelyScanner = scanStartTime != null &&
                    (currentTime - scanStartTime!! <= MAX_SCANNER_TOTAL_SCAN_TIMEOUT_MS) &&
                    lastCharTime != null &&
                    (currentTime - lastCharTime!! <= MAX_SCANNER_INTER_CHAR_DELAY_MS)
                if (isLikelyScanner) processBarcodeBuffer()
                clear()
                return true
            }

            in ALLOWED_BARCODE_CHARS -> {
                cancelTimeout()

                if (scanStartTime == null) {
                    startNewScan(char, currentTime)
                } else {
                    handleContinuedScan(char, currentTime)
                }
                return true
            }

            else -> {
                clear()
                return false
            }
        }
    }

    private fun startNewScan(char: Char, currentTime: Long) {
        scanStartTime = currentTime
        lastCharTime = currentTime
        barcodeBuffer.append(char)

        timeoutJob = coroutineScope.launch {
            delay(MAX_SCANNER_TOTAL_SCAN_TIMEOUT_MS)
            processBarcodeBuffer()
        }
    }

    private fun handleContinuedScan(char: Char, currentTime: Long) {
        val totalElapsedTime = currentTime - scanStartTime!!
        val timeSinceLastChar = currentTime - lastCharTime!!
        val humanInputDetected = totalElapsedTime > MAX_SCANNER_TOTAL_SCAN_TIMEOUT_MS ||
            timeSinceLastChar > MAX_SCANNER_INTER_CHAR_DELAY_MS

        if (humanInputDetected) {
            clear()
            startNewScan(char, currentTime)
        } else {
            lastCharTime = currentTime
            barcodeBuffer.append(char)

            val remainingTime = MAX_SCANNER_TOTAL_SCAN_TIMEOUT_MS - totalElapsedTime
            if (remainingTime > 0) {
                timeoutJob = coroutineScope.launch {
                    delay(remainingTime)
                    processBarcodeBuffer()
                }
            } else {
                clear()
            }
        }
    }

    private fun processBarcodeBuffer() {
        val scannedBarcode = barcodeBuffer.toString()

        if (scannedBarcode.length >= MIN_BARCODE_LENGTH) {
            onBarcodeScanned(scannedBarcode)
        }

        clear()
    }

    fun clear() {
        cancelTimeout()
        barcodeBuffer.clear()
        scanStartTime = null
        lastCharTime = null
    }

    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }
}


