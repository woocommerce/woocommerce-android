package com.woocommerce.android.ui.woopos.common.composeui.modifier

import androidx.compose.foundation.focusable
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

@Suppress("SpellCheckingInspection")
private const val ALLOWED_BARCODE_CHARS =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~:/?#[]@!$&'()*+,;="

fun Modifier.barcodeScanner(
    onBarcodeScanned: (String) -> Unit,
    enabled: Boolean = true
): Modifier = composed {
    val focusRequester = remember { FocusRequester() }
    val barcodeBuffer = remember { StringBuilder() }

    LaunchedEffect(enabled) {
        if (enabled) {
            focusRequester.requestFocus()
        }
    }

    this
        .focusRequester(focusRequester)
        .focusable(enabled)
        .onKeyEvent { keyEvent ->
            if (!enabled) return@onKeyEvent false

            if (keyEvent.type == KeyEventType.KeyDown) {
                val pressedKey = keyEvent.utf16CodePoint.toChar()
                if (pressedKey == '\n' || pressedKey == '\r') {
                    val scannedBarcode = barcodeBuffer.toString()
                    if (scannedBarcode.isNotEmpty()) {
                        onBarcodeScanned(scannedBarcode)
                        barcodeBuffer.clear()
                    }
                    return@onKeyEvent true
                } else if (pressedKey in ALLOWED_BARCODE_CHARS) {
                    barcodeBuffer.append(pressedKey)
                    return@onKeyEvent true
                }
            }
            return@onKeyEvent false
        }
}
